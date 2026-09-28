package com.facturx.app.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.facturx.app.AbstractIntegrationTest;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class ValidationReportServiceTest extends AbstractIntegrationTest {

    @Autowired
    private FacturXValidationService validationService;

    @Autowired
    private ValidationReportService validationReportService;

    @Test
    void validSampleReportHasNoErrorsAndAReadableNoticeForKnownRule() throws IOException {
        ValidationResult result = validationService.validate(
                readSample("EN16931_Einfach.pdf"), "EN16931_Einfach.pdf", null);

        ValidationReport report = validationReportService.getReport(result.runId());

        assertThat(report.valid()).isTrue();
        assertThat(report.errorCount()).isZero();
        // Still has non-blocking PEPPOL notices (see FacturXValidationServiceTest) -
        // "empty report" means no blocking error, not an empty error list.
        assertThat(report.infoCount()).isPositive();

        ReadableValidationError peppolNotice = report.errors().stream()
                .filter(e -> e.ruleCode().equals("PEPPOL-EN16931-R001"))
                .findFirst()
                .orElseThrow();
        assertThat(peppolNotice.titleFr()).isEqualTo("Identifiant de processus métier manquant");
        assertThat(peppolNotice.correctionHintFr()).isNotBlank();
    }

    @Test
    void invalidSampleReportExplainsThePdfA3Failure() throws IOException {
        ValidationResult result = validationService.validate(
                readSample("veraPDFtestsuite6-7-11-t01-fail-a.pdf"),
                "veraPDFtestsuite6-7-11-t01-fail-a.pdf", null);

        ValidationReport report = validationReportService.getReport(result.runId());

        assertThat(report.valid()).isFalse();
        assertThat(report.errorCount()).isPositive();
        assertThat(report.errors()).allSatisfy(e -> {
            // Never a bare, unexplained rule code (F09 mini-task) - either a curated
            // catalog row or at minimum Mustang's own message as the description.
            assertThat(e.titleFr()).isNotBlank();
            assertThat(e.descriptionFr()).isNotBlank();
        });

        ReadableValidationError pdfA3Failure = report.errors().stream()
                .filter(e -> e.rawMessage().equals("Not a PDF/A-3"))
                .findFirst()
                .orElseThrow();
        assertThat(pdfA3Failure.ruleCode()).isEqualTo("MUSTANG-ERROR-23");
        assertThat(pdfA3Failure.titleFr()).isEqualTo("Le fichier n'est pas un PDF/A-3 valide");
    }

    @Test
    void unknownRunIdReturns404() {
        assertThat(catchResponseStatusException(() -> validationReportService.getReport(-1L)).getStatusCode()
                .value()).isEqualTo(404);
    }

    private ResponseStatusException catchResponseStatusException(Runnable runnable) {
        try {
            runnable.run();
        } catch (ResponseStatusException e) {
            return e;
        }
        throw new AssertionError("Expected a ResponseStatusException");
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
