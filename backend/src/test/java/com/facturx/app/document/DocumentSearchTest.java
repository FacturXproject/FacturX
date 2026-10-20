package com.facturx.app.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import com.facturx.app.organization.Organization;
import com.facturx.app.organization.OrganizationMember;
import com.facturx.app.organization.OrganizationMemberRepository;
import com.facturx.app.organization.OrganizationService;
import com.facturx.app.organization.Role;
import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import com.facturx.app.validation.ValidationErrorEntity;
import com.facturx.app.validation.ValidationErrorRepository;
import com.facturx.app.validation.ValidationLayer;
import com.facturx.app.validation.ValidationRun;
import com.facturx.app.validation.ValidationRunRepository;
import com.facturx.app.validation.ValidationSeverity;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * F16 - GET /api/documents/search: filters combine, pagination is stable,
 * organisation isolation and role scoping hold, invalid filters are rejected.
 * Documents are inserted straight through the repository (no file on disk needed).
 */
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentSearchTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private OrganizationService organizationService;
    @Autowired private OrganizationMemberRepository memberRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private ValidationRunRepository runRepository;
    @Autowired private ValidationErrorRepository errorRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private record Account(User user, Cookie session) {}

    private Account admin;
    private Account client;
    private Account outsider;
    private Organization organization;
    private Organization otherOrganization;

    private Document invoiceValid;     // admin, PDF, VALID, 2026-03-10
    private Document invoiceInvalid;   // admin, PDF, INVALID (BR-02), 2026-03-15
    private Document clientXml;        // client, XML, UPLOADED, 2026-04-01
    private Document clientPdf;        // client, PDF, INVALID (BR-CO-15), 2026-04-20

    @BeforeEach
    void setUp() throws Exception {
        admin = register("admin");
        client = register("client");
        outsider = register("outsider");

        organization = organizationService.createOrganization("Cabinet-" + UUID.randomUUID(), admin.user().getId());
        addMember(organization, client, Role.CLIENT);
        otherOrganization = organizationService.createOrganization("Autre-" + UUID.randomUUID(), outsider.user().getId());

        invoiceValid = document(organization, admin, "Facture_ACME_2026-001.pdf", "application/pdf",
                DocumentStatus.VALID, "2026-03-10T10:00:00Z");
        invoiceInvalid = document(organization, admin, "facture_acme_2026-002.pdf", "application/pdf",
                DocumentStatus.INVALID, "2026-03-15T10:00:00Z");
        clientXml = document(organization, client, "avoir-client.xml", "text/xml",
                DocumentStatus.UPLOADED, "2026-04-01T10:00:00Z");
        clientPdf = document(organization, client, "client_100%_total.pdf", "application/pdf",
                DocumentStatus.INVALID, "2026-04-20T10:00:00Z");
        // Same name and status in another organisation: must never leak.
        document(otherOrganization, outsider, "Facture_ACME_2026-001.pdf", "application/pdf",
                DocumentStatus.INVALID, "2026-03-10T10:00:00Z");

        validationRun(invoiceInvalid, "BR-02", "An Invoice shall have an Invoice number (BT-1).");
        validationRun(clientPdf, "BR-CO-15", "Invoice total amount with VAT (BT-112) = ...");
    }

    // --- filters ----------------------------------------------------------------------

    @Test
    void findsByFilenameCaseInsensitiveSubstring() throws Exception {
        assertThat(ids(search(admin, "q", "ACME"))).containsExactlyInAnyOrder(invoiceValid.getId(), invoiceInvalid.getId());
        assertThat(ids(search(admin, "q", "2026-002"))).containsExactly(invoiceInvalid.getId());
    }

    @Test
    void likeWildcardsInQueryAreLiteral() throws Exception {
        assertThat(ids(search(admin, "q", "100%"))).containsExactly(clientPdf.getId());
        assertThat(ids(search(admin, "q", "%"))).containsExactly(clientPdf.getId());
        assertThat(ids(search(admin, "q", "_acme_"))).containsExactlyInAnyOrder(invoiceValid.getId(), invoiceInvalid.getId());
    }

    @Test
    void findsByStatusWithSeveralValues() throws Exception {
        assertThat(ids(search(admin, "status", "INVALID")))
                .containsExactlyInAnyOrder(invoiceInvalid.getId(), clientPdf.getId());
        assertThat(ids(search(admin, "status", "valid,uploaded")))
                .containsExactlyInAnyOrder(invoiceValid.getId(), clientXml.getId());
    }

    @Test
    void findsByType() throws Exception {
        assertThat(ids(search(admin, "type", "XML"))).containsExactly(clientXml.getId());
        assertThat(ids(search(admin, "type", "pdf"))).hasSize(3);
    }

    @Test
    void findsByInclusiveDateRange() throws Exception {
        assertThat(ids(search(admin, "from", "2026-03-15", "to", "2026-04-01")))
                .containsExactlyInAnyOrder(invoiceInvalid.getId(), clientXml.getId());
    }

    @Test
    void findsByOwner() throws Exception {
        assertThat(ids(search(admin, "ownerId", client.user().getId().toString())))
                .containsExactlyInAnyOrder(clientXml.getId(), clientPdf.getId());
    }

    @Test
    void findsByErrorCodeRawMessageOrFrenchTitle() throws Exception {
        assertThat(ids(search(admin, "error", "br-02"))).containsExactly(invoiceInvalid.getId());
        assertThat(ids(search(admin, "error", "invoice number"))).containsExactly(invoiceInvalid.getId());
        // French title of BR-02 seeded in rule_catalog (data.sql)
        assertThat(ids(search(admin, "error", "numéro de facture"))).containsExactly(invoiceInvalid.getId());
        assertThat(ids(search(admin, "error", "BR-"))).containsExactlyInAnyOrder(invoiceInvalid.getId(), clientPdf.getId());
    }

    @Test
    void errorSearchOnlyLooksAtLatestRun() throws Exception {
        // invoiceInvalid gets fixed: a newer run with no errors
        ValidationRun fixed = new ValidationRun();
        fixed.setDocumentId(invoiceInvalid.getId());
        fixed.setFilename(invoiceInvalid.getFilename());
        fixed.setValid(true);
        runRepository.save(fixed);

        assertThat(ids(search(admin, "error", "BR-02"))).isEmpty();
    }

    @Test
    void filtersCombineWithAnd() throws Exception {
        assertThat(ids(search(admin, "q", "client", "status", "INVALID", "type", "PDF", "error", "BR-CO-15",
                "from", "2026-04-01", "to", "2026-04-30")))
                .containsExactly(clientPdf.getId());
        assertThat(ids(search(admin, "q", "acme", "type", "XML"))).isEmpty();
    }

    // --- sort & pagination ------------------------------------------------------------

    @Test
    void defaultSortIsNewestFirst() throws Exception {
        assertThat(ids(search(admin)))
                .containsExactly(clientPdf.getId(), clientXml.getId(), invoiceInvalid.getId(), invoiceValid.getId());
    }

    @Test
    void sortsByFilenameInBothDirections() throws Exception {
        String clientId = client.user().getId().toString();
        JsonNode asc = search(admin, "ownerId", clientId, "sort", "filename,asc");
        assertThat(asc.get("sort").asString()).isEqualTo("filename,asc");
        assertThat(ids(asc)).containsExactly(clientXml.getId(), clientPdf.getId());
        assertThat(ids(search(admin, "ownerId", clientId, "sort", "filename,desc")))
                .containsExactly(clientPdf.getId(), clientXml.getId());
    }

    @Test
    void paginationIsStableWhenSortKeysTie() throws Exception {
        // 25 documents sharing the exact same date and status: only the id tie-breaker orders them
        Set<Long> created = new HashSet<>();
        for (int i = 0; i < 25; i++) {
            created.add(document(organization, admin, "lot-" + i + ".pdf", "application/pdf",
                    DocumentStatus.VALID, "2026-05-01T08:00:00Z").getId());
        }

        for (String sort : List.of("uploadedAt,desc", "status,asc")) {
            List<Long> seen = new ArrayList<>();
            for (int page = 0; page < 3; page++) {
                JsonNode body = search(admin, "q", "lot-", "sort", sort, "size", "10", "page", String.valueOf(page));
                assertThat(body.get("totalElements").asLong()).isEqualTo(25);
                assertThat(body.get("totalPages").asInt()).isEqualTo(3);
                seen.addAll(ids(body));
            }
            assertThat(seen).as("every document exactly once with sort " + sort).doesNotHaveDuplicates();
            assertThat(new HashSet<>(seen)).isEqualTo(created);
            // and the same request twice gives the same page
            assertThat(ids(search(admin, "q", "lot-", "sort", sort, "size", "10", "page", "1")))
                    .isEqualTo(seen.subList(10, 20));
        }
    }

    // --- organisation isolation & roles -----------------------------------------------

    @Test
    void neverReturnsDocumentsOfAnotherOrganisation() throws Exception {
        JsonNode body = search(admin, "q", "Facture_ACME_2026-001");
        assertThat(ids(body)).containsExactly(invoiceValid.getId());
        for (JsonNode doc : body.get("content")) {
            assertThat(doc.get("organizationId").asLong()).isEqualTo(organization.getId());
        }
    }

    @Test
    void nonMemberIsForbidden() throws Exception {
        mockMvc.perform(searchRequest(outsider, organization.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void unauthenticatedIsRejected() throws Exception {
        mockMvc.perform(get("/api/documents/search").param("organizationId", organization.getId().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clientOnlySeesOwnDocuments() throws Exception {
        assertThat(ids(search(client))).containsExactlyInAnyOrder(clientXml.getId(), clientPdf.getId());
        assertThat(ids(search(client, "q", "acme"))).isEmpty();
        assertThat(ids(search(client, "ownerId", client.user().getId().toString())))
                .containsExactlyInAnyOrder(clientXml.getId(), clientPdf.getId());
    }

    @Test
    void clientCannotFilterOnSomeoneElse() throws Exception {
        mockMvc.perform(searchRequest(client, organization.getId())
                        .param("ownerId", admin.user().getId().toString()))
                .andExpect(status().isForbidden());
    }

    // --- validation -------------------------------------------------------------------

    @Test
    void rejectsInvalidFilters() throws Exception {
        String[][] invalid = {
                {"status", "DONE"},
                {"type", "DOCX"},
                {"from", "15/03/2026"},
                {"ownerId", "abc"},
                {"sort", "storagePath,asc"},
                {"sort", "filename,sideways"},
                {"size", "0"},
                {"size", "101"},
                {"page", "-1"},
                {"q", "x".repeat(256)},
        };
        for (String[] param : invalid) {
            mockMvc.perform(searchRequest(admin, organization.getId()).param(param[0], param[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_SEARCH_FILTER"))
                    .andExpect(jsonPath("$.field").value(param[0]));
        }
    }

    @Test
    void rejectsInvertedDateRange() throws Exception {
        mockMvc.perform(searchRequest(admin, organization.getId())
                        .param("from", "2026-05-01").param("to", "2026-04-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("from"));
    }

    @Test
    void rejectsMissingOrganisation() throws Exception {
        mockMvc.perform(get("/api/documents/search").cookie(admin.session()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("organizationId"));
    }

    @Test
    void indexesExist() {
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename IN ('documents', 'validation_runs', 'validation_errors')",
                String.class);
        assertThat(indexes).contains(
                "idx_documents_org_uploaded_at", "idx_documents_org_status", "idx_documents_org_type",
                "idx_documents_org_filename", "idx_documents_filename_trgm",
                "idx_validation_runs_document", "idx_validation_errors_run");
    }

    // --- helpers ----------------------------------------------------------------------

    private Account register(String label) throws Exception {
        String email = label + "-" + UUID.randomUUID() + "@x.fr";
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correcthorsebattery","firstName":"Test","lastName":"User"}"""
                                .formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie session = result.getResponse().getCookie("EFACTURE_SESSION");
        assertThat(session).isNotNull();
        return new Account(userRepository.findByEmail(email).orElseThrow(), session);
    }

    private void addMember(Organization org, Account account, Role role) {
        OrganizationMember member = new OrganizationMember();
        member.setOrganization(org);
        member.setUser(account.user());
        member.setRole(role);
        memberRepository.save(member);
    }

    private Document document(Organization org, Account owner, String filename, String type,
                              DocumentStatus status, String uploadedAt) {
        Document document = new Document();
        document.setOrganization(org);
        document.setOwner(owner.user());
        document.setFilename(filename);
        document.setType(type);
        document.setSize(1024);
        document.setStatus(status);
        document.setStoragePath("/nonexistent/" + filename);
        document = documentRepository.save(document);
        jdbcTemplate.update("UPDATE documents SET uploaded_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse(uploadedAt)), document.getId());
        return document;
    }

    private void validationRun(Document document, String ruleCode, String message) {
        ValidationRun run = new ValidationRun();
        run.setDocumentId(document.getId());
        run.setFilename(document.getFilename());
        run.setValid(false);
        run.setLayerReached(ValidationLayer.SCHEMATRON);
        run = runRepository.save(run);

        ValidationErrorEntity error = new ValidationErrorEntity();
        error.setRun(run);
        error.setLayer(ValidationLayer.SCHEMATRON);
        error.setSeverity(ValidationSeverity.ERROR);
        error.setRuleCode(ruleCode);
        error.setMessage(message);
        errorRepository.save(error);
    }

    private MockHttpServletRequestBuilder searchRequest(Account account, Long organizationId) {
        return get("/api/documents/search")
                .cookie(account.session())
                .param("organizationId", organizationId.toString());
    }

    private JsonNode search(Account account, String... params) throws Exception {
        MockHttpServletRequestBuilder request = searchRequest(account, organization.getId());
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static List<Long> ids(JsonNode body) {
        List<Long> ids = new ArrayList<>();
        body.get("content").forEach(doc -> ids.add(doc.get("id").asLong()));
        return ids;
    }
}
