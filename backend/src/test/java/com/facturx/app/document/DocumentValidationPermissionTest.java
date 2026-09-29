package com.facturx.app.document;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
 * POST /api/documents/{id}/validate must require VALIDATE_DOCUMENT (see
 * {@link com.facturx.app.permission.Permission}): a CLIENT has UPLOAD_DOCUMENT and
 * VIEW_OWN_DOCUMENTS but not VALIDATE_DOCUMENT (see PermissionService), and a user with
 * no membership in the document's organization has no permission at all - both must be
 * rejected with 403 via AccessDeniedException, before FacturXValidationService ever
 * runs.
 */
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentValidationPermissionTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private OrganizationMemberRepository memberRepository;

    private record RegisteredUser(User user, Cookie session) {
    }

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

    private void addMember(Organization organization, RegisteredUser user, Role role) {
        OrganizationMember member = new OrganizationMember();
        member.setOrganization(organization);
        member.setUser(user.user());
        member.setRole(role);
        memberRepository.save(member);
    }

    // uploadName is distinct per call (see callers) - findFirst()-by-filename lookups
    // elsewhere in the suite (FacturXValidationServiceTest) share this same test
    // database, so reusing "EN16931_Einfach.pdf" as the stored document name would risk
    // colliding with those.
    private long uploadSample(Cookie session, long orgId, String resourceName, String uploadName) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", uploadName, "application/pdf", readSample(resourceName));

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

    private byte[] readSample(String filename) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/facturx-samples/" + filename)) {
            if (in == null) {
                throw new IOException("Sample not found on classpath: " + filename);
            }
            return in.readAllBytes();
        }
    }

    @Test
    void clientCannotValidateADocument() throws Exception {
        RegisteredUser admin = registerAndLogin("doc-validate-perm-admin");
        RegisteredUser client = registerAndLogin("doc-validate-perm-client");

        Organization organization = organizationService.createOrganization(
                "Cabinet Test " + UUID.randomUUID(), admin.user().getId());
        addMember(organization, client, Role.CLIENT);

        long documentId = uploadSample(admin.session(), organization.getId(),
                "EN16931_Einfach.pdf", "client-perm-" + UUID.randomUUID() + ".pdf");

        mockMvc.perform(
                        post("/api/documents/" + documentId + "/validate")
                                .with(csrf())
                                .cookie(client.session()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void userFromAnotherOrganizationCannotValidateADocument() throws Exception {
        RegisteredUser admin = registerAndLogin("doc-validate-perm-owner");
        RegisteredUser outsider = registerAndLogin("doc-validate-perm-outsider");

        Organization organization = organizationService.createOrganization(
                "Cabinet Test " + UUID.randomUUID(), admin.user().getId());

        // outsider is registered but never added as a member of this organization -
        // creating their own, unrelated one keeps them a real authenticated user.
        organizationService.createOrganization(
                "Cabinet Outsider " + UUID.randomUUID(), outsider.user().getId());

        long documentId = uploadSample(admin.session(), organization.getId(),
                "EN16931_Einfach.pdf", "outsider-perm-" + UUID.randomUUID() + ".pdf");

        mockMvc.perform(
                        post("/api/documents/" + documentId + "/validate")
                                .with(csrf())
                                .cookie(outsider.session()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }
}
