package com.facturx.app.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import com.facturx.app.user.User;
import com.facturx.app.user.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Covers the exact scenarios listed in F01_AUTH_BRIEF.md #9.
 */
@AutoConfigureMockMvc
class AuthFlowTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static String uniqueEmail(String label) {
        return label + "-" + UUID.randomUUID() + "@x.fr";
    }

    private static String registerBody(String email, String password) {
        return """
                {"email":"%s","password":"%s","firstName":"Jean","lastName":"Dupont"}""".formatted(email, password);
    }

    private static void assertNeverLeaksPassword(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("password_hash").doesNotContain("$2a$");
    }

    // Spring Session manages its own session id via the EFACTURE_SESSION cookie set on the
    // response - a pre-built MockHttpSession threaded across requests is NOT the same session.
    private static Cookie sessionCookie(MvcResult result) {
        Cookie sessionCookie = result.getResponse().getCookie("EFACTURE_SESSION");
        assertThat(sessionCookie).as("EFACTURE_SESSION cookie on response").isNotNull();
        return sessionCookie;
    }

    @Test
    void registerCreatesAnActiveUserWithAHashedPassword() throws Exception {
        String email = uniqueEmail("register");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "correcthorsebattery")))
                .andExpect(status().isCreated())
                .andReturn();
        assertNeverLeaksPassword(result);

        User saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("correcthorsebattery");
        assertThat(passwordEncoder.matches("correcthorsebattery", saved.getPasswordHash())).isTrue();
        assertThat(saved.getStatus()).isEqualTo(User.STATUS_ACTIVE);
    }

    @Test
    void registerWithExistingEmailIsRejected() throws Exception {
        String email = uniqueEmail("dup");
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "correcthorsebattery")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "anotherpassword")))
                .andExpect(status().isConflict());
    }

    @Test
    void registerWithExistingEmailInDifferentCaseIsRejected() throws Exception {
        String email = uniqueEmail("case");
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "correcthorsebattery")))
                .andExpect(status().isCreated());

        String shouted = email.toUpperCase();
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(shouted, "anotherpassword")))
                .andExpect(status().isConflict());

        assertThat(userRepository.findByEmail(email)).isPresent();
    }

    @Test
    void registerWithA9CharacterPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(uniqueEmail("shortpw"), "123456789")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithCorrectCredentialsSetsTheSessionCookie() throws Exception {
        String email = uniqueEmail("login-ok");
        mockMvc.perform(post("/api/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email, "correcthorsebattery")));

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correcthorsebattery"}""".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("EFACTURE_SESSION"));
    }

    @Test
    void loginWithWrongPasswordAndLoginWithUnknownEmailReturnByteIdenticalBodies() throws Exception {
        String email = uniqueEmail("wrongpw");
        mockMvc.perform(post("/api/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email, "correcthorsebattery")));

        MvcResult wrongPassword = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"wrongpassword"}""".formatted(email)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        MvcResult unknownEmail = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"wrongpassword"}""".formatted(uniqueEmail("unknown"))))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(wrongPassword.getResponse().getContentAsString())
                .isEqualTo(unknownEmail.getResponse().getContentAsString());
    }

    @Test
    void meWithoutASessionIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithASessionReturnsTheUserAndNeverThePasswordHash() throws Exception {
        String email = uniqueEmail("me");
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "correcthorsebattery")))
                .andReturn();
        Cookie session = sessionCookie(registerResult);

        MvcResult result = mockMvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(email)))
                .andReturn();
        assertNeverLeaksPassword(result);
    }

    @Test
    void logoutInvalidatesTheSessionSoMeBecomesUnauthorized() throws Exception {
        String email = uniqueEmail("logout");
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, "correcthorsebattery")))
                .andReturn();
        Cookie session = sessionCookie(registerResult);

        mockMvc.perform(post("/api/auth/logout").with(csrf()).cookie(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sixFailedLoginsInARowAreThrottled() throws Exception {
        String email = uniqueEmail("throttle");
        mockMvc.perform(post("/api/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email, "correcthorsebattery")));

        String wrongLoginBody = """
                {"email":"%s","password":"wrongpassword"}""".formatted(email);

        for (int attempt = 1; attempt <= 5; attempt++) {
            mockMvc.perform(post("/api/auth/login").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(wrongLoginBody))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongLoginBody))
                .andExpect(status().isTooManyRequests());
    }
}
