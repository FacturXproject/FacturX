package com.facturx.app.extraction;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record DraftInvoiceResponse(
        Long id,
        Long documentId,
        String source,
        OffsetDateTime createdAt,
        Map<String, FieldResponse> fields,
        List<LineResponse> lines
) {
    public record FieldResponse(String value, Double confidence) {
    }

    public record LineResponse(
            String description,
            String quantity,
            String unitPrice,
            String total,
            Double confidence
    ) {
    }

    public static DraftInvoiceResponse from(
            DraftInvoice draft,
            List<ExtractedField> fields,
            List<DraftLine> lines) {

        Map<String, FieldResponse> fieldMap = fields.stream()
                .collect(Collectors.toMap(
                        ExtractedField::getFieldName,
                        f -> new FieldResponse(f.getValue(), f.getConfidence())));

        List<LineResponse> lineList = lines.stream()
                .map(l -> new LineResponse(
                        l.getDescription(), l.getQuantity(), l.getUnitPrice(), l.getTotal(), l.getConfidence()))
                .toList();

        return new DraftInvoiceResponse(
                draft.getId(), draft.getDocumentId(), draft.getSource(), draft.getCreatedAt(),
                fieldMap, lineList);
    }
}
