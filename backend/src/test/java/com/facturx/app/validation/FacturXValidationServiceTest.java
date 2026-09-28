package com.facturx.app.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.facturx.app.AbstractIntegrationTest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FacturXValidationServiceTest extends AbstractIntegrationTest {

    @Autowired
    private FacturXValidationService validationService;

    @Autowired
    private ValidationRunRepository validationRunRepository;

    @Autowired
    private ValidationErrorRepository validationErrorRepository;

    @Test
    void validSampleIsValidAndPersistsItsNotices() throws IOException {
        byte[] sample = readSample("EN16931_Einfach.pdf");

        ValidationResult result = validationService.validate(sample, "EN16931_Einfach.pdf", null);

        assertThat(result.valid()).isTrue();
        assertThat(result.layerReached()).isEqualTo(ValidationLayer.SCHEMATRON);
        // Still compliant overall, but Mustang reports non-blocking XRechnung-profile
        // notices even on a valid EN16931 file (confirmed in the day-1/day-2 smoke test).
        assertThat(result.errors()).isNotEmpty();
        assertThat(result.errors()).allMatch(e -> e.severity() == ValidationSeverity.INFO);
        assertThat(result.errors()).anyMatch(e -> e.ruleCode().equals("PEPPOL-EN16931-R001"));

        List<ValidationRun> runs = validationRunRepository.findAll();
        ValidationRun persistedRun = runs.stream()
                .filter(r -> "EN16931_Einfach.pdf".equals(r.getFilename()))
                .findFirst()
                .orElseThrow();
        assertThat(persistedRun.isValid()).isTrue();
        assertThat(persistedRun.getDocumentId()).isNull();
        assertThat(validationErrorRepository.findByRunId(persistedRun.getId()))
                .hasSize(result.errors().size());
    }

    @Test
    void invalidSampleFailsAtPdfA3LayerAndStopsThere() throws IOException {
        byte[] sample = readSample("veraPDFtestsuite6-7-11-t01-fail-a.pdf");

        ValidationResult result = validationService.validate(
                sample, "veraPDFtestsuite6-7-11-t01-fail-a.pdf", null);

        assertThat(result.valid()).isFalse();
        assertThat(result.layerReached()).isEqualTo(ValidationLayer.PDF_A3);
        assertThat(result.errors()).allMatch(e -> e.layer() == ValidationLayer.PDF_A3);
        assertThat(result.errors()).anyMatch(e -> e.message().equals("Not a PDF/A-3"));

        List<ValidationRun> runs = validationRunRepository.findAll();
        ValidationRun persistedRun = runs.stream()
                .filter(r -> "veraPDFtestsuite6-7-11-t01-fail-a.pdf".equals(r.getFilename()))
                .findFirst()
                .orElseThrow();
        assertThat(persistedRun.isValid()).isFalse();
        assertThat(persistedRun.getLayerReached()).isEqualTo(ValidationLayer.PDF_A3);
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
