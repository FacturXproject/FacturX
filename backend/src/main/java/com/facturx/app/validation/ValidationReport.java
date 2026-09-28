package com.facturx.app.validation;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * The readable report for a persisted {@link ValidationRun}: a summary a non-expert
 * can act on (valid/invalid, how many errors vs. warnings vs. informational notices)
 * plus the enriched error list. Reachable either by run id right after validating
 * ({@code GET /api/validate/runs/{runId}/report}) or, once a document exists (F07),
 * by document id for its latest run ({@code GET /api/documents/{id}/report}) - see
 * {@link ValidationReportController} and {@link com.facturx.app.document.DocumentController}.
 */
public record ValidationReport(
        Long runId,
        String filename,
        boolean valid,
        ValidationLayer layerReached,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        long errorCount,
        long warningCount,
        long infoCount,
        List<ReadableValidationError> errors
) {
}
