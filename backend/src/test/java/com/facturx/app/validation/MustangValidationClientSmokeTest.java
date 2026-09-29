package com.facturx.app.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

/**
 * Not a real spec test yet — runs the wrapper against one real Factur-X sample and
 * prints the raw report so the exact schema Mustangproject returns can be confirmed
 * before writing the day-2 mapping into {@link ValidationError}.
 */
class MustangValidationClientSmokeTest {

    @Test
    void validatesRealSampleAndPrintsRawReport() throws IOException {
        byte[] sample = readSample("EN16931_Einfach.pdf");

        MustangValidationClient.MustangReport report =
                new MustangValidationClient().validate(sample, "EN16931_Einfach.pdf");

        System.out.println("=== Mustang raw report (EN16931_Einfach.pdf) ===");
        System.out.println(report.rawXmlReport());
        System.out.println("=== completelyValid: " + report.completelyValid() + " ===");

        assertThat(report.rawXmlReport()).isNotBlank();
    }

    @Test
    void validatesKnownInvalidSampleAndPrintsRawReport() throws IOException {
        byte[] sample = readSample("veraPDFtestsuite6-7-11-t01-fail-a.pdf");

        MustangValidationClient.MustangReport report =
                new MustangValidationClient().validate(sample, "veraPDFtestsuite6-7-11-t01-fail-a.pdf");

        System.out.println("=== Mustang raw report (veraPDFtestsuite6-7-11-t01-fail-a.pdf) ===");
        System.out.println(report.rawXmlReport());
        System.out.println("=== completelyValid: " + report.completelyValid() + " ===");
    }

    private byte[] readSample(String filename) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/facturx-samples/" + filename)) {
            if (in == null) {
                throw new IOException("Sample not found on classpath: " + filename);
            }
            return in.readAllBytes();
        }
    }
}
