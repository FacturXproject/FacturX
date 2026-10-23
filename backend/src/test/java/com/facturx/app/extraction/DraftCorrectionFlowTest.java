package com.facturx.app.extraction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import tools.jackson.databind.ObjectMapper;

/**
 * F12 : PUT /api/documents/{id}/draft - le serveur valide et recalcule seul la
 * correction, la persiste, et la renvoie au rechargement (GET .../draft).
 */
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DraftCorrectionFlowTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExtractionClient extractionClient;

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

    private long createOrganization(Cookie session) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/organizations?name=Cabinet Test")
                                .with(csrf())
                                .cookie(session))
                .andExpect(status().isOk())
                .andReturn();

        return new ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }

    private long uploadPdf(Cookie session, long orgId) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice-" + UUID.randomUUID() + ".pdf", "application/pdf",
                "%PDF-1.4 fake content".getBytes());

        MvcResult uploaded = mockMvc.perform(
                        multipart("/api/documents?organizationId=" + orgId)
                                .file(file)
                                .with(csrf())
                                .cookie(session))
                .andExpect(status().isOk())
                .andReturn();

        return new ObjectMapper()
                .readTree(uploaded.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }

    /** Document depose puis extrait (extracteur simule) : un brouillon F11 existe. */
    private long extractedDocument(Cookie session) throws Exception {
        when(extractionClient.extract(any(), anyString())).thenReturn(new ExtractionResponse(
                Map.of(
                        "invoiceNumber", new ExtractionResponse.FieldValue("FACT-2026-00142", 0.9),
                        "invoiceDate", new ExtractionResponse.FieldValue("15/03/2026", 0.8),
                        "sellerName", new ExtractionResponse.FieldValue("ACME SAS", 0.6),
                        "sellerSiren", new ExtractionResponse.FieldValue("452891237", 0.3),
                        "vatRate", new ExtractionResponse.FieldValue("20", 0.75)
                ),
                List.of(new ExtractionResponse.LineValue("Prestation conseil", "5", "750.00", "3750.00", 0.5)),
                "pdf-extraction-v1"));

        long documentId = uploadPdf(session, createOrganization(session));
        mockMvc.perform(post("/api/documents/" + documentId + "/extract").with(csrf()).cookie(session))
                .andExpect(status().isOk());
        return documentId;
    }

    private ResultActions save(Cookie session, long documentId, String json) throws Exception {
        return mockMvc.perform(
                put("/api/documents/" + documentId + "/draft")
                        .with(csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    // Numero corrige, acheteur ajoute, vendeur et date inchanges ; totaux du client faux expres.
    private static final String VALID_CORRECTION = """
            {"fields":{"invoiceNumber":"FACT-2026-00143","invoiceDate":"15/03/2026","sellerName":"ACME SAS",
                       "buyerName":"Client SARL","vatRate":"20","totalHt":"999999","totalTtc":"1"},
             "lines":[{"description":"Prestation conseil","quantity":"5","unitPrice":"750.00"},
                      {"description":"Livre","quantity":"2","unitPrice":"10,00","vatRate":"5,5"}]}""";

    @Test
    void savedCorrectionIsRecalculatedAndSurvivesAReload() throws Exception {
        Cookie session = registerAndLogin("draft-save");
        long documentId = extractedDocument(session);

        save(session, documentId, VALID_CORRECTION)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields.totalHt.value").value("3770.00"))
                .andExpect(jsonPath("$.fields.totalVat.value").value("751.10"))
                .andExpect(jsonPath("$.fields.totalTtc.value").value("4521.10"));

        mockMvc.perform(get("/api/documents/" + documentId + "/draft").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields.invoiceNumber.value").value("FACT-2026-00143"))
                .andExpect(jsonPath("$.fields.buyerName.value").value("Client SARL"))
                // Champ non envoye : conserve tel qu'extrait.
                .andExpect(jsonPath("$.fields.sellerSiren.value").value("452891237"))
                // Somme des lignes coherente avec les totaux, ventilation par taux.
                .andExpect(jsonPath("$.lines[0].total").value("3750.00"))
                .andExpect(jsonPath("$.lines[1].total").value("20.00"))
                .andExpect(jsonPath("$.lines[1].vatRate").value("5.5"))
                .andExpect(jsonPath("$.taxes.length()").value(2))
                .andExpect(jsonPath("$.taxes[0].rate").value("20"))
                .andExpect(jsonPath("$.taxes[0].taxableAmount").value("3750.00"))
                .andExpect(jsonPath("$.taxes[0].taxAmount").value("750.00"))
                .andExpect(jsonPath("$.taxes[1].rate").value("5.5"))
                .andExpect(jsonPath("$.taxes[1].taxAmount").value("1.10"))
                .andExpect(jsonPath("$.fields.totalTtc.value").value("4521.10"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void correctedFieldsAreMarkedManualAndUntouchedOnesKeepTheirConfidence() throws Exception {
        Cookie session = registerAndLogin("draft-confidence");
        long documentId = extractedDocument(session);

        save(session, documentId, VALID_CORRECTION).andExpect(status().isOk());

        mockMvc.perform(get("/api/documents/" + documentId + "/draft").cookie(session))
                .andExpect(jsonPath("$.fields.invoiceNumber.source").value("manual"))
                .andExpect(jsonPath("$.fields.invoiceNumber.confidence").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.fields.buyerName.source").value("manual"))
                .andExpect(jsonPath("$.fields.sellerName.source").value("pdf-extraction-v1"))
                .andExpect(jsonPath("$.fields.sellerName.confidence").value(0.6))
                .andExpect(jsonPath("$.fields.vatRate.confidence").value(0.75))
                .andExpect(jsonPath("$.fields.totalTtc.source").value("calculated"))
                // Premiere ligne identique a l'extraction : confiance conservee.
                .andExpect(jsonPath("$.lines[0].source").value("pdf-extraction-v1"))
                .andExpect(jsonPath("$.lines[0].confidence").value(0.5))
                .andExpect(jsonPath("$.lines[1].source").value("manual"));
    }

    @Test
    void missingRequiredFieldIsRefusedWithReadableErrors() throws Exception {
        Cookie session = registerAndLogin("draft-required");
        long documentId = extractedDocument(session);

        save(session, documentId, """
                {"fields":{"invoiceNumber":"F-1","invoiceDate":"15/03/2026","sellerName":"ACME","buyerName":" ","vatRate":"20"},
                 "lines":[{"description":"","quantity":"0","unitPrice":"10"}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("DRAFT_INVALID"))
                .andExpect(jsonPath("$.errors").value(Matchers.contains(
                        "Le nom de l'acheteur est obligatoire.",
                        "Ligne 1 : la description est obligatoire.",
                        "Ligne 1 : la quantité doit être un nombre strictement positif.")))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("acheteur")));

        // Rien n'a ete enregistre.
        mockMvc.perform(get("/api/documents/" + documentId + "/draft").cookie(session))
                .andExpect(jsonPath("$.fields.invoiceNumber.value").value("FACT-2026-00142"))
                .andExpect(jsonPath("$.lines[0].quantity").value("5"));
    }

    @Test
    void malformedNumbersAndOverlongValuesAreA400NotA500() throws Exception {
        Cookie session = registerAndLogin("draft-malformed");
        long documentId = extractedDocument(session);

        save(session, documentId, """
                {"fields":{"invoiceNumber":"F-1","invoiceDate":"15/03/2026","sellerName":"ACME","buyerName":"B","vatRate":"20"},
                 "lines":[{"description":"%s","quantity":"1E+100000000","unitPrice":"10"}]}"""
                .formatted("d".repeat(600)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));

        save(session, documentId, """
                {"fields":{"invoiceNumber":"F-1","invoiceDate":"15/03/2026","sellerName":"ACME","buyerName":"B",
                           "vatRate":"20","evilField":"x"},
                 "lines":[{"description":"d","quantity":"1","unitPrice":"10"}]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("Champ inconnu : evilField."));
    }

    @Test
    void staleSaveIsRefusedInsteadOfOverwritingANewerCorrection() throws Exception {
        Cookie session = registerAndLogin("draft-conflict");
        long documentId = extractedDocument(session);

        MvcResult loaded = mockMvc.perform(get("/api/documents/" + documentId + "/draft").cookie(session))
                .andReturn();
        String loadedAt = new ObjectMapper().readTree(loaded.getResponse().getContentAsString())
                .get("updatedAt").asString();
        String withVersion = VALID_CORRECTION.replaceFirst("\\{", "{\"updatedAt\":\"" + loadedAt + "\",");

        // Premiere sauvegarde (onglet A) : acceptee.
        MvcResult first = save(session, documentId, withVersion).andExpect(status().isOk()).andReturn();

        // Seconde sauvegarde avec la meme version (onglet B, reste sur l'ancienne) : refusee.
        save(session, documentId, withVersion)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DRAFT_CONFLICT"));

        // Avec la version renvoyee par la premiere sauvegarde : acceptee.
        String savedAt = new ObjectMapper().readTree(first.getResponse().getContentAsString())
                .get("updatedAt").asString();
        save(session, documentId, VALID_CORRECTION.replaceFirst("\\{", "{\"updatedAt\":\"" + savedAt + "\","))
                .andExpect(status().isOk());
    }

    @Test
    void savingWithoutAnyDraftReturns404() throws Exception {
        Cookie session = registerAndLogin("draft-none");
        long documentId = uploadPdf(session, createOrganization(session));

        save(session, documentId, VALID_CORRECTION)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DRAFT_NOT_FOUND"));
    }

    @Test
    void anOutsiderCannotCorrectADraft() throws Exception {
        Cookie owner = registerAndLogin("draft-owner");
        Cookie outsider = registerAndLogin("draft-outsider");
        long documentId = extractedDocument(owner);

        save(outsider, documentId, VALID_CORRECTION)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }
}
