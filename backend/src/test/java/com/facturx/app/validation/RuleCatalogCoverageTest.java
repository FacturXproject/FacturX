package com.facturx.app.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.facturx.app.AbstractIntegrationTest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Regression guard for the F09 "never a bare, unexplained rule code" goal: every code
 * these "phase 1" fixtures actually raise (plain PDF with no embedded XML, not PDF/A-3,
 * missing invoice number, missing type code, wrong total) must have a
 * {@link RuleCatalog} row, so the F09 report never falls back to
 * {@code UNCATALOGUED_TITLE_FR} for them. BR-DE and PEPPOL notices are excluded from
 * this check on purpose - {@link ValidationReportService} hides that whole family from
 * the report regardless of whether they're catalogued (see data.sql).
 */
class RuleCatalogCoverageTest extends AbstractIntegrationTest {

    private static final String UNCATALOGUED_TITLE_FR = "Erreur de validation non répertoriée";

    @Autowired
    private FacturXValidationService validationService;

    @Autowired
    private ValidationReportService validationReportService;

    @ParameterizedTest
    @ValueSource(strings = {
            "EN16931_Einfach.pdf",
            "veraPDFtestsuite6-7-11-t01-fail-a.pdf",
            "plain-pdf-no-xml.pdf",
            "missing-invoice-number.pdf",
            "missing-type-code.pdf",
            "wrong-total-amount.pdf"
    })
    void everyShownErrorHasAFrenchExplanation(String filename) throws IOException {
        ValidationResult result = validationService.validate(readSample(filename), filename, null);
        ValidationReport report = validationReportService.getReport(result.runId());

        assertThat(report.errors()).allSatisfy(e -> {
            assertThat(e.titleFr()).isNotEqualTo(UNCATALOGUED_TITLE_FR)
                    .describedAs("rule %s on %s has no catalog entry", e.ruleCode(), filename)
                    .isNotBlank();
            assertThat(e.descriptionFr()).isNotBlank();
        });
    }

    @Test
    void missingInvoiceNumberRaisesTheExpectedCodes() throws IOException {
        ValidationResult result = validationService.validate(
                readSample("missing-invoice-number.pdf"), "missing-invoice-number.pdf", null);
        ValidationReport report = validationReportService.getReport(result.runId());

        List<String> codes = report.errors().stream().map(ReadableValidationError::ruleCode).toList();
        assertThat(codes).contains("BR-02", "FX-SCH-A-000019", "MUSTANG-ERROR-18");
    }

    @Test
    void missingTypeCodeRaisesTheExpectedCodes() throws IOException {
        ValidationResult result = validationService.validate(
                readSample("missing-type-code.pdf"), "missing-type-code.pdf", null);
        ValidationReport report = validationReportService.getReport(result.runId());

        List<String> codes = report.errors().stream().map(ReadableValidationError::ruleCode).toList();
        assertThat(codes).contains("BR-04", "FX-SCH-A-000020", "MUSTANG-ERROR-18");
    }

    @Test
    void wrongTotalAmountRaisesTheExpectedCodes() throws IOException {
        ValidationResult result = validationService.validate(
                readSample("wrong-total-amount.pdf"), "wrong-total-amount.pdf", null);
        ValidationReport report = validationReportService.getReport(result.runId());

        List<String> codes = report.errors().stream().map(ReadableValidationError::ruleCode).toList();
        assertThat(codes).contains("BR-CO-15", "BR-CO-16");
    }

    @Test
    void plainPdfWithNoXmlRaisesTheExpectedCodes() throws IOException {
        ValidationResult result = validationService.validate(
                readSample("plain-pdf-no-xml.pdf"), "plain-pdf-no-xml.pdf", null);
        ValidationReport report = validationReportService.getReport(result.runId());

        List<String> codes = report.errors().stream().map(ReadableValidationError::ruleCode).toList();
        assertThat(codes).contains("MUSTANG-ERROR-17", "MUSTANG-ERROR-23", "MUSTANG-EXCEPTION-17");
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
