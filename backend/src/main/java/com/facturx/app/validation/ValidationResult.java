package com.facturx.app.validation;

import java.util.List;

/**
 * {@code runId} is null right after {@link MustangReportParser} parses the raw report -
 * there's no persisted {@link ValidationRun} yet at that point. {@link FacturXValidationService}
 * fills it in once the run is saved, so a client can fetch
 * {@code GET /api/validate/runs/{runId}/report} (F09) right after validating.
 */
public record ValidationResult(
        boolean valid,
        ValidationLayer layerReached,
        List<ValidationError> errors,
        Long runId
) {
}
