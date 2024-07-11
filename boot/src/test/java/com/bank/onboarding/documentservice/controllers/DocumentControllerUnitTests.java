package com.bank.onboarding.documentservice.controllers;

import com.bank.onboarding.commonslib.persistence.repositories.AccountRepository;
import com.bank.onboarding.commonslib.persistence.repositories.AddressRepository;
import com.bank.onboarding.commonslib.persistence.repositories.CardRepository;
import com.bank.onboarding.commonslib.persistence.repositories.ContactRepository;
import com.bank.onboarding.commonslib.persistence.repositories.CustomerRefRepository;
import com.bank.onboarding.commonslib.persistence.repositories.CustomerRepository;
import com.bank.onboarding.commonslib.persistence.repositories.DocumentRepository;
import com.bank.onboarding.commonslib.persistence.repositories.InterventionRepository;
import com.bank.onboarding.commonslib.persistence.repositories.RelationRepository;
import com.bank.onboarding.commonslib.persistence.services.AccountRepoService;
import com.bank.onboarding.commonslib.persistence.services.CardRepoService;
import com.bank.onboarding.commonslib.persistence.services.CustomerRefRepoService;
import com.bank.onboarding.commonslib.utils.OnboardingUtils;
import com.bank.onboarding.commonslib.web.SecurityConfig;
import com.bank.onboarding.commonslib.web.dtos.document.DeleteDocumentRequestDTO;
import com.bank.onboarding.commonslib.web.dtos.document.DocumentDTO;
import com.bank.onboarding.commonslib.web.dtos.document.UploadDocumentRequestDTO;
import com.bank.onboarding.documentservice.services.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static com.bank.onboarding.commonslib.utils.TestOnboardingUtils.buildDeleteDocumentRequestDTO;
import static com.bank.onboarding.commonslib.utils.TestOnboardingUtils.buildDocumentDTO;
import static com.bank.onboarding.commonslib.utils.TestOnboardingUtils.buildUploadDocumentRequestDTO;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(DocumentController.class)
@Import(SecurityConfig.class)
@MockBeans({
        @MockBean(OnboardingUtils.class),
        @MockBean(AccountRepoService.class),
        @MockBean(CardRepoService.class),
        @MockBean(CustomerRefRepoService.class),
        @MockBean(CustomerRefRepository.class),
        @MockBean(CustomerRepository.class),
        @MockBean(AccountRepository.class),
        @MockBean(CardRepository.class),
        @MockBean(DocumentRepository.class),
        @MockBean(InterventionRepository.class),
        @MockBean(RelationRepository.class),
        @MockBean(ContactRepository.class),
        @MockBean(AddressRepository.class)
})
class DocumentControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityConfig securityConfig;

    @MockBean
    private DocumentService documentService;

    @Value("${bank.onboarding.client.id}")
    private String clientId;

    private String token;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        token = securityConfig.generateJWToken();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void uploadDocTest() throws Exception{
        UploadDocumentRequestDTO uploadDocumentRequestDTO = buildUploadDocumentRequestDTO();
        DocumentDTO documentDTO = buildDocumentDTO();

        when(documentService.uploadDoc(uploadDocumentRequestDTO)).thenReturn(documentDTO);

        mockMvc.perform(put("/document")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Onboarding-Client-Id", clientId)
                        .content(objectMapper.writeValueAsString(uploadDocumentRequestDTO))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.documentName").value(documentDTO.getDocumentName()))
                .andExpect(jsonPath("$.documentType").value(documentDTO.getDocumentType()))
                .andExpect(jsonPath("$.documentBase64").value(documentDTO.getDocumentBase64()));
    }

    @Test
    void deleteDocTest() throws Exception{
        DeleteDocumentRequestDTO deleteDocumentRequestDTO = buildDeleteDocumentRequestDTO();

        mockMvc.perform(delete("/document")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Onboarding-Client-Id", clientId)
                        .content(objectMapper.writeValueAsString(deleteDocumentRequestDTO))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
