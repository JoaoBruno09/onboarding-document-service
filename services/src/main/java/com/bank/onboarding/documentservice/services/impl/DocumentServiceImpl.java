package com.bank.onboarding.documentservice.services.impl;

import com.bank.onboarding.commonslib.persistence.enums.OperationType;
import com.bank.onboarding.commonslib.persistence.exceptions.OnboardingException;
import com.bank.onboarding.commonslib.persistence.models.Document;
import com.bank.onboarding.commonslib.persistence.services.AccountRefRepoService;
import com.bank.onboarding.commonslib.persistence.services.CustomerRefRepoService;
import com.bank.onboarding.commonslib.persistence.services.DocumentRepoService;
import com.bank.onboarding.commonslib.utils.OnboardingUtils;
import com.bank.onboarding.commonslib.utils.kafka.CreateAccountEvent;
import com.bank.onboarding.commonslib.utils.kafka.ErrorEvent;
import com.bank.onboarding.commonslib.utils.mappers.CustomerMapper;
import com.bank.onboarding.commonslib.utils.mappers.DocumentMapper;
import com.bank.onboarding.commonslib.web.dtos.account.AccountRefDTO;
import com.bank.onboarding.commonslib.web.dtos.account.CreateAccountRequestDTO;
import com.bank.onboarding.commonslib.web.dtos.customer.CustomerDocumentsRequest;
import com.bank.onboarding.commonslib.web.dtos.customer.CustomerRefDTO;
import com.bank.onboarding.commonslib.web.dtos.document.DeleteDocumentRequestDTO;
import com.bank.onboarding.commonslib.web.dtos.document.DocumentDTO;
import com.bank.onboarding.commonslib.web.dtos.document.UploadDocumentRequestDTO;
import com.bank.onboarding.documentservice.services.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.bank.onboarding.commonslib.persistence.constants.OnboardingConstants.DOCUMENT_TYPES_CREATE_ACCOUNT_REQUEST;
import static com.bank.onboarding.commonslib.persistence.constants.OnboardingConstants.DOCUMENT_TYPES_PHASE_3_ACCOUNT;
import static com.bank.onboarding.commonslib.persistence.constants.OnboardingConstants.DOCUMENT_TYPES_PHASE_3_CUSTOMER;
import static com.bank.onboarding.commonslib.persistence.enums.OperationType.CREATE_ACCOUNT;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepoService documentRepoService;
    private final AccountRefRepoService accountRefRepoService;
    private final CustomerRefRepoService customerRefRepoService;
    private final OnboardingUtils onboardingUtils;

    @Value("${spring.kafka.producer.customer.topic-name}")
    private String customerTopicName;

    @Value("${spring.kafka.producer.account.topic-name}")
    private String accountTopicName;

    @Value("${spring.kafka.producer.intervention.topic-name}")
    private String interventionTopicName;

    @Override
    public void createDocumentForCreateAccountOperation(CreateAccountEvent createAccountEvent) {
        List<CustomerDocumentsRequest> customerDocuments =  Optional.ofNullable(createAccountEvent.getCreateAccountRequestDTO()).map(CreateAccountRequestDTO::getCustomerDocuments)
                .orElseThrow(() -> new OnboardingException("Não foram inseridos documentos")).stream().toList();

        if(!customerDocuments.stream().allMatch(document -> DOCUMENT_TYPES_CREATE_ACCOUNT_REQUEST.contains(document.getDocumentType()))){
            sendCreationEventErrors(createAccountEvent.getAccountRefDTO(), createAccountEvent.getCustomerRefDTO());
            throw new OnboardingException("O/s tipo/s de documento/s introduzido é inválido");
        }

        customerDocuments.forEach(document -> {
            String documentType = document.getDocumentType();
            documentRepoService.saveDocumentDB(Document.builder()
                    .documentName(onboardingUtils.getDocumentTypeValue(documentType))
                    .documentType(documentType)
                    .documentBase64(document.getDocumentBase64())
                    .uploadedTime(LocalDateTime.now())
                    .customerId(createAccountEvent.getCustomerRefDTO().getCustomerId())
                    .build());
        });
        customerRefRepoService.saveCustomerRefDB(CustomerMapper.INSTANCE.toCustomerRef(createAccountEvent.getCustomerRefDTO()));
    }

    private void sendCreationEventErrors(AccountRefDTO accountRefDTO, CustomerRefDTO customerRefDTO) {
        onboardingUtils.sendErrorEvent(customerTopicName, accountRefDTO, customerRefDTO, CREATE_ACCOUNT);
        onboardingUtils.sendErrorEvent(accountTopicName, accountRefDTO, customerRefDTO, CREATE_ACCOUNT);
        onboardingUtils.sendErrorEvent(interventionTopicName, accountRefDTO, customerRefDTO, CREATE_ACCOUNT);
        accountRefRepoService.deleteAccountById(accountRefDTO.getAccountId());
    }

    @Override
    public void handleErrorEvent(ErrorEvent errorEvent) {
        if(CREATE_ACCOUNT.equals(errorEvent.getOperationType())){
            accountRefRepoService.deleteAccountById(errorEvent.getAccountRefDTO().getAccountId());
        }
    }

    @Override
    public DocumentDTO uploadDoc(UploadDocumentRequestDTO uploadDocumentRequestDTO) {
        onboardingUtils.isValidPhase(uploadDocumentRequestDTO.getAccountPhase(), OperationType.DOCS_UPLOAD);
        return validateDocumentTypeForAccountOrCustomer(uploadDocumentRequestDTO.getDocumentType(),
                uploadDocumentRequestDTO.getDocumentBase64(),
                uploadDocumentRequestDTO.getAccountNumber(),
                uploadDocumentRequestDTO.getCustomerNumber(),
                OperationType.DOCS_UPLOAD);
    }

    @Override
    public void deleteDoc(DeleteDocumentRequestDTO deleteDocumentRequestDTO) {
        onboardingUtils.isValidPhase(deleteDocumentRequestDTO.getAccountPhase(), OperationType.DOCS_DELETE);

        validateDocumentTypeForAccountOrCustomer(deleteDocumentRequestDTO.getDocumentType(),
                null,
                deleteDocumentRequestDTO.getAccountNumber(),
                deleteDocumentRequestDTO.getCustomerNumber(),
                OperationType.DOCS_DELETE);
    }

    private DocumentDTO validateDocumentTypeForAccountOrCustomer(String documentType, String documentBase64, String accountNumber, String customerNumber, OperationType operationType) {
        if(accountNumber != null && DOCUMENT_TYPES_PHASE_3_ACCOUNT.contains(documentType)){
            String accountId = accountRefRepoService.findAccountRefByAccountNumber(accountNumber).getId();

            if(OperationType.DOCS_UPLOAD.equals(operationType)){
                return saveDoc(accountId, documentBase64, documentType, true);
            }else {
                documentRepoService.deleteDocumentByAccountIdOrCustomerId(accountId, true);
                return null;
            }
        } else if(customerNumber != null && DOCUMENT_TYPES_PHASE_3_CUSTOMER.contains(documentType)){
            String customerId = customerRefRepoService.findCustomerRefByCustomerNumber(customerNumber).getId();

            if(OperationType.DOCS_UPLOAD.equals(operationType)){
                return saveDoc(customerId, documentBase64, documentType, false);
            }else {
                documentRepoService.deleteDocumentByAccountIdOrCustomerId(customerId, false);
                return null;
            }
        } else{
            throw new OnboardingException("O tipo de documento introduzido é inválido");
        }
    }

    private DocumentDTO saveDoc(String id, String documentBase64, String documentType, boolean isAccountDoc) {
        if(id != null){
            Document documentSaved = documentRepoService.saveDocumentDB(Document.builder()
                    .documentName(onboardingUtils.getDocumentTypeValue(documentType))
                    .documentType(documentType)
                    .documentBase64(documentBase64)
                    .uploadedTime(LocalDateTime.now())
                    .accountId(isAccountDoc ? id : null)
                    .customerId(isAccountDoc ? null : id)
                    .build());

            return DocumentMapper.INSTANCE.toDocumentDTO(documentSaved);
        }

        return null;
    }
}
