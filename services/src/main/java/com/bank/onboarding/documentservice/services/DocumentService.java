package com.bank.onboarding.documentservice.services;

import com.bank.onboarding.commonslib.utils.kafka.CreateAccountEvent;
import com.bank.onboarding.commonslib.utils.kafka.ErrorEvent;

public interface DocumentService {

    void createDocumentForCreateAccountOperation(CreateAccountEvent createAccountEvent);
    void handleErrorEvent(ErrorEvent errorEvent);
}
