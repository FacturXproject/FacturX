package com.facturx.app.extraction;

import java.util.List;
import java.util.Map;

public record DraftInvoiceUpdateRequest(
        Map<String, String> fields,
        List<LineUpdateRequest> lines
) {
    public record LineUpdateRequest(
            String description,
            String quantity,
            String unitPrice,
            String total
    ) {
    }
}