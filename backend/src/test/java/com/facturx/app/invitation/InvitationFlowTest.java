package com.facturx.app.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class InvitationFlowTest extends AbstractIntegrationTest {

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

    /*
     * CI must not depend on a running Mailpit container.
     * We test that the invitation flow requests an email, while the real
     * SMTP/Mailpit path is covered by the Docker integration/manual test.
     */
    @MockitoBean
    private EmailService emailService;

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

        Cookie session = result.getResponse().getCookie("EFACTURE_SESSION");
        if (session == null) {
            throw new IllegalStateException(
                "EFACTURE_SESSION cookie was not created"
            );
        }

        User user = userRepository.findByEmail(email).orElseThrow();
        return new RegisteredUser(user, session);
    }

    private Organization createOrganization(RegisteredUser admin) {
        return organizationService.createOrganization(
            "Organization-" + UUID.randomUUID(),
            admin.user().getId()
        );
    }

    private Invitation createInvitation(
            RegisteredUser admin,
            Organization organization,
            String invitedEmail,
            Role role) throws Exception {

        mockMvc.perform(
                post("/api/organizations/{orgId}/invitations",
                    organization.getId())
                    .with(csrf())
                    .cookie(admin.session())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "email": "%s",
                            "role": "%s"
                        }
                        """.formatted(invitedEmail, role.name()))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(invitedEmail))
            .andExpect(jsonPath("$.role").value(role.name()))
            .andExpect(jsonPath("$.status").value("PENDING"));

        return invitationRepository
            .findByEmailAndOrganizationIdAndStatus(
                invitedEmail,
                organization.getId(),
                InvitationStatus.PENDING
            )
            .orElseThrow();
    }

    @Test
    void adminCanCreateInvitationAndEmailIsRequested() throws Exception {
        RegisteredUser admin = register("invite-create-admin");
        Organization organization = createOrganization(admin);
        String invitedEmail = uniqueEmail("invite-create-target");

        Invitation invitation = createInvitation(
            admin,
            organization,
            invitedEmail,
            Role.ACCOUNTANT
        );

        assertThat(invitation.getToken()).isNotBlank();
        assertThat(invitation.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);

        verify(emailService).sendInvitation(
            eq(invitedEmail),
            eq(organization.getName()),
            eq(invitation.getToken())
        );
    }

    @Test
    void duplicatePendingInvitationIsRejected() throws Exception {
        RegisteredUser admin = register("invite-duplicate-admin");
        Organization organization = createOrganization(admin);
        String invitedEmail = uniqueEmail("invite-duplicate-target");

        createInvitation(
            admin,
            organization,
            invitedEmail,
            Role.CLIENT
        );

        mockMvc.perform(
                post("/api/organizations/{orgId}/invitations",
                    organization.getId())
                    .with(csrf())
                    .cookie(admin.session())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "email": "%s",
                            "role": "CLIENT"
                        }
                        """.formatted(invitedEmail))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error")
                .value("INVITATION_ALREADY_PENDING"));
    }

    @Test
    void validInvitationCanBeCheckedWithoutAuthentication() throws Exception {
        RegisteredUser admin = register("invite-check-admin");
        Organization organization = createOrganization(admin);
        String invitedEmail = uniqueEmail("invite-check-target");

        Invitation invitation = createInvitation(
            admin,
            organization,
            invitedEmail,
            Role.ACCOUNTANT
        );

        mockMvc.perform(
                get("/api/invitations/{token}", invitation.getToken())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(invitedEmail))
            .andExpect(jsonPath("$.role").value("ACCOUNTANT"))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void invitedUserCanAcceptAndBecomesMemberWithInvitedRole() throws Exception {
        RegisteredUser admin = register("invite-accept-admin");
        RegisteredUser invitedUser = register("invite-accept-user");
        Organization organization = createOrganization(admin);

        Invitation invitation = createInvitation(
            admin,
            organization,
            invitedUser.user().getEmail(),
            Role.ACCOUNTANT
        );

        mockMvc.perform(
                post("/api/invitations/accept")
                    .with(csrf())
                    .cookie(invitedUser.session())
                    .param("token", invitation.getToken())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACCEPTED"));

        Invitation accepted = invitationRepository
            .findById(invitation.getId())
            .orElseThrow();

        assertThat(accepted.getStatus())
            .isEqualTo(InvitationStatus.ACCEPTED);

        OrganizationMember membership = memberRepository
            .findByUserIdAndOrganizationId(
                invitedUser.user().getId(),
                organization.getId()
            )
            .orElseThrow();

        assertThat(membership.getRole()).isEqualTo(Role.ACCOUNTANT);
    }

    @Test
    void anotherLoggedInUserCannotAcceptSomeoneElsesInvitation()
            throws Exception {

        RegisteredUser admin = register("invite-wrong-user-admin");
        RegisteredUser invitedUser = register("invite-right-user");
        RegisteredUser wrongUser = register("invite-wrong-user");
        Organization organization = createOrganization(admin);

        Invitation invitation = createInvitation(
            admin,
            organization,
            invitedUser.user().getEmail(),
            Role.CLIENT
        );

        mockMvc.perform(
                post("/api/invitations/accept")
                    .with(csrf())
                    .cookie(wrongUser.session())
                    .param("token", invitation.getToken())
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error")
                .value("INVITATION_NOT_FOR_CURRENT_USER"));

        Invitation stillPending = invitationRepository
            .findById(invitation.getId())
            .orElseThrow();

        assertThat(stillPending.getStatus())
            .isEqualTo(InvitationStatus.PENDING);

        assertThat(memberRepository.findByUserIdAndOrganizationId(
            wrongUser.user().getId(),
            organization.getId()
        )).isEmpty();
    }

    @Test
    void adminCanRevokeAndResendInvitationWithANewToken()
            throws Exception {

        RegisteredUser admin = register("invite-resend-admin");
        Organization organization = createOrganization(admin);
        String invitedEmail = uniqueEmail("invite-resend-target");

        Invitation invitation = createInvitation(
            admin,
            organization,
            invitedEmail,
            Role.CLIENT
        );

        String oldToken = invitation.getToken();

        mockMvc.perform(
                patch(
                    "/api/organizations/{orgId}/invitations/{invitationId}/revoke",
                    organization.getId(),
                    invitation.getId()
                )
                    .with(csrf())
                    .cookie(admin.session())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REVOKED"));

        mockMvc.perform(
                patch(
                    "/api/organizations/{orgId}/invitations/{invitationId}/resend",
                    organization.getId(),
                    invitation.getId()
                )
                    .with(csrf())
                    .cookie(admin.session())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"));

        Invitation resent = invitationRepository
            .findById(invitation.getId())
            .orElseThrow();

        assertThat(resent.getToken())
            .isNotBlank()
            .isNotEqualTo(oldToken);

        assertThat(resent.getExpiresAt()).isAfter(LocalDateTime.now());

        verify(emailService, times(2)).sendInvitation(
            eq(invitedEmail),
            eq(organization.getName()),
            org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void expiredInvitationReturnsGoneAndIsMarkedExpired()
            throws Exception {

        RegisteredUser admin = register("invite-expired-admin");
        Organization organization = createOrganization(admin);
        String invitedEmail = uniqueEmail("invite-expired-target");

        Invitation invitation = createInvitation(
            admin,
            organization,
            invitedEmail,
            Role.CLIENT
        );

        invitation.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        invitationRepository.save(invitation);

        mockMvc.perform(
                get("/api/invitations/{token}", invitation.getToken())
            )
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.error").value("INVITATION_EXPIRED"));

        Invitation expired = invitationRepository
            .findById(invitation.getId())
            .orElseThrow();

        assertThat(expired.getStatus())
            .isEqualTo(InvitationStatus.EXPIRED);
    }

    @Test
    void acceptedInvitationCannotBeAcceptedTwice() throws Exception {
        RegisteredUser admin = register("invite-twice-admin");
        RegisteredUser invitedUser = register("invite-twice-user");
        Organization organization = createOrganization(admin);

        Invitation invitation = createInvitation(
            admin,
            organization,
            invitedUser.user().getEmail(),
            Role.CLIENT
        );

        mockMvc.perform(
                post("/api/invitations/accept")
                    .with(csrf())
                    .cookie(invitedUser.session())
                    .param("token", invitation.getToken())
            )
            .andExpect(status().isOk());

        mockMvc.perform(
                post("/api/invitations/accept")
                    .with(csrf())
                    .cookie(invitedUser.session())
                    .param("token", invitation.getToken())
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error")
                .value("INVITATION_NOT_PENDING"));
    }

    private record RegisteredUser(
        User user,
        Cookie session
    ) {}
}
