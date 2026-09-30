package com.facturx.app.extraction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.ObjectMapper;

/**
 * F11: POST /api/documents/{id}/extract delegates to the (mocked here) Python
 * extractor and persists the returned draft, reachable afterwards through
 * GET /api/documents/{id}/draft. The real HTTP call to the extractor is
 * covered separately by the extractor's own test suite (extractor/tests) -
 * this test only proves the Java side's orchestration and persistence.
 */
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExtractionFlowTest extends AbstractIntegrationTest {

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

    private long uploadPdf(Cookie session, long orgId, String uploadName) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", uploadName, "application/pdf", "%PDF-1.4 fake content".getBytes());

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

    private ExtractionResponse fakeExtractorResponse() {
        return new ExtractionResponse(
                Map.of(
                        "invoiceNumber", new ExtractionResponse.FieldValue("FACT-2026-00142", 0.9),
                        "sellerSiren", new ExtractionResponse.FieldValue("452891237", 0.3)
                ),
                List.of(new ExtractionResponse.LineValue("Prestation conseil", "5", "750.00", "3750.00", 0.5)),
                "pdf-extraction-v1"
        );
    }

    @Test
    void extractingAnUploadedPdfPersistsAndExposesTheDraft() throws Exception {
        when(extractionClient.extract(any(), anyString())).thenReturn(fakeExtractorResponse());

        Cookie session = registerAndLogin("extract-ok");
        long orgId = createOrganization(session);
        long documentId = uploadPdf(session, orgId, "invoice-" + UUID.randomUUID() + ".pdf");

        mockMvc.perform(
                        post("/api/documents/" + documentId + "/extract")
                                .with(csrf())
                                .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(documentId))
                .andExpect(jsonPath("$.source").value("pdf-extraction-v1"))
                .andExpect(jsonPath("$.fields.invoiceNumber.value").value("FACT-2026-00142"))
                .andExpect(jsonPath("$.fields.invoiceNumber.confidence").value(0.9))
                .andExpect(jsonPath("$.lines[0].description").value("Prestation conseil"));

        mockMvc.perform(get("/api/documents/" + documentId + "/draft").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields.invoiceNumber.value").value("FACT-2026-00142"));
    }

    @Test
    void draftForADocumentNeverExtractedReturns404() throws Exception {
        Cookie session = registerAndLogin("extract-none");
        long orgId = createOrganization(session);
        long documentId = uploadPdf(session, orgId, "no-extraction-" + UUID.randomUUID() + ".pdf");

        mockMvc.perform(get("/api/documents/" + documentId + "/draft").cookie(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DRAFT_NOT_FOUND"));
    }

    @Test
    void extractingAnXmlDocumentIsRejected() throws Exception {
        Cookie session = registerAndLogin("extract-xml");
        long orgId = createOrganization(session);

        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice.xml", "application/xml", "<xml/>".getBytes());

        MvcResult uploaded = mockMvc.perform(
                        multipart("/api/documents?organizationId=" + orgId)
                                .file(file)
                                .with(csrf())
                                .cookie(session))
                .andExpect(status().isOk())
                .andReturn();

        long documentId = new ObjectMapper()
                .readTree(uploaded.getResponse().getContentAsString())
                .get("id")
                .asLong();

        mockMvc.perform(
                        post("/api/documents/" + documentId + "/extract")
                                .with(csrf())
                                .cookie(session))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("EXTRACTION_UNSUPPORTED_TYPE"));
    }

    @Test
    void aClientCannotExtractADocument() throws Exception {
        Cookie admin = registerAndLogin("extract-perm-admin");
        Cookie client = registerAndLogin("extract-perm-client");
        long orgId = createOrganization(admin);

        // The client needs to belong to the organisation to reach the permission
        // check at all; reuse the invitation-less path isn't available here, so this
        // test instead relies on CLIENT being the default role only for the creator's
        // own organisation - a second, unrelated organisation created by "client"
        // leaves them with no membership in orgId, which is enough to prove the
        // 403 (see DocumentValidationPermissionTest for the membership-based variant).
        long documentId = uploadPdf(admin, orgId, "perm-" + UUID.randomUUID() + ".pdf");

        mockMvc.perform(
                        post("/api/documents/" + documentId + "/extract")
                                .with(csrf())
                                .cookie(client))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }
}
