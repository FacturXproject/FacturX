package com.facturx.app.document;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.ObjectMapper;

/**
 * Covers the F07/F08 link: "Verifier" a document already uploaded via F06/F07
 * (POST /api/documents/{id}/validate) updates its status using its real documentId,
 * and the F09 readable report is then reachable by that same documentId.
 */
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentValidationFlowTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

    private long uploadSample(Cookie session, long orgId, String filename) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "application/pdf", readSample(filename));

        MvcResult uploaded = mockMvc.perform(
                        multipart("/api/documents?organizationId=" + orgId)
                                .file(file)
                                .with(csrf())
                                .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andReturn();

        return new ObjectMapper()
                .readTree(uploaded.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }

    private byte[] readSample(String filename) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(
                "/facturx-samples/" + filename)) {
            if (in == null) {
                throw new IOException("Sample not found on classpath: " + filename);
            }
            return in.readAllBytes();
        }
    }

    @Test
    void validatingAnUploadedDocumentMarksItValidAndExposesAReadableReport() throws Exception {
        Cookie session = registerAndLogin("doc-validate-ok");
        long orgId = createOrganization(session);
        long documentId = uploadSample(session, orgId, "EN16931_Einfach.pdf");

        mockMvc.perform(
                        post("/api/documents/" + documentId + "/validate")
                                .with(csrf())
                                .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.runId").isNotEmpty());

        mockMvc.perform(get("/api/documents/" + documentId).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"));

        mockMvc.perform(get("/api/documents/" + documentId + "/report").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.errorCount").value(0))
                .andExpect(jsonPath("$.errors[0].titleFr").isNotEmpty());
    }

    @Test
    void validatingAnUploadedDocumentMarksItInvalidWhenNonCompliant() throws Exception {
        Cookie session = registerAndLogin("doc-validate-ko");
        long orgId = createOrganization(session);
        long documentId = uploadSample(session, orgId, "veraPDFtestsuite6-7-11-t01-fail-a.pdf");

        mockMvc.perform(
                        post("/api/documents/" + documentId + "/validate")
                                .with(csrf())
                                .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));

        mockMvc.perform(get("/api/documents/" + documentId).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALID"));

        mockMvc.perform(get("/api/documents/" + documentId + "/report").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errorCount").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void reportForADocumentNeverValidatedReturns404() throws Exception {
        Cookie session = registerAndLogin("doc-report-none");
        long orgId = createOrganization(session);
        long documentId = uploadSample(session, orgId, "EN16931_Einfach.pdf");

        mockMvc.perform(get("/api/documents/" + documentId + "/report").cookie(session))
                .andExpect(status().isNotFound());
    }
}
