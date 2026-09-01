package com.facturx.app.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Couvre les scenarios principaux de la feature Organisations (F02).
 */
@AutoConfigureMockMvc
class OrganizationFlowTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static String uniqueEmail(String label) {
        return label + "-" + UUID.randomUUID() + "@x.fr";
    }

    private static String registerBody(String email, String password) {
        return """
                {"email":"%s","password":"%s","firstName":"Jean","lastName":"Dupont"}""".formatted(email, password);
    }

    private static Cookie sessionCookie(MvcResult result) {
        Cookie sessionCookie = result.getResponse().getCookie("EFACTURE_SESSION");
        assertThat(sessionCookie).as("EFACTURE_SESSION cookie on response").isNotNull();
        return sessionCookie;
    }

    // Un compte de test = son email + son cookie de session, les deux servent ensuite
    private record TestAccount(String email, Cookie session) {
    }

    private TestAccount registerAndLogin(String label) throws Exception {
        String email = uniqueEmail(label);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "correcthorsebattery")))
                .andExpect(status().isCreated())
                .andReturn();
        return new TestAccount(email, sessionCookie(result));
    }

    private long userIdFor(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return user.getId();
    }

    private long extractId(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    void creatingAnOrganizationMakesTheCreatorAdmin() throws Exception {
        TestAccount creator = registerAndLogin("org-create");

        MvcResult created = mockMvc.perform(post("/api/organizations?name=Cabinet Dupont")
                        .with(csrf())
                        .cookie(creator.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cabinet Dupont"))
                .andReturn();
        long orgId = extractId(created);

        mockMvc.perform(get("/api/organizations/" + orgId + "/members").cookie(creator.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(creator.email()))
                .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }

    @Test
    void creatingAnOrganizationWithoutASessionIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/organizations?name=Cabinet Sans Auth").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void gettingMembersOfAnUnknownOrganizationReturnsNotFound() throws Exception {
        TestAccount account = registerAndLogin("org-404");

        mockMvc.perform(get("/api/organizations/999999/members").cookie(account.session()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ORGANIZATION_NOT_FOUND"));
    }

    @Test
    void addingTheSameMemberTwiceIsRejected() throws Exception {
        TestAccount owner = registerAndLogin("org-owner");
        TestAccount member = registerAndLogin("org-member");

        MvcResult created = mockMvc.perform(post("/api/organizations?name=Cabinet Doublon")
                        .with(csrf())
                        .cookie(owner.session()))
                .andExpect(status().isOk())
                .andReturn();
        long orgId = extractId(created);
        long memberUserId = userIdFor(member.email());

        mockMvc.perform(post("/api/organizations/" + orgId + "/members"
                        + "?userId=" + memberUserId + "&role=CLIENT")
                        .with(csrf())
                        .cookie(owner.session()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/organizations/" + orgId + "/members"
                        + "?userId=" + memberUserId + "&role=CLIENT")
                        .with(csrf())
                        .cookie(owner.session()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("USER_ALREADY_MEMBER"));
    }

    @Test
    void deletingAnOrganizationAlsoRemovesItsMembers() throws Exception {
        TestAccount owner = registerAndLogin("org-delete");

        MvcResult created = mockMvc.perform(post("/api/organizations?name=Cabinet A Supprimer")
                        .with(csrf())
                        .cookie(owner.session()))
                .andExpect(status().isOk())
                .andReturn();
        long orgId = extractId(created);

        mockMvc.perform(delete("/api/organizations/" + orgId)
                        .with(csrf())
                        .cookie(owner.session()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/organizations/" + orgId + "/members").cookie(owner.session()))
                .andExpect(status().isNotFound());
    }
}
