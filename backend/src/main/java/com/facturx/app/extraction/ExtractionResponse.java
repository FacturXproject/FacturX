package com.facturx.app.extraction;

import java.util.List;
import java.util.Map;

/**
 * Shape returned by the Python extractor's {@code POST /extract} (see
 * extractor/app/schemas.py). Kept separate from {@link DraftInvoiceResponse},
 * which is what this backend exposes to the frontend once persisted.
 */
public record ExtractionResponse(
        Map<String, FieldValue> fields,
        List<LineValue> lines,
        String source
) {
    public record FieldValue(String value, Double confidence) {
    }

    public record LineValue(
            String description,
            String quantity,
            String unitPrice,
            String total,
            Double confidence
    ) {
    }
}
