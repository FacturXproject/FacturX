package com.facturx.app.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
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
 * Couvre les scenarios principaux de la feature Depot de documents (F06)
 * et Historique des documents (F07).
 */
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentFlowTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static String uniqueEmail(String label) {
        return label + "-" + UUID.randomUUID() + "@x.fr";
    }

    private static String registerBody(String email, String password) {
        return """
                {"email":"%s","password":"%s","firstName":"Jean","lastName":"Dupont"}"""
                .formatted(email, password);
    }

    private static Cookie sessionCookie(MvcResult result) {
        Cookie sessionCookie =
                result.getResponse().getCookie("EFACTURE_SESSION");

        assertThat(sessionCookie)
                .as("EFACTURE_SESSION cookie on response")
                .isNotNull();

        return sessionCookie;
    }

    private Cookie registerAndLogin(String label) throws Exception {
        String email = uniqueEmail(label);

        MvcResult result = mockMvc.perform(
                        post("/api/auth/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerBody(
                                        email,
                                        "correcthorsebattery"
                                )))
                .andExpect(status().isCreated())
                .andReturn();

        return sessionCookie(result);
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

    private static byte[] validPdfBytes() {
        return """
                %PDF-1.4
                1 0 obj<</Type/Catalog>>endobj
                trailer<</Root 1 0 R>>
                """.getBytes();
    }

    private static byte[] validXmlBytes() {
        return """
                <?xml version="1.0"?>
                <facture>
                    <montant>100</montant>
                </facture>
                """.getBytes();
    }

    private static byte[] invalidFileBytes() {
        return "ceci n'est pas un vrai pdf".getBytes();
    }

    @Test
    void uploadingAValidPdfSucceeds() throws Exception {

        Cookie session = registerAndLogin("doc-pdf");
        long orgId = createOrganization(session);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "facture.pdf",
                "application/pdf",
                validPdfBytes()
        );

        mockMvc.perform(
                        multipart(
                                "/api/documents?organizationId=" + orgId
                        )
                                .file(file)
                                .with(csrf())
                                .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename")
                        .value("facture.pdf"))
                .andExpect(jsonPath("$.type")
                        .value("application/pdf"))
                .andExpect(jsonPath("$.status")
                        .value("UPLOADED"))
                .andExpect(jsonPath("$.organizationId")
                        .value(orgId));
    }

    @Test
    void uploadingAValidXmlSucceeds() throws Exception {

        Cookie session = registerAndLogin("doc-xml");
        long orgId = createOrganization(session);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "facture.xml",
                "application/xml",
                validXmlBytes()
        );

        mockMvc.perform(
                        multipart(
                                "/api/documents?organizationId=" + orgId
                        )
                                .file(file)
                                .with(csrf())
                                .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename")
                        .value("facture.xml"));
    }

    @Test
    void uploadingAnInvalidFileTypeIsRejected() throws Exception {

        Cookie session = registerAndLogin("doc-invalid");
        long orgId = createOrganization(session);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                invalidFileBytes()
        );

        mockMvc.perform(
                        multipart(
                                "/api/documents?organizationId=" + orgId
                        )
                                .file(file)
                                .with(csrf())
                                .cookie(session))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error")
                        .value("INVALID_FILE_TYPE"));
    }

    @Test
    void uploadingWithoutASessionIsUnauthorized() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "facture.pdf",
                "application/pdf",
                validPdfBytes()
        );

        mockMvc.perform(
                        multipart(
                                "/api/documents?organizationId=1"
                        )
                                .file(file)
                                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    /*
     * F06 :
     * ancien endpoint de liste de Yanis
     */
    @Test
    void listingMyDocumentsReturnsUploadedFiles() throws Exception {

        Cookie session = registerAndLogin("doc-list");
        long orgId = createOrganization(session);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "facture.pdf",
                "application/pdf",
                validPdfBytes()
        );

        mockMvc.perform(
                        multipart(
                                "/api/documents?organizationId=" + orgId
                        )
                                .file(file)
                                .with(csrf())
                                .cookie(session))
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/documents/mine")
                                .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename")
                        .value("facture.pdf"));
    }

    /*
     * F07 :
     * liste paginee par organisation
     */
    @Test
    void documentHistoryReturnsDocumentsForOrganization()
            throws Exception {

        Cookie session = registerAndLogin("history-list");
        long orgId = createOrganization(session);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "history.pdf",
                "application/pdf",
                validPdfBytes()
        );

        mockMvc.perform(
                        multipart(
                                "/api/documents?organizationId=" + orgId
                        )
                                .file(file)
                                .with(csrf())
                                .cookie(session))
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/documents")
                                .param(
                                        "organizationId",
                                        String.valueOf(orgId)
                                )
                                .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].filename")
                        .value("history.pdf"))
                .andExpect(jsonPath("$.content[0].organizationId")
                        .value(orgId));
    }

    /*
     * F07 :
     * detail d'un document
     */
    @Test
    void documentHistoryDetailReturnsDocument()
            throws Exception {

        Cookie session = registerAndLogin("history-detail");
        long orgId = createOrganization(session);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "detail.pdf",
                "application/pdf",
                validPdfBytes()
        );

        MvcResult uploaded = mockMvc.perform(
                        multipart(
                                "/api/documents?organizationId=" + orgId
                        )
                                .file(file)
                                .with(csrf())
                                .cookie(session))
                .andExpect(status().isOk())
                .andReturn();

        long documentId = new ObjectMapper()
                .readTree(
                        uploaded.getResponse().getContentAsString()
                )
                .get("id")
                .asLong();

        mockMvc.perform(
                        get("/api/documents/" + documentId)
                                .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(documentId))
                .andExpect(jsonPath("$.filename")
                        .value("detail.pdf"))
                .andExpect(jsonPath("$.organizationId")
                        .value(orgId));
    }

    @Test
    void downloadingAndDeletingADocumentWorks()
            throws Exception {

        Cookie session = registerAndLogin("doc-delete");
        long orgId = createOrganization(session);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "facture.pdf",
                "application/pdf",
                validPdfBytes()
        );

        MvcResult uploaded = mockMvc.perform(
                        multipart(
                                "/api/documents?organizationId=" + orgId
                        )
                                .file(file)
                                .with(csrf())
                                .cookie(session))
                .andExpect(status().isOk())
                .andReturn();

        long documentId = new ObjectMapper()
                .readTree(
                        uploaded.getResponse().getContentAsString()
                )
                .get("id")
                .asLong();

        mockMvc.perform(
                        get(
                                "/api/documents/"
                                        + documentId
                                        + "/download"
                        )
                                .cookie(session))
                .andExpect(status().isOk());

        mockMvc.perform(
                        delete(
                                "/api/documents/"
                                        + documentId
                        )
                                .with(csrf())
                                .cookie(session))
                .andExpect(status().isOk());

        mockMvc.perform(
                        get(
                                "/api/documents/"
                                        + documentId
                                        + "/download"
                        )
                                .cookie(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("DOCUMENT_NOT_FOUND"));
    }
}