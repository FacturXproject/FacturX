package com.facturx.app.permission;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.facturx.app.AbstractIntegrationTest;
import com.facturx.app.organization.Invitation;
import com.facturx.app.organization.InvitationRepository;
import com.facturx.app.organization.InvitationStatus;
import com.facturx.app.organization.Organization;
import com.facturx.app.organization.OrganizationMember;
import com.facturx.app.organization.OrganizationMemberRepository;
import com.facturx.app.organization.OrganizationService;
import com.facturx.app.organization.Role;
import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;

import jakarta.servlet.http.Cookie;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class PermissionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private OrganizationMemberRepository memberRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    private static String uniqueEmail(String label) {
        return label + "-" + UUID.randomUUID() + "@x.fr";
    }

    private static String registerBody(String email) {
        return """
            {
                "email": "%s",
                "password": "correcthorsebattery",
                "firstName": "Test",
                "lastName": "User"
            }
            """.formatted(email);
    }

    private RegisteredUser register(String label) throws Exception {
        String email = uniqueEmail(label);

        MvcResult result = mockMvc.perform(
                post("/api/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerBody(email))
            )
            .andExpect(status().isCreated())
            .andReturn();

        Cookie session =
            result.getResponse().getCookie("EFACTURE_SESSION");

        if (session == null) {
            throw new IllegalStateException(
                "EFACTURE_SESSION cookie was not created"
            );
        }

        User user = userRepository.findByEmail(email)
            .orElseThrow();

        return new RegisteredUser(user, session);
    }

    private Organization createOrganization(RegisteredUser admin) {
        return organizationService.createOrganization(
            "Organization-" + UUID.randomUUID(),
            admin.user().getId()
        );
    }

    private OrganizationMember addMember(
            Organization organization,
            RegisteredUser registeredUser,
            Role role) {

        OrganizationMember member = new OrganizationMember();
        member.setOrganization(organization);
        member.setUser(registeredUser.user());
        member.setRole(role);

        return memberRepository.save(member);
    }

    @Test
    void adminCanManageInvitations() throws Exception {
        RegisteredUser admin = register("admin-invitations");

        Organization organization = createOrganization(admin);

        String invitedEmail = uniqueEmail("invited");

        mockMvc.perform(
                post("/api/organizations/{orgId}/invitations",
                    organization.getId())
                    .with(csrf())
                    .cookie(admin.session())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "email": "%s",
                            "role": "ACCOUNTANT"
                        }
                        """.formatted(invitedEmail))
            )
            .andExpect(status().isCreated());

        mockMvc.perform(
                get("/api/organizations/{orgId}/invitations",
                    organization.getId())
                    .cookie(admin.session())
            )
            .andExpect(status().isOk());

        Invitation invitation = invitationRepository
            .findByEmailAndOrganizationIdAndStatus(
                invitedEmail,
                organization.getId(),
                InvitationStatus.PENDING
            )
            .orElseThrow();

        mockMvc.perform(
                patch(
                    "/api/organizations/{orgId}/invitations/{invitationId}/revoke",
                    organization.getId(),
                    invitation.getId()
                )
                    .with(csrf())
                    .cookie(admin.session())
            )
            .andExpect(status().isOk());
    }

    @Test
    void accountantAndClientCannotManageInvitations() throws Exception {
        RegisteredUser admin = register("admin-invite-denied");
        RegisteredUser accountant = register("accountant-invite-denied");
        RegisteredUser client = register("client-invite-denied");

        Organization organization = createOrganization(admin);

        addMember(
            organization,
            accountant,
            Role.ACCOUNTANT
        );

        addMember(
            organization,
            client,
            Role.CLIENT
        );

        String invitationBody = """
            {
                "email": "%s",
                "role": "CLIENT"
            }
            """.formatted(uniqueEmail("target"));

        mockMvc.perform(
                post("/api/organizations/{orgId}/invitations",
                    organization.getId())
                    .with(csrf())
                    .cookie(accountant.session())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invitationBody)
            )
            .andExpect(status().isForbidden());

        mockMvc.perform(
                post("/api/organizations/{orgId}/invitations",
                    organization.getId())
                    .with(csrf())
                    .cookie(client.session())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invitationBody)
            )
            .andExpect(status().isForbidden());

        mockMvc.perform(
                get("/api/organizations/{orgId}/invitations",
                    organization.getId())
                    .cookie(accountant.session())
            )
            .andExpect(status().isForbidden());

        mockMvc.perform(
                get("/api/organizations/{orgId}/invitations",
                    organization.getId())
                    .cookie(client.session())
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void onlyAdminCanManageMembers() throws Exception {
        RegisteredUser admin = register("admin-members");
        RegisteredUser accountant = register("accountant-members");
        RegisteredUser client = register("client-members");

        Organization organization = createOrganization(admin);

        OrganizationMember accountantMember =
            addMember(
                organization,
                accountant,
                Role.ACCOUNTANT
            );

        OrganizationMember clientMember =
            addMember(
                organization,
                client,
                Role.CLIENT
            );

        mockMvc.perform(
                patch(
                    "/api/organizations/{orgId}/members/{userId}/role",
                    organization.getId(),
                    client.user().getId()
                )
                    .with(csrf())
                    .cookie(accountant.session())
                    .param("role", "ACCOUNTANT")
            )
            .andExpect(status().isForbidden());

        mockMvc.perform(
                delete(
                    "/api/organizations/{orgId}/members/{userId}",
                    organization.getId(),
                    client.user().getId()
                )
                    .with(csrf())
                    .cookie(accountant.session())
            )
            .andExpect(status().isForbidden());

        mockMvc.perform(
                patch(
                    "/api/organizations/{orgId}/members/{userId}/role",
                    organization.getId(),
                    client.user().getId()
                )
                    .with(csrf())
                    .cookie(client.session())
                    .param("role", "ACCOUNTANT")
            )
            .andExpect(status().isForbidden());

        mockMvc.perform(
                patch(
                    "/api/organizations/{orgId}/members/{userId}/role",
                    organization.getId(),
                    client.user().getId()
                )
                    .with(csrf())
                    .cookie(admin.session())
                    .param("role", "ACCOUNTANT")
            )
            .andExpect(status().isOk());

        mockMvc.perform(
                delete(
                    "/api/organizations/{orgId}/members/{userId}",
                    organization.getId(),
                    accountant.user().getId()
                )
                    .with(csrf())
                    .cookie(admin.session())
            )
            .andExpect(status().isOk());
    }

    @Test
    void onlyAdminCanManageOrganization() throws Exception {
        RegisteredUser admin = register("admin-org");
        RegisteredUser accountant = register("accountant-org");
        RegisteredUser client = register("client-org");

        Organization organization = createOrganization(admin);

        addMember(
            organization,
            accountant,
            Role.ACCOUNTANT
        );

        addMember(
            organization,
            client,
            Role.CLIENT
        );

        mockMvc.perform(
                put("/api/organizations/{id}",
                    organization.getId())
                    .with(csrf())
                    .cookie(accountant.session())
                    .param("name", "Forbidden name")
            )
            .andExpect(status().isForbidden());

        mockMvc.perform(
                put("/api/organizations/{id}",
                    organization.getId())
                    .with(csrf())
                    .cookie(client.session())
                    .param("name", "Forbidden name")
            )
            .andExpect(status().isForbidden());

        mockMvc.perform(
                delete("/api/organizations/{id}",
                    organization.getId())
                    .with(csrf())
                    .cookie(accountant.session())
            )
            .andExpect(status().isForbidden());

        mockMvc.perform(
                delete("/api/organizations/{id}",
                    organization.getId())
                    .with(csrf())
                    .cookie(client.session())
            )
            .andExpect(status().isForbidden());

        mockMvc.perform(
                put("/api/organizations/{id}",
                    organization.getId())
                    .with(csrf())
                    .cookie(admin.session())
                    .param("name", "Updated Organization")
            )
            .andExpect(status().isOk());

        mockMvc.perform(
                delete("/api/organizations/{id}",
                    organization.getId())
                    .with(csrf())
                    .cookie(admin.session())
            )
            .andExpect(status().isOk());
    }

    @Test
    void memberCanListMembersButNonMemberCannot() throws Exception {
        RegisteredUser admin = register("admin-list");
        RegisteredUser accountant = register("accountant-list");
        RegisteredUser outsider = register("outsider-list");

        Organization organization = createOrganization(admin);

        addMember(
            organization,
            accountant,
            Role.ACCOUNTANT
        );

        mockMvc.perform(
                get("/api/organizations/{id}/members",
                    organization.getId())
                    .cookie(admin.session())
            )
            .andExpect(status().isOk());

        mockMvc.perform(
                get("/api/organizations/{id}/members",
                    organization.getId())
                    .cookie(accountant.session())
            )
            .andExpect(status().isOk());

        mockMvc.perform(
                get("/api/organizations/{id}/members",
                    organization.getId())
                    .cookie(outsider.session())
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void memberCanReadOrganizationButNonMemberCannot() throws Exception {
        RegisteredUser admin = register("admin-get-org");
        RegisteredUser accountant = register("accountant-get-org");
        RegisteredUser outsider = register("outsider-get-org");

        Organization organization = createOrganization(admin);

        addMember(
            organization,
            accountant,
            Role.ACCOUNTANT
        );

        mockMvc.perform(
                get("/api/organizations/{id}", organization.getId())
                    .cookie(admin.session())
            )
            .andExpect(status().isOk());

        mockMvc.perform(
                get("/api/organizations/{id}", organization.getId())
                    .cookie(accountant.session())
            )
            .andExpect(status().isOk());

        mockMvc.perform(
                get("/api/organizations/{id}", organization.getId())
                    .cookie(outsider.session())
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void cannotDemoteLastAdmin() throws Exception {
        RegisteredUser admin = register("last-admin-demote");

        Organization organization = createOrganization(admin);

        mockMvc.perform(
                patch(
                    "/api/organizations/{orgId}/members/{userId}/role",
                    organization.getId(),
                    admin.user().getId()
                )
                    .with(csrf())
                    .cookie(admin.session())
                    .param("role", "ACCOUNTANT")
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("LAST_ADMIN_REQUIRED"));
    }

    @Test
    void cannotRemoveLastAdmin() throws Exception {
        RegisteredUser admin = register("last-admin-remove");

        Organization organization = createOrganization(admin);

        mockMvc.perform(
                delete(
                    "/api/organizations/{orgId}/members/{userId}",
                    organization.getId(),
                    admin.user().getId()
                )
                    .with(csrf())
                    .cookie(admin.session())
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("LAST_ADMIN_REQUIRED"));
    }

    private record RegisteredUser(
        User user,
        Cookie session
    ) {}
}
