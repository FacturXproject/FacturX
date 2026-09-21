package com.facturx.app.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class OrganizationFlowTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository memberRepository;

    private static String uniqueEmail(String label) {
        String safeLabel = label.replaceAll("[^A-Za-z0-9]", "");

        if (safeLabel.length() > 12) {
            safeLabel = safeLabel.substring(0, 12);
        }

        String uniquePart = UUID.randomUUID()
            .toString()
            .replace("-", "");

        return safeLabel + "-" + uniquePart + "@x.fr";
    }

    private static String uniqueOrganizationName(String label) {
        return label + "-" + UUID.randomUUID();
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

        User user = userRepository.findByEmail(email)
            .orElseThrow();

        return new RegisteredUser(user, session);
    }

    private Organization createOrganizationThroughApi(
            RegisteredUser owner,
            String name) throws Exception {

        mockMvc.perform(
                post("/api/organizations")
                    .with(csrf())
                    .cookie(owner.session())
                    .param("name", name)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value(name))
            .andExpect(jsonPath("$.createdAt").exists());

        return memberRepository.findByUserId(owner.user().getId())
            .stream()
            .map(OrganizationMember::getOrganization)
            .filter(organization -> name.equals(organization.getName()))
            .findFirst()
            .orElseThrow();
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
    void authenticatedUserCanCreateOrganizationAndBecomesAdmin()
            throws Exception {

        RegisteredUser creator = register("organization-creator");
        String name = uniqueOrganizationName("Cabinet-Alpha");

        Organization organization =
            createOrganizationThroughApi(creator, name);

        assertThat(organizationRepository.findById(organization.getId()))
            .isPresent();

        List<OrganizationMember> memberships =
            memberRepository.findByUserId(creator.user().getId());

        assertThat(memberships).hasSize(1);

        OrganizationMember membership = memberships.get(0);

        assertThat(membership.getOrganization().getId())
            .isEqualTo(organization.getId());
        assertThat(membership.getRole())
            .isEqualTo(Role.ADMIN);
    }

    @Test
    void userWithNoOrganizationGetsAnEmptyList() throws Exception {
        RegisteredUser user = register("organization-empty");

        mockMvc.perform(
                get("/api/organizations")
                    .cookie(user.session())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void userCanListOnlyOwnOrganizations() throws Exception {
        RegisteredUser firstUser = register("organization-list-first");
        RegisteredUser secondUser = register("organization-list-second");

        Organization firstOrganization =
            createOrganizationThroughApi(
                firstUser,
                uniqueOrganizationName("First")
            );

        Organization secondOrganization =
            createOrganizationThroughApi(
                firstUser,
                uniqueOrganizationName("Second")
            );

        createOrganizationThroughApi(
            secondUser,
            uniqueOrganizationName("Other-user")
        );

        mockMvc.perform(
                get("/api/organizations")
                    .cookie(firstUser.session())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath(
                "$[*].organizationId",
                containsInAnyOrder(
                    firstOrganization.getId().intValue(),
                    secondOrganization.getId().intValue()
                )
            ));
    }

    @Test
    void organizationMemberCanReadOrganizationAndMembers()
            throws Exception {

        RegisteredUser admin = register("organization-read-admin");
        RegisteredUser accountant =
            register("organization-read-accountant");

        String name = uniqueOrganizationName("Readable-Organization");
        Organization organization =
            createOrganizationThroughApi(admin, name);

        addMember(
            organization,
            accountant,
            Role.ACCOUNTANT
        );

        mockMvc.perform(
                get("/api/organizations/{id}", organization.getId())
                    .cookie(accountant.session())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(organization.getId()))
            .andExpect(jsonPath("$.name").value(name));

        mockMvc.perform(
                get(
                    "/api/organizations/{id}/members",
                    organization.getId()
                )
                    .cookie(accountant.session())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath(
                "$[*].email",
                hasItems(
                    admin.user().getEmail(),
                    accountant.user().getEmail()
                )
            ));
    }

    @Test
    void nonMemberCannotReadOrganizationOrMembers()
            throws Exception {

        RegisteredUser admin = register("organization-private-admin");
        RegisteredUser outsider =
            register("organization-private-outsider");

        Organization organization =
            createOrganizationThroughApi(
                admin,
                uniqueOrganizationName("Private-Organization")
            );

        mockMvc.perform(
                get("/api/organizations/{id}", organization.getId())
                    .cookie(outsider.session())
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("MEMBER_NOT_FOUND"));

        mockMvc.perform(
                get(
                    "/api/organizations/{id}/members",
                    organization.getId()
                )
                    .cookie(outsider.session())
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void adminCanRenameOrganization() throws Exception {
        RegisteredUser admin = register("organization-rename-admin");

        Organization organization =
            createOrganizationThroughApi(
                admin,
                uniqueOrganizationName("Before-Rename")
            );

        String newName =
            uniqueOrganizationName("After-Rename");

        mockMvc.perform(
                put("/api/organizations/{id}", organization.getId())
                    .with(csrf())
                    .cookie(admin.session())
                    .param("name", newName)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(organization.getId()))
            .andExpect(jsonPath("$.name").value(newName));

        Organization updated =
            organizationRepository.findById(organization.getId())
                .orElseThrow();

        assertThat(updated.getName()).isEqualTo(newName);
    }

    @Test
    void adminCanDeleteOrganizationAndItsMemberships()
            throws Exception {

        RegisteredUser admin = register("organization-delete-admin");

        Organization organization =
            createOrganizationThroughApi(
                admin,
                uniqueOrganizationName("Delete-Organization")
            );

        Long organizationId = organization.getId();

        assertThat(memberRepository.findByOrganizationId(organizationId))
            .hasSize(1);

        mockMvc.perform(
                delete("/api/organizations/{id}", organizationId)
                    .with(csrf())
                    .cookie(admin.session())
            )
            .andExpect(status().isOk());

        assertThat(organizationRepository.existsById(organizationId))
            .isFalse();

        assertThat(memberRepository.findByOrganizationId(organizationId))
            .isEmpty();
    }

    private record RegisteredUser(
        User user,
        Cookie session
    ) {}
}
