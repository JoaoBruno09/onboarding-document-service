package com.bank.onboarding.documentservice.controllers;

import com.bank.onboarding.commonslib.persistence.models.Document;
import com.bank.onboarding.commonslib.persistence.services.DocumentRepoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentRepoService documentRepoService;

    @GetMapping("/test")
    public List<Document> getDocuments() {
        return documentRepoService.getAllDocuments();
    }
}
