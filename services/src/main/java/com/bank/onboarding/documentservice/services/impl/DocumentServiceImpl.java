package com.bank.onboarding.documentservice.services.impl;

import com.bank.onboarding.commonslib.persistence.exceptions.OnboardingException;
import com.bank.onboarding.commonslib.persistence.models.Document;
import com.bank.onboarding.commonslib.persistence.services.AccountRefRepoService;
import com.bank.onboarding.commonslib.persistence.services.CustomerRefRepoService;
import com.bank.onboarding.commonslib.persistence.services.DocumentRepoService;
import com.bank.onboarding.commonslib.utils.OnboardingUtils;
import com.bank.onboarding.commonslib.utils.kafka.CreateAccountEvent;
import com.bank.onboarding.commonslib.utils.mappers.AccountMapper;
import com.bank.onboarding.commonslib.utils.mappers.CustomerMapper;
import com.bank.onboarding.commonslib.web.dtos.account.CreateAccountRequestDTO;
import com.bank.onboarding.commonslib.web.dtos.customer.CustomerDocumentsRequest;
import com.bank.onboarding.documentservice.services.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.bank.onboarding.commonslib.persistence.constants.OnboardingConstants.DOCUMENT_TYPES_CREATE_ACCOUNT_REQUEST;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepoService documentRepoService;
    private final AccountRefRepoService accountRefRepoService;
    private final CustomerRefRepoService customerRefRepoService;
    private final OnboardingUtils onboardingUtils;
    @Override
    public void createDocumentForCreateAccountOperation(CreateAccountEvent createAccountEvent) {
        List<CustomerDocumentsRequest> customerDocuments =  Optional.ofNullable(createAccountEvent.getCreateAccountRequestDTO()).map(CreateAccountRequestDTO::getCustomerDocuments)
                .orElseThrow(() -> new OnboardingException("Não foram inseridos documentos")).stream().toList();

        if(!customerDocuments.stream().allMatch(document -> DOCUMENT_TYPES_CREATE_ACCOUNT_REQUEST.contains(document.getDocumentType())))
            //TODO -> Enviar evento para eliminar conta e retirar a conta do cliente
            throw new OnboardingException("O/s tipo/s de documento/s introduzido é inválido");

        customerDocuments.forEach(document -> {
            String documentType = document.getDocumentType();
            documentRepoService.saveAccountDB(Document.builder()
                    .documentName(onboardingUtils.getDocumentTypeValue(documentType))
                    .documentType(documentType)
                    .documentBase64(document.getDocumentBase64())
                    .uploadedTime(LocalDateTime.now())
                    .accountId(createAccountEvent.getAccountRefDTO().getAccountId())
                    .customerId(createAccountEvent.getCustomerRefDTO().getCustomerId())
                    .build());
        });

        accountRefRepoService.saveAccountRefDB(AccountMapper.INSTANCE.toAccountRef(createAccountEvent.getAccountRefDTO()));
        customerRefRepoService.saveCustomerRefDB(CustomerMapper.INSTANCE.toCustomerRef(createAccountEvent.getCustomerRefDTO()));
    }
}
