package com.facturx.app.validation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.facturx.app.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class ValidationControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        MockMultipartFile file = sampleFile("EN16931_Einfach.pdf");

        mockMvc.perform(multipart("/api/validate").file(file).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emptyFileIsRejected() throws Exception {
        Cookie session = loginSession();
        MockMultipartFile empty = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/validate").file(empty).with(csrf()).cookie(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validSampleReturnsValidTrue() throws Exception {
        Cookie session = loginSession();
        MockMultipartFile file = sampleFile("EN16931_Einfach.pdf");

        mockMvc.perform(multipart("/api/validate").file(file).with(csrf()).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.layerReached").value("SCHEMATRON"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void invalidSampleReturnsValidFalseWithPdfA3Errors() throws Exception {
        Cookie session = loginSession();
        MockMultipartFile file = sampleFile("veraPDFtestsuite6-7-11-t01-fail-a.pdf");

        mockMvc.perform(multipart("/api/validate").file(file).with(csrf()).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.layerReached").value("PDF_A3"))
                .andExpect(jsonPath("$.errors[0].layer").value("PDF_A3"));
    }

    private Cookie loginSession() throws Exception {
        String email = "validate-" + UUID.randomUUID() + "@x.fr";
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
