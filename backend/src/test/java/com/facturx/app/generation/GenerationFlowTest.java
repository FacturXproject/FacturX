package com.facturx.app.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import com.facturx.app.extraction.ExtractionClient;
import com.facturx.app.extraction.ExtractionResponse;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * F13 : brouillon corrige -> generation (service Python, simule ici par un fichier
 * reellement produit par extractor/app/generation.py) -> validation par le vrai
 * moteur Mustang -> rapport et telechargement.
 */
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GenerationFlowTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExtractionClient extractionClient;

    @MockitoBean
    private GenerationClient generationClient;

    private static byte[] sample(String name) throws IOException {
        try (InputStream in = GenerationFlowTest.class.getResourceAsStream("/facturx-samples/" + name)) {
            if (in == null) {
                throw new IOException("Sample not found on classpath: " + name);
            }
            return in.readAllBytes();
        }
    }

    private Cookie registerAndLogin(String label) throws Exception {
        String email = label + "-" + UUID.randomUUID() + "@x.fr";
        MvcResult result = mockMvc.perform(
                        post("/api/auth/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email":"%s","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}"""
                                        .formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie session = result.getResponse().getCookie("EFACTURE_SESSION");
        if (session == null) {
            throw new IllegalStateException("No session cookie after register");
        }
        return session;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return new ObjectMapper().readTree(result.getResponse().getContentAsString());
    }

    /** Document depose + extrait (extracteur simule). */
    private long extractedDocument(Cookie session) throws Exception {
        when(extractionClient.extract(any(), anyString())).thenReturn(new ExtractionResponse(
                Map.of(
                        "invoiceNumber", new ExtractionResponse.FieldValue("FACT-2026-00142", 0.9),
                        "invoiceDate", new ExtractionResponse.FieldValue("28/07/2026", 0.8),
                        "sellerName", new ExtractionResponse.FieldValue("SARL Dupont Informatique", 0.6)
                ),
                List.of(new ExtractionResponse.LineValue("Prestation conseil IT", "5", "750.00", "3750.00", 0.5)),
                "pdf-extraction-v1"));

        long orgId = json(mockMvc.perform(post("/api/organizations?name=Cabinet Test").with(csrf()).cookie(session))
                .andExpect(status().isOk()).andReturn()).get("id").asLong();
        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice-" + UUID.randomUUID() + ".pdf", "application/pdf", "%PDF-1.4 fake".getBytes());
        long documentId = json(mockMvc.perform(multipart("/api/documents?organizationId=" + orgId)
                        .file(file).with(csrf()).cookie(session))
                .andExpect(status().isOk()).andReturn()).get("id").asLong();
        mockMvc.perform(post("/api/documents/" + documentId + "/extract").with(csrf()).cookie(session))
                .andExpect(status().isOk());
        return documentId;
    }

    private static final String CORRECTION = """
            {"fields":{"invoiceNumber":"FACT-2026-00142","invoiceDate":"28/07/2026","dueDate":"27/08/2026",
                       "sellerName":"SARL Dupont Informatique","sellerSiren":"452 891 237","sellerVat":"%s",
                       "sellerAddress":"14 rue des Lilas, 75011 Paris","buyerName":"SAS Martin & Associés",
                       "buyerAddress":"8 avenue Foch, 69002 Lyon","vatRate":"20"},
             "lines":[{"description":"Prestation conseil IT","quantity":"5","unitPrice":"750.00"},
                      {"description":"Livre technique","quantity":"3","unitPrice":"12,50","vatRate":"5,5"}]}""";

    /** Document dont le brouillon a ete verifie et enregistre par un humain (F12). */
    private long correctedDocument(Cookie session, String sellerVat) throws Exception {
        long documentId = extractedDocument(session);
        mockMvc.perform(put("/api/documents/" + documentId + "/draft").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(CORRECTION.formatted(sellerVat)))
                .andExpect(status().isOk());
        return documentId;
    }

    private ResultActions generate(Cookie session, long documentId, String body) throws Exception {
        var request = post("/api/documents/" + documentId + "/generate").with(csrf()).cookie(session);
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }

    @Test
    void correctedDraftIsGeneratedValidatedAndDownloadable() throws Exception {
        byte[] golden = sample("generated-facturx-en16931.pdf");
        when(generationClient.generate(any())).thenReturn(golden);
        Cookie session = registerAndLogin("gen-ok");
        long documentId = correctedDocument(session, "FR45452891237");

        MvcResult result = generate(session, documentId, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.profile").value("EN16931"))
                .andExpect(jsonPath("$.sourceDocumentId").value(documentId))
                .andExpect(jsonPath("$.filename").value("Factur-X_FACT-2026-00142.pdf"))
                .andExpect(jsonPath("$.errorCount").value(0))
                .andExpect(jsonPath("$.validationRunId").isNumber())
                .andReturn();
        long generatedId = json(result).get("generatedDocumentId").asLong();

        // Ce qui a ete envoye au generateur : brouillon revalide et recalcule par le serveur.
        ArgumentCaptor<GenerationPayload> captor = ArgumentCaptor.forClass(GenerationPayload.class);
        verify(generationClient).generate(captor.capture());
        GenerationPayload.Invoice sent = captor.getValue().invoice();
        assertThat(sent.number()).isEqualTo("FACT-2026-00142");
        assertThat(sent.issueDate()).isEqualTo("2026-07-28");
        assertThat(sent.dueDate()).isEqualTo("2026-08-27");
        assertThat(sent.seller().vatNumber()).isEqualTo("FR45452891237");
        assertThat(sent.lines()).extracting(GenerationPayload.Line::vatRate).containsExactly("20", "5.5");
        assertThat(sent.lines()).extracting(GenerationPayload.Line::total).containsExactly("3750.00", "37.50");
        assertThat(sent.taxes()).extracting(GenerationPayload.Tax::taxAmount).containsExactly("750.00", "2.06");
        assertThat(sent.totalTtc()).isEqualTo("4539.56");

        // Le fichier genere est un document de l'organisation, valide par Mustang (F08/F09).
        byte[] downloaded = mockMvc.perform(get("/api/documents/" + generatedId + "/download").cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(downloaded).isEqualTo(golden);
        mockMvc.perform(get("/api/documents/" + generatedId + "/report").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        mockMvc.perform(get("/api/documents/" + documentId + "/generations").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("VALID"))
                .andExpect(jsonPath("$[0].generatedDocumentId").value(generatedId));
    }

    @Test
    void basicProfileIsPassedToTheGenerator() throws Exception {
        when(generationClient.generate(any())).thenReturn(sample("generated-facturx-en16931.pdf"));
        Cookie session = registerAndLogin("gen-basic");
        long documentId = correctedDocument(session, "FR45452891237");

        generate(session, documentId, "{\"profile\":\"BASIC\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").value("BASIC"));

        ArgumentCaptor<GenerationPayload> captor = ArgumentCaptor.forClass(GenerationPayload.class);
        verify(generationClient).generate(captor.capture());
        assertThat(captor.getValue().profile()).isEqualTo("BASIC");
    }

    @Test
    void nonConformantGeneratedFileIsReportedAsInvalid() throws Exception {
        // Fichier reellement produit par le generateur sans n° de TVA vendeur (BR-S-02).
        when(generationClient.generate(any())).thenReturn(sample("generated-facturx-invalid.pdf"));
        Cookie session = registerAndLogin("gen-invalid");
        long documentId = correctedDocument(session, "FR45452891237");

        MvcResult result = generate(session, documentId, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALID"))
                .andExpect(jsonPath("$.errorCount").value(Matchers.greaterThan(0)))
                .andReturn();

        mockMvc.perform(get("/api/documents/" + json(result).get("generatedDocumentId").asLong() + "/report")
                        .cookie(session))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[*].ruleCode").value(Matchers.hasItem("BR-S-02")));
    }

    @Test
    void generatorFailureIsRecordedAsFailed() throws Exception {
        when(generationClient.generate(any())).thenThrow(new GenerationFailedException(
                "Le service de génération est indisponible ou a échoué à produire la facture.", null));
        Cookie session = registerAndLogin("gen-failed");
        long documentId = correctedDocument(session, "FR45452891237");

        generate(session, documentId, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.generatedDocumentId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.errorMessage").value(Matchers.containsString("indisponible")));

        mockMvc.perform(get("/api/documents/" + documentId + "/generations").cookie(session))
                .andExpect(jsonPath("$[0].status").value("FAILED"));
    }

    @Test
    void invalidDraftIsRefused() throws Exception {
        Cookie session = registerAndLogin("gen-invalid-draft");
        // Brouillon tel qu'extrait : acheteur, TVA... manquants, revalide avant generation.
        long documentId = extractedDocument(session);
        generate(session, documentId, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("DRAFT_INVALID"))
                .andExpect(jsonPath("$.errors").value(Matchers.hasItem("Le nom de l'acheteur est obligatoire.")));
        verify(generationClient, never()).generate(any());
    }

    @Test
    void validButUncheckedDraftIsRefused() throws Exception {
        when(extractionClient.extract(any(), anyString())).thenReturn(new ExtractionResponse(
                Map.of(
                        "invoiceNumber", new ExtractionResponse.FieldValue("F-1", 0.9),
                        "invoiceDate", new ExtractionResponse.FieldValue("28/07/2026", 0.8),
                        "sellerName", new ExtractionResponse.FieldValue("ACME", 0.6),
                        "sellerVat", new ExtractionResponse.FieldValue("FR45452891237", 0.6),
                        "buyerName", new ExtractionResponse.FieldValue("Client", 0.6),
                        "vatRate", new ExtractionResponse.FieldValue("20", 0.8)
                ),
                List.of(new ExtractionResponse.LineValue("Conseil", "1", "100", "100", 0.5)),
                "pdf-extraction-v1"));
        Cookie session = registerAndLogin("gen-valid-unchecked");
        long orgId = json(mockMvc.perform(post("/api/organizations?name=Org").with(csrf()).cookie(session))
                .andReturn()).get("id").asLong();
        long documentId = json(mockMvc.perform(multipart("/api/documents?organizationId=" + orgId)
                .file(new MockMultipartFile("file", "a.pdf", "application/pdf", "%PDF-1.4 x".getBytes()))
                .with(csrf()).cookie(session)).andReturn()).get("id").asLong();
        mockMvc.perform(post("/api/documents/" + documentId + "/extract").with(csrf()).cookie(session))
                .andExpect(status().isOk());

        generate(session, documentId, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("GENERATION_DRAFT_NOT_CHECKED"));
        verify(generationClient, never()).generate(any());
    }

    @Test
    void sellerVatNumberIsRequiredWhenVatIsCharged() throws Exception {
        Cookie session = registerAndLogin("gen-novat");
        long documentId = correctedDocument(session, "");

        generate(session, documentId, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("GENERATION_DRAFT_INCOMPLETE"))
                .andExpect(jsonPath("$.errors[0]").value(Matchers.containsString("TVA intracommunautaire du vendeur")));
        verify(generationClient, never()).generate(any());
    }

    @Test
    void unsupportedProfileIsRefused() throws Exception {
        Cookie session = registerAndLogin("gen-profile");
        long documentId = correctedDocument(session, "FR45452891237");

        generate(session, documentId, "{\"profile\":\"EXTENDED\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("GENERATION_PROFILE_UNSUPPORTED"));
    }

    @Test
    void documentWithoutDraftReturns404() throws Exception {
        Cookie session = registerAndLogin("gen-nodraft");
        long orgId = json(mockMvc.perform(post("/api/organizations?name=Org").with(csrf()).cookie(session))
                .andReturn()).get("id").asLong();
        long documentId = json(mockMvc.perform(multipart("/api/documents?organizationId=" + orgId)
                .file(new MockMultipartFile("file", "a.pdf", "application/pdf", "%PDF-1.4 x".getBytes()))
                .with(csrf()).cookie(session)).andReturn()).get("id").asLong();

        generate(session, documentId, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DRAFT_NOT_FOUND"));
    }

    @Test
    void anOutsiderCannotGenerateNorSeeTheHistory() throws Exception {
        Cookie owner = registerAndLogin("gen-owner");
        Cookie outsider = registerAndLogin("gen-outsider");
        long documentId = correctedDocument(owner, "FR45452891237");

        generate(outsider, documentId, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
        mockMvc.perform(get("/api/documents/" + documentId + "/generations").cookie(outsider))
                .andExpect(status().isForbidden());
        verify(generationClient, never()).generate(any());
    }
}
