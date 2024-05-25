package com.bank.onboarding.documentservice.services;

import com.bank.onboarding.commonslib.utils.kafka.CreateAccountEvent;

public interface DocumentService {

    public void createDocumentForCreateAccountOperation(CreateAccountEvent createAccountEvent);
}
