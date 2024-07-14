package com.bank.onboarding.documentservice.services.impl;

import com.bank.onboarding.commonslib.persistence.enums.OperationType;
import com.bank.onboarding.commonslib.persistence.exceptions.OnboardingException;
import com.bank.onboarding.commonslib.persistence.models.Document;
import com.bank.onboarding.commonslib.persistence.services.CustomerRefRepoService;
import com.bank.onboarding.commonslib.persistence.services.DocumentRepoService;
import com.bank.onboarding.commonslib.utils.AsyncExecutor;
import com.bank.onboarding.commonslib.utils.OnboardingUtils;
import com.bank.onboarding.commonslib.utils.kafka.KafkaProducer;
import com.bank.onboarding.commonslib.utils.kafka.models.CreateAccountEvent;
import com.bank.onboarding.commonslib.utils.kafka.models.DocUploadEvent;
import com.bank.onboarding.commonslib.utils.mappers.CustomerMapper;
import com.bank.onboarding.commonslib.utils.mappers.DocumentMapper;
import com.bank.onboarding.commonslib.web.dtos.account.AccountRefDTO;
import com.bank.onboarding.commonslib.web.dtos.account.CreateAccountRequestDTO;
import com.bank.onboarding.commonslib.web.dtos.customer.CustomerDocumentsRequest;
import com.bank.onboarding.commonslib.web.dtos.customer.CustomerRefDTO;
import com.bank.onboarding.commonslib.web.dtos.customer.CustomerRequestDTO;
import com.bank.onboarding.commonslib.web.dtos.document.DeleteDocumentRequestDTO;
import com.bank.onboarding.commonslib.web.dtos.document.DocumentDTO;
import com.bank.onboarding.commonslib.web.dtos.document.UploadDocumentRequestDTO;
import com.bank.onboarding.documentservice.services.DocumentService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.bank.onboarding.commonslib.persistence.constants.OnboardingConstants.DOCUMENT_TYPES_CREATE_ACCOUNT_REQUEST;
import static com.bank.onboarding.commonslib.persistence.constants.OnboardingConstants.DOCUMENT_TYPES_PHASE_3_ACCOUNT;
import static com.bank.onboarding.commonslib.persistence.constants.OnboardingConstants.DOCUMENT_TYPES_PHASE_3_CUSTOMER;
import static com.bank.onboarding.commonslib.persistence.enums.OperationType.CREATE_ACCOUNT;
import static com.bank.onboarding.commonslib.persistence.enums.OperationType.DOCS_UPLOAD;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepoService documentRepoService;
    private final CustomerRefRepoService customerRefRepoService;
    private final OnboardingUtils onboardingUtils;
    private final AsyncExecutor asyncExecutor;
    private final KafkaProducer kafkaProducer;

    @Value("${spring.kafka.producer.customer.topic-name}")
    private String customerTopicName;

    @Value("${spring.kafka.producer.account.topic-name}")
    private String accountTopicName;

    @Value("${spring.kafka.producer.intervention.topic-name}")
    private String interventionTopicName;

    @Override
    public void createDocumentForCreateAccountOperation(CreateAccountEvent createAccountEvent) {
        List<CustomerDocumentsRequest> customerDocuments =  Optional.ofNullable(createAccountEvent.getCreateAccountRequestDTO())
                .map(CreateAccountRequestDTO::getCustomerIntervenient).map(CustomerRequestDTO::getCustomerDocuments)
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
                    .customerNumber(createAccountEvent.getCustomerRefDTO().getCustomerNumber())
                    .build());
        });
        customerRefRepoService.saveCustomerRefDB(CustomerMapper.INSTANCE.toCustomerRef(createAccountEvent.getCustomerRefDTO()));
    }

    private void sendCreationEventErrors(AccountRefDTO accountRefDTO, CustomerRefDTO customerRefDTO) {
        List<CompletableFuture<?>> completableFutures = new ArrayList<>();
        completableFutures.add(CompletableFuture.runAsync(() -> onboardingUtils.sendErrorEvent(customerTopicName, accountRefDTO, customerRefDTO, CREATE_ACCOUNT)));
        completableFutures.add(CompletableFuture.runAsync(() -> onboardingUtils.sendErrorEvent(accountTopicName, accountRefDTO, customerRefDTO, CREATE_ACCOUNT)));
        completableFutures.add(CompletableFuture.runAsync(() -> onboardingUtils.sendErrorEvent(interventionTopicName, accountRefDTO, customerRefDTO, CREATE_ACCOUNT)));

        asyncExecutor.execute(completableFutures);
    }

    @Override
    public DocumentDTO uploadDoc(UploadDocumentRequestDTO uploadDocumentRequestDTO) {
        onboardingUtils.isValidPhase(uploadDocumentRequestDTO.getAccountPhase(), DOCS_UPLOAD);
        return validateDocumentTypeForAccountOrCustomer(uploadDocumentRequestDTO.getDocumentType(),
                uploadDocumentRequestDTO.getDocumentBase64(),
                uploadDocumentRequestDTO.getAccountNumber(),
                uploadDocumentRequestDTO.getCustomerNumber(),
                DOCS_UPLOAD);
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
        DocumentDTO documentDTOToBeReturned = null;

        if(accountNumber != null && DOCUMENT_TYPES_PHASE_3_ACCOUNT.contains(documentType)){
            if(DOCS_UPLOAD.equals(operationType)){
                documentDTOToBeReturned = saveDoc(accountNumber, documentBase64, documentType, true);

                boolean accountHasAllDocs = new HashSet<>(documentRepoService.getAllDocumentsByAccountNumber(accountNumber).stream()
                        .map(Document::getDocumentType)
                        .filter(StringUtils::isNotBlank)
                        .toList())
                        .containsAll(DOCUMENT_TYPES_PHASE_3_ACCOUNT);

                if(accountHasAllDocs)
                    kafkaProducer.sendEvent(accountTopicName, DOCS_UPLOAD, DocUploadEvent.builder().accountNumber(accountNumber).areDocsValid(true).build());
            }else {
                documentRepoService.deleteDocumentByAccountNumberOrCustomerNumber(accountNumber, documentType, true);
                kafkaProducer.sendEvent(accountTopicName, DOCS_UPLOAD, DocUploadEvent.builder().accountNumber(accountNumber).areDocsValid(false).build());
            }
        } else if(customerNumber != null && DOCUMENT_TYPES_PHASE_3_CUSTOMER.contains(documentType)){
            if(DOCS_UPLOAD.equals(operationType)){
                documentDTOToBeReturned = saveDoc(customerNumber, documentBase64, documentType, false);

                boolean customerHasAllDocs = new HashSet<>(documentRepoService.getAllDocumentsByCustomerNumber(customerNumber).stream()
                        .map(Document::getDocumentType)
                        .filter(StringUtils::isNotBlank)
                        .toList())
                        .containsAll(DOCUMENT_TYPES_PHASE_3_CUSTOMER);

                if(customerHasAllDocs)
                    kafkaProducer.sendEvent(accountTopicName, DOCS_UPLOAD, DocUploadEvent.builder().accountNumber(accountNumber).areDocsValid(true).build());
            }else {
                documentRepoService.deleteDocumentByAccountNumberOrCustomerNumber(customerNumber, documentType, false);
                kafkaProducer.sendEvent(accountTopicName, DOCS_UPLOAD, DocUploadEvent.builder().customerNumber(customerNumber).areDocsValid(false).build());
            }
        } else{
            throw new OnboardingException("O tipo de documento introduzido é inválido");
        }

        return documentDTOToBeReturned;
    }

    private DocumentDTO saveDoc(String id, String documentBase64, String documentType, boolean isAccountDoc) {
        if(id != null){
            Document documentSaved = documentRepoService.saveDocumentDB(Document.builder()
                    .documentName(onboardingUtils.getDocumentTypeValue(documentType))
                    .documentType(documentType)
                    .documentBase64(documentBase64)
                    .uploadedTime(LocalDateTime.now())
                    .accountNumber(isAccountDoc ? id : null)
                    .customerNumber(isAccountDoc ? null : id)
                    .build());

            return DocumentMapper.INSTANCE.toDocumentDTO(documentSaved);
        }

        return null;
    }
}
