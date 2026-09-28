package com.facturx.app.validation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class ValidationReportControllerTest extends AbstractIntegrationTest {

    private static final Pattern RUN_ID_PATTERN = Pattern.compile("\"runId\":(\\d+)");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/validate/runs/1/report"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownRunReturns404() throws Exception {
        Cookie session = loginSession();

        mockMvc.perform(get("/api/validate/runs/999999999/report").cookie(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void reportIsReachableRightAfterValidating() throws Exception {
        Cookie session = loginSession();
        MockMultipartFile file = sampleFile("EN16931_Einfach.pdf");

        MvcResult validateResult = mockMvc.perform(multipart("/api/validate").file(file).with(csrf()).cookie(session))
                .andExpect(status().isOk())
                .andReturn();
        Matcher matcher = RUN_ID_PATTERN.matcher(validateResult.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new IllegalStateException("No runId in validate response: " + validateResult.getResponse().getContentAsString());
        }
        String runId = matcher.group(1);

        mockMvc.perform(get("/api/validate/runs/" + runId + "/report").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.errorCount").value(0))
                .andExpect(jsonPath("$.errors[0].titleFr").isNotEmpty());
    }

    private Cookie loginSession() throws Exception {
        String email = "report-" + UUID.randomUUID() + "@x.fr";
        MvcResult result = mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"correcthorsebattery","firstName":"Jean","lastName":"Dupont"}"""
                                .formatted(email)))
                .andReturn();
        Cookie session = result.getResponse().getCookie("EFACTURE_SESSION");
        if (session == null) {
            throw new IllegalStateException("No session cookie after register");
        }
        return session;
    }

    private MockMultipartFile sampleFile(String filename) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/facturx-samples/" + filename)) {
            if (in == null) {
                throw new IOException("Sample not found on classpath: " + filename);
            }
            return new MockMultipartFile("file", filename, "application/pdf", in.readAllBytes());
        }
    }
}
