package com.facturx.app.publicapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * F17 - public API: key management (session), then the public endpoints themselves
 * (X-API-Key). Covers the roadmap's test list - valid key accepted, invalid refused,
 * rate limit, scopes respected - plus organisation isolation and role enforcement.
 */
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicApiFlowTest extends AbstractIntegrationTest {

    private static final String DOCUMENTS = "/api/public/v1/documents";
    private static final String READ = "documents:read";
    private static final String WRITE = "documents:write";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private OrganizationMemberRepository memberRepository;

    @Autowired
    private ApiRateLimiter rateLimiter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private record RegisteredUser(User user, Cookie session) {
    }

    private record CreatedKey(long id, String key) {
    }

    // ---------- helpers ----------

    private RegisteredUser registerAndLogin(String label) throws Exception {
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

        User user = userRepository.findByEmail(email).orElseThrow();
        return new RegisteredUser(user, session);
    }

    private Organization createOrganization(RegisteredUser admin) {
        return organizationService.createOrganization(
                "Cabinet API " + UUID.randomUUID(), admin.user().getId());
    }

    private OrganizationMember addMember(Organization organization, RegisteredUser user, Role role) {
        OrganizationMember member = new OrganizationMember();
        member.setOrganization(organization);
        member.setUser(user.user());
        member.setRole(role);
        return memberRepository.save(member);
    }

    private static String keyBody(long organizationId, String... scopes) {
        return """
                {"name":"Logiciel comptable","organizationId":%d,"scopes":["%s"]}"""
                .formatted(organizationId, String.join("\",\"", scopes));
    }

    private CreatedKey createKey(RegisteredUser user, long organizationId, String... scopes) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/api-keys")
                                .with(csrf())
                                .cookie(user.session())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(keyBody(organizationId, scopes)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CreatedKey(body.get("apiKey").get("id").asLong(), body.get("key").asString());
    }

    private static MockMultipartFile pdf(String filename) {
        return new MockMultipartFile(
                "file", filename, "application/pdf", "%PDF-1.4 fake content".getBytes());
    }

    // No csrf() and no cookie anywhere below: the key alone must be enough.
    private long uploadWithKey(String key, MockMultipartFile file) throws Exception {
        MvcResult result = mockMvc.perform(
                        multipart(DOCUMENTS).file(file).header("X-API-Key", key))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private byte[] readSample(String filename) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/facturx-samples/" + filename)) {
            if (in == null) {
                throw new IOException("Sample not found on classpath: " + filename);
            }
            return in.readAllBytes();
        }
    }

    // ---------- key management (session) ----------

    @Test
    void createReturnsTheKeyOnceAndListNeverExposesIt() throws Exception {
        RegisteredUser admin = registerAndLogin("api-key-create");
        Organization organization = createOrganization(admin);

        MvcResult created = mockMvc.perform(
                        post("/api/api-keys")
                                .with(csrf())
                                .cookie(admin.session())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(keyBody(organization.getId(), READ, WRITE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiKey.name").value("Logiciel comptable"))
                .andExpect(jsonPath("$.apiKey.organizationName").value(organization.getName()))
                .andExpect(jsonPath("$.apiKey.scopes.length()").value(2))
                .andExpect(jsonPath("$.apiKey.revokedAt").doesNotExist())
                .andReturn();

        String key = objectMapper.readTree(created.getResponse().getContentAsString()).get("key").asString();
        assertThat(key).startsWith("fxk_").hasSize(4 + 64);

        MvcResult listed = mockMvc.perform(get("/api/api-keys").cookie(admin.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].keyPrefix").value(key.substring(0, 12)))
                .andReturn();

        // Neither the key nor its hash may ever leave the server again.
        assertThat(listed.getResponse().getContentAsString())
                .doesNotContain(key)
                .doesNotContain("keyHash");
    }

    @Test
    void keyManagementRequiresASession() throws Exception {
        mockMvc.perform(get("/api/api-keys"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cannotCreateAKeyForAnOrganizationYouDoNotBelongTo() throws Exception {
        RegisteredUser admin = registerAndLogin("api-key-owner");
        RegisteredUser outsider = registerAndLogin("api-key-outsider");
        Organization organization = createOrganization(admin);

        mockMvc.perform(
                        post("/api/api-keys")
                                .with(csrf())
                                .cookie(outsider.session())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(keyBody(organization.getId(), READ)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void createRejectsMissingNameUnknownScopeAndEmptyScopes() throws Exception {
        RegisteredUser admin = registerAndLogin("api-key-invalid");
        Organization organization = createOrganization(admin);

        mockMvc.perform(
                        post("/api/api-keys").with(csrf()).cookie(admin.session())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"name":"  ","organizationId":%d,"scopes":["documents:read"]}"""
                                        .formatted(organization.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        mockMvc.perform(
                        post("/api/api-keys").with(csrf()).cookie(admin.session())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"name":"Cle","organizationId":%d,"scopes":[]}"""
                                        .formatted(organization.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        mockMvc.perform(
                        post("/api/api-keys").with(csrf()).cookie(admin.session())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(keyBody(organization.getId(), "documents:admin")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aUserCannotRevokeSomeoneElsesKey() throws Exception {
        RegisteredUser admin = registerAndLogin("api-key-revoke-owner");
        RegisteredUser other = registerAndLogin("api-key-revoke-other");
        Organization organization = createOrganization(admin);
        CreatedKey key = createKey(admin, organization.getId(), READ);

        mockMvc.perform(delete("/api/api-keys/" + key.id()).with(csrf()).cookie(other.session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("API_KEY_NOT_FOUND"));

        // Still usable: the refused revocation changed nothing.
        mockMvc.perform(get(DOCUMENTS).header("X-API-Key", key.key()))
                .andExpect(status().isOk());
    }

    // ---------- authentication ----------

    @Test
    void validKeyIsAccepted() throws Exception {
        RegisteredUser admin = registerAndLogin("api-valid");
        Organization organization = createOrganization(admin);
        CreatedKey key = createKey(admin, organization.getId(), READ);

        mockMvc.perform(get(DOCUMENTS).header("X-API-Key", key.key()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalItems").value(0));

        // Using the key is recorded, so its owner can spot an unused or a leaked one.
        mockMvc.perform(get("/api/api-keys").cookie(admin.session()))
                .andExpect(jsonPath("$[0].lastUsedAt").isNotEmpty());
    }

    @Test
    void missingKeyIsRefused() throws Exception {
        mockMvc.perform(get(DOCUMENTS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("API_KEY_REQUIRED"));
    }

    @Test
    void unknownKeyIsRefused() throws Exception {
        mockMvc.perform(get(DOCUMENTS).header("X-API-Key", "fxk_" + "0".repeat(64)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_API_KEY"));
    }

    @Test
    void aSessionCookieIsNotAnApiKey() throws Exception {
        RegisteredUser admin = registerAndLogin("api-cookie-only");
        createOrganization(admin);

        mockMvc.perform(get(DOCUMENTS).cookie(admin.session()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("API_KEY_REQUIRED"));
    }

    @Test
    void anApiKeyDoesNotOpenTheSessionApi() throws Exception {
        RegisteredUser admin = registerAndLogin("api-key-on-session-api");
        Organization organization = createOrganization(admin);
        CreatedKey key = createKey(admin, organization.getId(), READ, WRITE);

        mockMvc.perform(get("/api/api-keys").header("X-API-Key", key.key()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users").header("X-API-Key", key.key()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokedKeyIsRefused() throws Exception {
        RegisteredUser admin = registerAndLogin("api-revoked");
        Organization organization = createOrganization(admin);
        CreatedKey key = createKey(admin, organization.getId(), READ);

        mockMvc.perform(get(DOCUMENTS).header("X-API-Key", key.key()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/api-keys/" + key.id()).with(csrf()).cookie(admin.session()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(DOCUMENTS).header("X-API-Key", key.key()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_API_KEY"));

        // The revoked key stays listed, marked as revoked.
        mockMvc.perform(get("/api/api-keys").cookie(admin.session()))
                .andExpect(jsonPath("$[0].revokedAt").isNotEmpty());
    }

    @Test
    void keyOfADisabledAccountIsRefused() throws Exception {
        RegisteredUser admin = registerAndLogin("api-disabled");
        Organization organization = createOrganization(admin);
        CreatedKey key = createKey(admin, organization.getId(), READ);

        User user = userRepository.findById(admin.user().getId()).orElseThrow();
        user.setStatus(User.STATUS_DISABLED);
        userRepository.save(user);

        mockMvc.perform(get(DOCUMENTS).header("X-API-Key", key.key()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_API_KEY"));
    }

    // ---------- scopes ----------

    @Test
    void readOnlyKeyCannotWrite() throws Exception {
        RegisteredUser admin = registerAndLogin("api-scope-read");
        Organization organization = createOrganization(admin);
        CreatedKey readKey = createKey(admin, organization.getId(), READ);
        CreatedKey writeKey = createKey(admin, organization.getId(), WRITE);
        long documentId = uploadWithKey(writeKey.key(), pdf("scope-" + UUID.randomUUID() + ".pdf"));

        mockMvc.perform(multipart(DOCUMENTS).file(pdf("refused.pdf")).header("X-API-Key", readKey.key()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_SCOPE"));

        mockMvc.perform(put(DOCUMENTS + "/" + documentId).header("X-API-Key", readKey.key())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"filename\":\"x.pdf\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_SCOPE"));

        mockMvc.perform(delete(DOCUMENTS + "/" + documentId).header("X-API-Key", readKey.key()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_SCOPE"));

        mockMvc.perform(post(DOCUMENTS + "/" + documentId + "/validate").header("X-API-Key", readKey.key()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_SCOPE"));

        // ...but it does read.
        mockMvc.perform(get(DOCUMENTS + "/" + documentId).header("X-API-Key", readKey.key()))
                .andExpect(status().isOk());
    }

    @Test
    void writeOnlyKeyCannotRead() throws Exception {
        RegisteredUser admin = registerAndLogin("api-scope-write");
        Organization organization = createOrganization(admin);
        CreatedKey writeKey = createKey(admin, organization.getId(), WRITE);
        long documentId = uploadWithKey(writeKey.key(), pdf("write-only-" + UUID.randomUUID() + ".pdf"));

        mockMvc.perform(get(DOCUMENTS).header("X-API-Key", writeKey.key()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_SCOPE"));

        mockMvc.perform(get(DOCUMENTS + "/" + documentId + "/download").header("X-API-Key", writeKey.key()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_SCOPE"));
    }

    // ---------- the endpoints ----------

    @Test
    void uploadListGetRenameDownloadDelete() throws Exception {
        RegisteredUser admin = registerAndLogin("api-flow");
        Organization organization = createOrganization(admin);
        CreatedKey key = createKey(admin, organization.getId(), READ, WRITE);
        String filename = "flow-" + UUID.randomUUID() + ".pdf";

        long documentId = uploadWithKey(key.key(), pdf(filename));

        mockMvc.perform(get(DOCUMENTS).header("X-API-Key", key.key()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].id").value(documentId))
                .andExpect(jsonPath("$.items[0].filename").value(filename));

        mockMvc.perform(get(DOCUMENTS + "/" + documentId).header("X-API-Key", key.key()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.organizationId").value(organization.getId()))
                .andExpect(jsonPath("$.ownerId").value(admin.user().getId()));

        mockMvc.perform(put(DOCUMENTS + "/" + documentId).header("X-API-Key", key.key())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filename\":\"facture-2026-001.pdf\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("facture-2026-001.pdf"));

        MvcResult downloaded = mockMvc.perform(
                        get(DOCUMENTS + "/" + documentId + "/download").header("X-API-Key", key.key()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("facture-2026-001.pdf")))
                .andReturn();
        assertThat(downloaded.getResponse().getContentAsByteArray())
                .isEqualTo("%PDF-1.4 fake content".getBytes());

        // Never validated yet: there is no report to read.
        mockMvc.perform(get(DOCUMENTS + "/" + documentId + "/report").header("X-API-Key", key.key()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(delete(DOCUMENTS + "/" + documentId).header("X-API-Key", key.key()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(DOCUMENTS + "/" + documentId).header("X-API-Key", key.key()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DOCUMENT_NOT_FOUND"));
    }

    @Test
    void validateThenReadTheReport() throws Exception {
        RegisteredUser admin = registerAndLogin("api-validate");
        Organization organization = createOrganization(admin);
        CreatedKey key = createKey(admin, organization.getId(), READ, WRITE);

        long documentId = uploadWithKey(key.key(), new MockMultipartFile(
                "file", "api-" + UUID.randomUUID() + ".pdf", "application/pdf",
                readSample("EN16931_Einfach.pdf")));

        mockMvc.perform(post(DOCUMENTS + "/" + documentId + "/validate").header("X-API-Key", key.key()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        mockMvc.perform(get(DOCUMENTS + "/" + documentId).header("X-API-Key", key.key()))
                .andExpect(jsonPath("$.status").value("VALID"));

        mockMvc.perform(get(DOCUMENTS + "/" + documentId + "/report").header("X-API-Key", key.key()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.errorCount").value(0));
    }

    @Test
    void uploadRejectsAFileThatIsNeitherPdfNorXml() throws Exception {
        RegisteredUser admin = registerAndLogin("api-bad-file");
        Organization organization = createOrganization(admin);
        CreatedKey key = createKey(admin, organization.getId(), WRITE);

        MockMultipartFile notAPdf = new MockMultipartFile(
                "file", "facture.pdf", "application/pdf", "just some text".getBytes());

        mockMvc.perform(multipart(DOCUMENTS).file(notAPdf).header("X-API-Key", key.key()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("INVALID_FILE_TYPE"));
    }

    @Test
    void renameRejectsAnInvalidFilename() throws Exception {
        RegisteredUser admin = registerAndLogin("api-bad-name");
        Organization organization = createOrganization(admin);
        CreatedKey key = createKey(admin, organization.getId(), WRITE);
        long documentId = uploadWithKey(key.key(), pdf("name-" + UUID.randomUUID() + ".pdf"));

        mockMvc.perform(put(DOCUMENTS + "/" + documentId).header("X-API-Key", key.key())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"filename\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        mockMvc.perform(put(DOCUMENTS + "/" + documentId).header("X-API-Key", key.key())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"filename\":\"../../etc/passwd\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    // These are answered by Spring itself (sendError), not by one of our exception
    // handlers. They must still come back as a JSON error with the real status - see
    // JsonErrorResponseWrapper for what happens otherwise behind a real server.
    @Test
    void malformedRequestsGetAJsonErrorWithTheRealStatus() throws Exception {
        RegisteredUser admin = registerAndLogin("api-malformed");
        Organization organization = createOrganization(admin);
        CreatedKey key = createKey(admin, organization.getId(), READ, WRITE);
        long documentId = uploadWithKey(key.key(), pdf("malformed-" + UUID.randomUUID() + ".pdf"));

        mockMvc.perform(get(DOCUMENTS + "/abc").header("X-API-Key", key.key()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        mockMvc.perform(get(DOCUMENTS + "?page=x").header("X-API-Key", key.key()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        mockMvc.perform(multipart(DOCUMENTS)
                        .file(new MockMultipartFile("other", "x.pdf", "application/pdf", "%PDF-".getBytes()))
                        .header("X-API-Key", key.key()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        mockMvc.perform(put(DOCUMENTS + "/" + documentId).header("X-API-Key", key.key())
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        mockMvc.perform(put(DOCUMENTS + "/" + documentId).header("X-API-Key", key.key())
                        .contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_MEDIA_TYPE"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch(DOCUMENTS + "/" + documentId).header("X-API-Key", key.key()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"));

        mockMvc.perform(get("/api/public/v1/unknown").header("X-API-Key", key.key()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void listIsPaginated() throws Exception {
        RegisteredUser admin = registerAndLogin("api-page");
        Organization organization = createOrganization(admin);
        CreatedKey key = createKey(admin, organization.getId(), READ, WRITE);

        for (int i = 0; i < 3; i++) {
            uploadWithKey(key.key(), pdf("page-" + i + "-" + UUID.randomUUID() + ".pdf"));
        }

        mockMvc.perform(get(DOCUMENTS + "?page=0&size=2").header("X-API-Key", key.key()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get(DOCUMENTS + "?page=1&size=2").header("X-API-Key", key.key()))
                .andExpect(jsonPath("$.items.length()").value(1));

        // An absurd size is capped rather than obeyed.
        mockMvc.perform(get(DOCUMENTS + "?size=100000").header("X-API-Key", key.key()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    // ---------- organisation isolation and roles ----------

    @Test
    void aKeyNeverReachesAnotherOrganizationsDocuments() throws Exception {
        RegisteredUser adminA = registerAndLogin("api-iso-a");
        RegisteredUser adminB = registerAndLogin("api-iso-b");
        Organization organizationA = createOrganization(adminA);
        Organization organizationB = createOrganization(adminB);
        CreatedKey keyA = createKey(adminA, organizationA.getId(), READ, WRITE);
        CreatedKey keyB = createKey(adminB, organizationB.getId(), READ, WRITE);

        long documentOfB = uploadWithKey(keyB.key(), pdf("iso-" + UUID.randomUUID() + ".pdf"));

        mockMvc.perform(get(DOCUMENTS).header("X-API-Key", keyA.key()))
                .andExpect(jsonPath("$.totalItems").value(0));

        // 404, not 403: key A must not even learn that the document exists.
        String path = DOCUMENTS + "/" + documentOfB;
        mockMvc.perform(get(path).header("X-API-Key", keyA.key()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(path + "/download").header("X-API-Key", keyA.key()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(path + "/report").header("X-API-Key", keyA.key()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(path + "/validate").header("X-API-Key", keyA.key()))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(path).header("X-API-Key", keyA.key())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"filename\":\"stolen.pdf\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(path).header("X-API-Key", keyA.key()))
                .andExpect(status().isNotFound());

        // Untouched for its real owner.
        mockMvc.perform(get(path).header("X-API-Key", keyB.key()))
                .andExpect(status().isOk());
    }

    @Test
    void aKeyIsLimitedToItsOwnersRole() throws Exception {
        RegisteredUser admin = registerAndLogin("api-role-admin");
        RegisteredUser client = registerAndLogin("api-role-client");
        Organization organization = createOrganization(admin);
        addMember(organization, client, Role.CLIENT);

        CreatedKey adminKey = createKey(admin, organization.getId(), READ, WRITE);
        CreatedKey clientKey = createKey(client, organization.getId(), READ, WRITE);

        long adminDocument = uploadWithKey(adminKey.key(), pdf("admin-" + UUID.randomUUID() + ".pdf"));
        long clientDocument = uploadWithKey(clientKey.key(), pdf("client-" + UUID.randomUUID() + ".pdf"));

        // A client sees only its own documents...
        mockMvc.perform(get(DOCUMENTS).header("X-API-Key", clientKey.key()))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].id").value(clientDocument));
        mockMvc.perform(get(DOCUMENTS + "/" + adminDocument).header("X-API-Key", clientKey.key()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));

        // ...cannot validate (no VALIDATE_DOCUMENT), even with the write scope...
        mockMvc.perform(post(DOCUMENTS + "/" + clientDocument + "/validate").header("X-API-Key", clientKey.key()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));

        // ...and cannot rename or delete someone else's document.
        mockMvc.perform(put(DOCUMENTS + "/" + adminDocument).header("X-API-Key", clientKey.key())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"filename\":\"x.pdf\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(DOCUMENTS + "/" + adminDocument).header("X-API-Key", clientKey.key()))
                .andExpect(status().isForbidden());

        // The admin sees and manages both.
        mockMvc.perform(get(DOCUMENTS).header("X-API-Key", adminKey.key()))
                .andExpect(jsonPath("$.totalItems").value(2));
        mockMvc.perform(delete(DOCUMENTS + "/" + clientDocument).header("X-API-Key", adminKey.key()))
                .andExpect(status().isNoContent());
    }

    @Test
    void aKeyLosesAccessWhenItsOwnerLeavesTheOrganization() throws Exception {
        RegisteredUser admin = registerAndLogin("api-left-admin");
        RegisteredUser accountant = registerAndLogin("api-left-accountant");
        Organization organization = createOrganization(admin);
        OrganizationMember membership = addMember(organization, accountant, Role.ACCOUNTANT);
        CreatedKey key = createKey(accountant, organization.getId(), READ, WRITE);

        mockMvc.perform(get(DOCUMENTS).header("X-API-Key", key.key()))
                .andExpect(status().isOk());

        memberRepository.delete(membership);

        // The key was never revoked, yet it opens nothing any more.
        mockMvc.perform(get(DOCUMENTS).header("X-API-Key", key.key()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
        mockMvc.perform(multipart(DOCUMENTS).file(pdf("late.pdf")).header("X-API-Key", key.key()))
                .andExpect(status().isForbidden());
    }

    // ---------- rate limit ----------

    @Test
    void requestsBeyondTheLimitAreRefusedPerKey() throws Exception {
        RegisteredUser admin = registerAndLogin("api-rate");
        Organization organization = createOrganization(admin);
        CreatedKey key = createKey(admin, organization.getId(), READ);
        CreatedKey otherKey = createKey(admin, organization.getId(), READ);
        int limit = rateLimiter.getRequestsPerMinute();

        mockMvc.perform(get(DOCUMENTS).header("X-API-Key", key.key()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", String.valueOf(limit)))
                .andExpect(header().string("X-RateLimit-Remaining", String.valueOf(limit - 1)));

        for (int i = 1; i < limit; i++) {
            mockMvc.perform(get(DOCUMENTS).header("X-API-Key", key.key()))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get(DOCUMENTS).header("X-API-Key", key.key()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));

        // The limit is per key: another key of the same user is unaffected.
        mockMvc.perform(get(DOCUMENTS).header("X-API-Key", otherKey.key()))
                .andExpect(status().isOk());
    }

    // ---------- documentation ----------

    @Test
    void openApiDescriptionIsPublicAndCoversOnlyThePublicApi() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/public/v1/openapi"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode spec = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode paths = spec.get("paths");

        assertThat(paths.has(DOCUMENTS)).isTrue();
        assertThat(paths.get(DOCUMENTS).has("get")).isTrue();
        assertThat(paths.get(DOCUMENTS).has("post")).isTrue();
        assertThat(paths.get(DOCUMENTS + "/{id}").has("get")).isTrue();
        assertThat(paths.get(DOCUMENTS + "/{id}").has("put")).isTrue();
        assertThat(paths.get(DOCUMENTS + "/{id}").has("delete")).isTrue();
        assertThat(paths.has(DOCUMENTS + "/{id}/validate")).isTrue();
        assertThat(paths.has(DOCUMENTS + "/{id}/report")).isTrue();
        assertThat(paths.has(DOCUMENTS + "/{id}/download")).isTrue();

        // The session API of our own frontend is not part of the public contract.
        paths.propertyNames().forEach(path -> assertThat(path).startsWith("/api/public/v1/"));

        assertThat(spec.get("components").get("securitySchemes").get("ApiKey").get("name").asString())
                .isEqualTo("X-API-Key");
    }

    @Test
    void swaggerUiIsReachableWithoutAKey() throws Exception {
        MvcResult redirect = mockMvc.perform(get("/api/public/docs"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = redirect.getResponse().getHeader("Location");
        assertThat(location).startsWith("/api/public/");

        mockMvc.perform(get(location))
                .andExpect(status().isOk());
    }
}
