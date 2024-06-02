package com.bank.onboarding.documentservice.controllers;

import com.bank.onboarding.commonslib.persistence.exceptions.OnboardingException;
import com.bank.onboarding.commonslib.web.dtos.document.DeleteDocumentRequestDTO;
import com.bank.onboarding.commonslib.web.dtos.document.DocumentDTO;
import com.bank.onboarding.commonslib.web.dtos.document.UploadDocumentRequestDTO;
import com.bank.onboarding.documentservice.services.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PutMapping
    public ResponseEntity<?> uploadDoc(@RequestBody UploadDocumentRequestDTO uploadDocumentRequest){
        try {
            final DocumentDTO documentDTO = documentService.uploadDoc(uploadDocumentRequest);
            return new ResponseEntity<>(documentDTO, HttpStatus.OK);
        }
        catch(OnboardingException e ) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteDoc(@RequestBody DeleteDocumentRequestDTO deleteDocumentRequestDTO){
        try {
            documentService.deleteDoc(deleteDocumentRequestDTO);
            return new ResponseEntity<>(null, HttpStatus.OK);
        }
        catch(OnboardingException e ) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
