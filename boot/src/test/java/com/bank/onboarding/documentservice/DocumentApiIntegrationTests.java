package com.bank.onboarding.documentservice;

import com.bank.onboarding.commonslib.persistence.models.Document;
import com.bank.onboarding.commonslib.persistence.repositories.CustomerRepository;
import com.bank.onboarding.commonslib.persistence.repositories.DocumentRepository;
import com.bank.onboarding.commonslib.web.SecurityConfig;
import com.bank.onboarding.commonslib.web.dtos.document.DocumentDTO;
import com.bank.onboarding.commonslib.web.dtos.document.UploadDocumentRequestDTO;
import com.bank.onboarding.documentservice.services.DocumentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static com.bank.onboarding.commonslib.utils.TestOnboardingUtils.buildDeleteDocumentRequestDTO;
import static com.bank.onboarding.commonslib.utils.TestOnboardingUtils.buildDoc;
import static com.bank.onboarding.commonslib.utils.TestOnboardingUtils.buildUploadDocumentRequestDTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DocumentApiIntegrationTests {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private SecurityConfig securityConfig;

    @Value("${bank.onboarding.client.id}")
    private String clientId;

    private String token;

    private HttpHeaders httpHeaders;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        token = securityConfig.generateJWToken();
        httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(token);
        httpHeaders.set("X-Onboarding-Client-Id", clientId);
        objectMapper.registerModule(new JavaTimeModule());
        documentRepository.findAllByCustomerNumber("C123456789").forEach(document -> documentRepository.delete(document));
    }

    private String createURLWithPort() {
        return "http://localhost:" + port + "/documents";
    }

    private String insertDocumentDB() {
        String IBAN = "PT50 0000 2927 8040 8012 4082 5";
        Document dodcumentSaved = documentRepository.save(buildDoc("C123456789", IBAN.trim().replaceAll(" ", "").substring(IBAN.length()-19)));
        return dodcumentSaved.getId();
    }

    @Test
    void uploadDocTest() throws JsonProcessingException {
        UploadDocumentRequestDTO uploadDocumentRequestDTO = buildUploadDocumentRequestDTO();

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(uploadDocumentRequestDTO), httpHeaders);
        ResponseEntity<DocumentDTO> response = restTemplate.exchange(
                createURLWithPort(), HttpMethod.PUT, entity, DocumentDTO.class);

        DocumentDTO documentUploaded = response.getBody();
        assertEquals(response.getStatusCode(), HttpStatusCode.valueOf(201));
        assertEquals(documentUploaded, documentService.uploadDoc(uploadDocumentRequestDTO));
        assertFalse(documentRepository.findAllByCustomerNumber(uploadDocumentRequestDTO.getCustomerNumber()).isEmpty());
    }

    @Test
    void deleteDocTest() throws JsonProcessingException {
        String docId = insertDocumentDB();

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(buildDeleteDocumentRequestDTO()), httpHeaders);
        ResponseEntity<?> response = restTemplate.exchange(
                createURLWithPort(), HttpMethod.DELETE, entity, new ParameterizedTypeReference<>(){});

        assertEquals(response.getStatusCode(), HttpStatusCode.valueOf(204));
        assertTrue(documentRepository.findById(docId).isEmpty());
    }
}
