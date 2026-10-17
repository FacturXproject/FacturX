package com.facturx.app.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.facturx.app.validation.MustangValidationClient;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

/**
 * F13 : echantillon de reference produit par le generateur Python
 * (extractor/app/generation.py). Le build echoue si le moteur Java ne le declare
 * plus conforme (XML CII, regles EN 16931, PDF/A-3).
 */
class GeneratedSampleConformanceTest {

    private byte[] sample(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/facturx-samples/" + name)) {
            if (in == null) {
                throw new IOException("Sample not found on classpath: " + name);
            }
            return in.readAllBytes();
        }
    }

    @Test
    void goldenGeneratedSampleIsConformant() throws IOException {
        MustangValidationClient.MustangReport report = new MustangValidationClient()
                .validate(sample("generated-facturx-en16931.pdf"), "generated-facturx-en16931.pdf");

        assertThat(report.completelyValid())
                .as("rapport Mustang :%n%s", report.rawXmlReport())
                .isTrue();
    }

    @Test
    void sampleWithoutSellerVatNumberIsRejected() throws IOException {
        MustangValidationClient.MustangReport report = new MustangValidationClient()
                .validate(sample("generated-facturx-invalid.pdf"), "generated-facturx-invalid.pdf");

        assertThat(report.completelyValid()).isFalse();
        assertThat(report.rawXmlReport()).contains("BR-S-02");
    }
}
