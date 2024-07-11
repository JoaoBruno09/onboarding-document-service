package com.bank.onboarding.documentservice.controllers;

import com.bank.onboarding.commonslib.persistence.exceptions.OnboardingException;
import com.bank.onboarding.commonslib.utils.OnboardingUtils;
import com.bank.onboarding.commonslib.web.dtos.document.DeleteDocumentRequestDTO;
import com.bank.onboarding.commonslib.web.dtos.document.DocumentDTO;
import com.bank.onboarding.commonslib.web.dtos.document.UploadDocumentRequestDTO;
import com.bank.onboarding.documentservice.services.DocumentService;
import feign.Request;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final OnboardingUtils onboardingUtils;

    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadDoc(@RequestBody @Valid UploadDocumentRequestDTO uploadDocumentRequest){
        try {
            final DocumentDTO documentDTO = documentService.uploadDoc(uploadDocumentRequest);
            return new ResponseEntity<>(documentDTO, HttpStatus.CREATED);
        }
        catch(OnboardingException e ) {
            return onboardingUtils.buildResponseEntity(Request.HttpMethod.PUT.name(), e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteDoc(@RequestBody @Valid DeleteDocumentRequestDTO deleteDocumentRequestDTO){
        try {
            documentService.deleteDoc(deleteDocumentRequestDTO);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        catch(OnboardingException e ) {
            return onboardingUtils.buildResponseEntity(Request.HttpMethod.DELETE.name(), e.getMessage());
        }
    }
}
