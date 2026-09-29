package com.facturx.app.validation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * F09: the readable report for a validation run, reachable right after validating via
 * {@code POST /api/validate} or {@code POST /api/documents/{id}/validate} (F08). The
 * document-scoped {@code GET /api/documents/{id}/report} in
 * {@link com.facturx.app.document.DocumentController} is the one to use once a
 * document exists - this route stays for the raw, document-less {@code /api/validate}
 * flow.
 */
@RestController
@RequestMapping("/api/validate/runs")
public class ValidationReportController {

    private final ValidationReportService validationReportService;

    public ValidationReportController(ValidationReportService validationReportService) {
        this.validationReportService = validationReportService;
    }

    @GetMapping("/{runId}/report")
    public ValidationReport report(@PathVariable Long runId) {
        return validationReportService.getReport(runId);
    }
}
