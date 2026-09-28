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

    @Autowired
    private ValidationErrorRepository validationErrorRepository;

    @Test
    void peppolNoticeIsHiddenFromTheReportButKeptInTheDatabase() throws IOException {
        ValidationResult result = validationService.validate(
                readSample("EN16931_Einfach.pdf"), "EN16931_Einfach.pdf", null);

        ValidationReport report = validationReportService.getReport(result.runId());
        assertThat(report.errors()).noneMatch(e -> e.ruleCode().startsWith("PEPPOL"));

        assertThat(validationErrorRepository.findByRunId(result.runId()))
                .anyMatch(e -> e.getRuleCode().equals("PEPPOL-EN16931-R001"));
    }

    @Test
    void validSampleReportHasNoErrorsAndHidesThePeppolNotice() throws IOException {
        ValidationResult result = validationService.validate(
                readSample("EN16931_Einfach.pdf"), "EN16931_Einfach.pdf", null);

        ValidationReport report = validationReportService.getReport(result.runId());

        assertThat(report.valid()).isTrue();
        assertThat(report.errorCount()).isZero();
        // The sample's only info-level message is a PEPPOL-EN16931-R001 notice (see
        // FacturXValidationServiceTest) - PEPPOL/BR-DE noise is filtered out of the F09
        // report entirely (kept in validation_errors, just not surfaced here).
        assertThat(report.infoCount()).isZero();
        assertThat(report.errors()).noneMatch(e -> e.ruleCode().startsWith("PEPPOL"));
        assertThat(report.errors()).noneMatch(e -> e.ruleCode().startsWith("BR-DE"));
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

        ReadableValidationError noXmlFailure = report.errors().stream()
                .filter(e -> e.ruleCode().equals("MUSTANG-EXCEPTION-17"))
                .findFirst()
                .orElseThrow();
        assertThat(noXmlFailure.titleFr()).isEqualTo("Ce PDF ne contient pas de facture Factur-X");
        assertThat(noXmlFailure.correctionHintFr()).contains("Convertir");
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
