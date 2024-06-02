package com.bank.onboarding.documentservice.services;

import com.bank.onboarding.commonslib.utils.kafka.CreateAccountEvent;
import com.bank.onboarding.commonslib.utils.kafka.ErrorEvent;
import com.bank.onboarding.commonslib.web.dtos.document.DeleteDocumentRequestDTO;
import com.bank.onboarding.commonslib.web.dtos.document.DocumentDTO;
import com.bank.onboarding.commonslib.web.dtos.document.UploadDocumentRequestDTO;

public interface DocumentService {

    void createDocumentForCreateAccountOperation(CreateAccountEvent createAccountEvent);
    void handleErrorEvent(ErrorEvent errorEvent);
    DocumentDTO uploadDoc(UploadDocumentRequestDTO uploadDocumentRequestDTO);
    void deleteDoc(DeleteDocumentRequestDTO deleteDocumentRequestDTO);
}
