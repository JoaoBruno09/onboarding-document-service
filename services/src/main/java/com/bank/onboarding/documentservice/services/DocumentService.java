package com.bank.onboarding.documentservice.services;

import com.bank.onboarding.commonslib.utils.kafka.models.CreateAccountEvent;
import com.bank.onboarding.commonslib.web.dtos.document.DeleteDocumentRequestDTO;
import com.bank.onboarding.commonslib.web.dtos.document.DocumentDTO;
import com.bank.onboarding.commonslib.web.dtos.document.UploadDocumentRequestDTO;

public interface DocumentService {

    void createDocumentForCreateAccountOperation(CreateAccountEvent createAccountEvent);
    DocumentDTO uploadDoc(UploadDocumentRequestDTO uploadDocumentRequestDTO);
    void deleteDoc(DeleteDocumentRequestDTO deleteDocumentRequestDTO);
}
