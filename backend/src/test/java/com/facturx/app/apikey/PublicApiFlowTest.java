package com.facturx.app.apikey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import com.facturx.app.organization.OrganizationMemberRepository;
import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * F17 : API publique securisee - cle valide acceptee, invalide / revoquee refusee,
 * scopes respectes, limite de debit, isolation par organisation, documentation.
 */
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.public-api.rate-limit.requests=" + PublicApiFlowTest.RATE_LIMIT)
class PublicApiFlowTest extends AbstractIntegrationTest {

    static final int RATE_LIMIT = 10;
    private static final String API = "/api/public/v1/documents";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private OrganizationMemberRepository memberRepository;

    // --- helpers -----------------------------------------------------------------

    private Cookie registerAndLogin(String label) throws Exception {
        String email = label + "-" + UUID.randomUUID() + "@x.fr";
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}"""
                                .formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie session = result.getResponse().getCookie("EFACTURE_SESSION");
        assertThat(session).isNotNull();
        return session;
    }

    private long createOrganization(Cookie session) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/organizations?name=Cabinet API")
                        .with(csrf())
                        .cookie(session))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("id").asLong();
    }

    private JsonNode createKeyResponse(Cookie session, long orgId, String... scopes) throws Exception {
        String scopesJson = String.join(",", Arrays.stream(scopes).map(s -> "\"" + s + "\"").toList());
        MvcResult result = mockMvc.perform(post("/api/api-keys")
                        .with(csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ERP","organizationId":%d,"scopes":[%s]}""".formatted(orgId, scopesJson)))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result);
    }

    private String createKey(Cookie session, long orgId, String... scopes) throws Exception {
        return createKeyResponse(session, orgId, scopes).get("key").asString();
    }

    private static JsonNode json(MvcResult result) throws Exception {
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    private static byte[] pdfBytes() {
        return """
                %PDF-1.4
                1 0 obj<</Type/Catalog>>endobj
                trailer<</Root 1 0 R>>
                """.getBytes();
    }

    private long uploadWithKey(String key, String filename) throws Exception {
        // Type declare volontairement generique, comme curl sans ;type=
        MockMultipartFile file = new MockMultipartFile("file", filename, "application/octet-stream", pdfBytes());
        MvcResult result = mockMvc.perform(multipart(API).file(file).header("X-API-Key", key))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value(filename))
                .andExpect(jsonPath("$.type").value("application/pdf"))
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andReturn();
        return json(result).get("id").asLong();
    }

    // --- gestion des cles --------------------------------------------------------

    @Test
    void createdKeyIsShownOnceAndOnlyItsHashIsStored() throws Exception {
        Cookie session = registerAndLogin("key-create");
        long orgId = createOrganization(session);

        JsonNode created = createKeyResponse(session, orgId, "DOCUMENTS_READ");
        String secret = created.get("key").asString();
        long keyId = created.get("apiKey").get("id").asLong();

        assertThat(secret).startsWith("fxk_").hasSize(4 + 43);
        assertThat(created.get("apiKey").get("prefix").asString()).isEqualTo(secret.substring(0, 12));
        assertThat(created.get("apiKey").get("organizationId").asLong()).isEqualTo(orgId);
        assertThat(created.get("apiKey").get("active").asBoolean()).isTrue();

        MvcResult list = mockMvc.perform(get("/api/api-keys").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(keyId))
                .andExpect(jsonPath("$[0].organizationName").value("Cabinet API"))
                .andExpect(jsonPath("$[0].scopes[0]").value("DOCUMENTS_READ"))
                .andReturn();
        assertThat(list.getResponse().getContentAsString()).doesNotContain(secret);

        ApiKey stored = apiKeyRepository.findById(keyId).orElseThrow();
        assertThat(stored.getKeyHash()).hasSize(64).isNotEqualTo(secret).isEqualTo(ApiKeyService.hash(secret));
    }

    @Test
    void keyManagementRequiresASession() throws Exception {
        mockMvc.perform(get("/api/api-keys"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cannotCreateAKeyForAnOrganizationYouDoNotBelongTo() throws Exception {
        Cookie owner = registerAndLogin("key-org-owner");
        long orgId = createOrganization(owner);
        Cookie outsider = registerAndLogin("key-org-outsider");

        mockMvc.perform(post("/api/api-keys")
                        .with(csrf())
                        .cookie(outsider)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ERP","organizationId":%d,"scopes":["DOCUMENTS_READ"]}""".formatted(orgId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void creatingAKeyWithoutScopeOrNameIsRejected() throws Exception {
        Cookie session = registerAndLogin("key-invalid");
        long orgId = createOrganization(session);

        mockMvc.perform(post("/api/api-keys")
                        .with(csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","organizationId":%d,"scopes":[]}""".formatted(orgId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.name").exists())
                .andExpect(jsonPath("$.fields.scopes").exists());
    }

    @Test
    void cannotRevokeSomeoneElsesKey() throws Exception {
        Cookie owner = registerAndLogin("key-revoke-owner");
        long orgId = createOrganization(owner);
        long keyId = createKeyResponse(owner, orgId, "DOCUMENTS_READ").get("apiKey").get("id").asLong();

        Cookie other = registerAndLogin("key-revoke-other");
        mockMvc.perform(delete("/api/api-keys/" + keyId).with(csrf()).cookie(other))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("API_KEY_NOT_FOUND"));
    }

    // --- authentification par cle ------------------------------------------------

    @Test
    void validKeyGivesAccessToListUploadStatusDownloadAndReport() throws Exception {
        Cookie session = registerAndLogin("api-flow");
        long orgId = createOrganization(session);
        String key = createKey(session, orgId, "DOCUMENTS_READ", "DOCUMENTS_WRITE");

        long documentId = uploadWithKey(key, "facture-api.pdf");

        mockMvc.perform(get(API).header("X-API-Key", key))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", String.valueOf(RATE_LIMIT)))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(documentId))
                .andExpect(jsonPath("$.content[0].organizationId").value(orgId));

        mockMvc.perform(get(API + "/" + documentId).header("X-API-Key", key))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"));

        MvcResult download = mockMvc.perform(get(API + "/" + documentId + "/download").header("X-API-Key", key))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn();
        assertThat(download.getResponse().getContentAsByteArray()).isEqualTo(pdfBytes());

        // Pas encore valide : 404 au format JSON de l'API (et pas un 401 venu de /error)
        mockMvc.perform(get(API + "/" + documentId + "/report").header("X-API-Key", key))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));

        // Le document depose par API est un document normal de l'organisation
        mockMvc.perform(get("/api/documents/" + documentId).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("facture-api.pdf"));
    }

    @Test
    void bearerAuthorizationHeaderIsAccepted() throws Exception {
        Cookie session = registerAndLogin("api-bearer");
        long orgId = createOrganization(session);
        String key = createKey(session, orgId, "DOCUMENTS_READ");

        mockMvc.perform(get(API).header("Authorization", "Bearer " + key))
                .andExpect(status().isOk());
    }

    @Test
    void missingOrInvalidKeyIsRejected() throws Exception {
        mockMvc.perform(get(API))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("API_KEY_MISSING"));

        mockMvc.perform(get(API).header("X-API-Key", "fxk_" + "A".repeat(43)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_API_KEY"));

        mockMvc.perform(get(API).header("X-API-Key", "not-a-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_API_KEY"));
    }

    @Test
    void sessionCookieDoesNotAuthenticateThePublicApi() throws Exception {
        Cookie session = registerAndLogin("api-cookie");
        createOrganization(session);

        mockMvc.perform(get(API).cookie(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("API_KEY_MISSING"));
    }

    @Test
    void revokedKeyIsRejected() throws Exception {
        Cookie session = registerAndLogin("api-revoked");
        long orgId = createOrganization(session);
        JsonNode created = createKeyResponse(session, orgId, "DOCUMENTS_READ");
        String key = created.get("key").asString();
        long keyId = created.get("apiKey").get("id").asLong();

        mockMvc.perform(get(API).header("X-API-Key", key))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/api-keys/" + keyId).with(csrf()).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.revokedAt").exists());

        mockMvc.perform(get(API).header("X-API-Key", key))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_API_KEY"));
    }

    @Test
    void keyStopsWorkingWhenItsOwnerLeavesTheOrganization() throws Exception {
        Cookie admin = registerAndLogin("api-leave-admin");
        long orgId = createOrganization(admin);
        String key = createKey(admin, orgId, "DOCUMENTS_READ");

        mockMvc.perform(get(API).header("X-API-Key", key))
                .andExpect(status().isOk());

        // On retire directement le membre : la cle n'a plus d'organisation ou agir.
        ApiKey apiKey = apiKeyRepository.findByKeyHash(ApiKeyService.hash(key)).orElseThrow();
        memberRepository.findByUserIdAndOrganizationId(apiKey.getOwner().getId(), orgId)
                .ifPresent(memberRepository::delete);

        mockMvc.perform(get(API).header("X-API-Key", key))
                .andExpect(status().isUnauthorized());
    }

    // --- scopes ------------------------------------------------------------------

    @Test
    void scopesAreEnforced() throws Exception {
        Cookie session = registerAndLogin("api-scopes");
        long orgId = createOrganization(session);
        String readOnly = createKey(session, orgId, "DOCUMENTS_READ");
        String writeOnly = createKey(session, orgId, "DOCUMENTS_WRITE");

        MockMultipartFile file = new MockMultipartFile("file", "f.pdf", "application/pdf", pdfBytes());
        mockMvc.perform(multipart(API).file(file).header("X-API-Key", readOnly))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_SCOPE"));

        long documentId = uploadWithKey(writeOnly, "f.pdf");

        mockMvc.perform(get(API).header("X-API-Key", writeOnly))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_SCOPE"));

        mockMvc.perform(post(API + "/" + documentId + "/validate").header("X-API-Key", readOnly))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_SCOPE"));
    }

    @Test
    void validateScopeQueuesAValidationJob() throws Exception {
        Cookie session = registerAndLogin("api-validate");
        long orgId = createOrganization(session);
        String key = createKey(session, orgId, "DOCUMENTS_WRITE", "DOCUMENTS_VALIDATE");

        long documentId = uploadWithKey(key, "to-validate.pdf");

        mockMvc.perform(post(API + "/" + documentId + "/validate").header("X-API-Key", key))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.documentId").value(documentId))
                .andExpect(jsonPath("$.type").value("VALIDATION"));
    }

    // --- isolation ---------------------------------------------------------------

    @Test
    void keyCannotReachAnotherOrganizationsDocuments() throws Exception {
        Cookie owner = registerAndLogin("api-iso-owner");
        long orgId = createOrganization(owner);
        String ownerKey = createKey(owner, orgId, "DOCUMENTS_READ", "DOCUMENTS_WRITE");
        long documentId = uploadWithKey(ownerKey, "secret.pdf");

        Cookie outsider = registerAndLogin("api-iso-outsider");
        long otherOrg = createOrganization(outsider);
        String outsiderKey = createKey(outsider, otherOrg, "DOCUMENTS_READ");

        mockMvc.perform(get(API + "/" + documentId).header("X-API-Key", outsiderKey))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DOCUMENT_NOT_FOUND"));

        mockMvc.perform(get(API + "/" + documentId + "/download").header("X-API-Key", outsiderKey))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(API).header("X-API-Key", outsiderKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // --- limite de debit ---------------------------------------------------------

    @Test
    void rateLimitIsEnforcedPerKey() throws Exception {
        Cookie session = registerAndLogin("api-rate");
        long orgId = createOrganization(session);
        String key = createKey(session, orgId, "DOCUMENTS_READ");
        String otherKey = createKey(session, orgId, "DOCUMENTS_READ");

        for (int i = 0; i < RATE_LIMIT; i++) {
            mockMvc.perform(get(API).header("X-API-Key", key))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-RateLimit-Remaining", String.valueOf(RATE_LIMIT - i - 1)));
        }

        mockMvc.perform(get(API).header("X-API-Key", key))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("RATE_LIMITED"));

        // Le quota est par cle : une autre cle n'est pas affectee.
        mockMvc.perform(get(API).header("X-API-Key", otherKey))
                .andExpect(status().isOk());
    }

    // --- documentation -----------------------------------------------------------

    @Test
    void openApiDocumentationIsPublicAndDescribesAtLeastFiveEndpoints() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/docs/openapi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.apiKey.name").value("X-API-Key"))
                .andReturn();

        JsonNode paths = json(result).get("paths");
        int operations = 0;
        for (String path : paths.propertyNames()) {
            assertThat(path).startsWith("/api/public/");
            operations += paths.get(path).size();
        }
        assertThat(operations).isGreaterThanOrEqualTo(5);

        mockMvc.perform(get("/api/docs/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
