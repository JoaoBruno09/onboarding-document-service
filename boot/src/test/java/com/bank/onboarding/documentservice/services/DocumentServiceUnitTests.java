package com.bank.onboarding.documentservice.services;

import com.bank.onboarding.commonslib.persistence.enums.DocumentType;
import com.bank.onboarding.commonslib.web.dtos.document.DocumentDTO;
import com.bank.onboarding.commonslib.web.dtos.document.UploadDocumentRequestDTO;
import com.bank.onboarding.documentservice.Application;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.bank.onboarding.commonslib.utils.TestOnboardingUtils.buildDocumentDTO;
import static com.bank.onboarding.commonslib.utils.TestOnboardingUtils.buildUploadDocumentRequestDTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = Application.class)
@ExtendWith({SpringExtension.class})
class DocumentServiceUnitTests {

    @Mock
    private DocumentService documentService;

    @Test
    void uploadDocTest() throws Exception{
        UploadDocumentRequestDTO uploadDocumentRequestDTO = buildUploadDocumentRequestDTO();
        DocumentDTO documentDTO = buildDocumentDTO();

        when(documentService.uploadDoc(uploadDocumentRequestDTO)).thenReturn(documentDTO);

        assertEquals(DocumentType.CC.getValue(), documentService.uploadDoc(uploadDocumentRequestDTO).getDocumentName());
        assertEquals(DocumentType.CC.name(), documentService.uploadDoc(uploadDocumentRequestDTO).getDocumentType());
        assertEquals("JVBERi0xLjQKJcOkw7zDtsOfCjIgMCBvYmoKP", documentService.uploadDoc(uploadDocumentRequestDTO).getDocumentBase64());
    }
}
