package com.facturx.app.extraction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record DraftInvoiceResponse(
        Long id,
        Long documentId,
        String source,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Map<String, FieldResponse> fields,
        List<LineResponse> lines,
        List<TaxResponse> taxes
) {
    // source : source de l'extracteur, "manual" (corrige par l'utilisateur) ou
    // "calculated" (totaux recalcules par le serveur, F12).
    public record FieldResponse(String value, Double confidence, String source) {
    }

    public record LineResponse(
            String description,
            String quantity,
            String unitPrice,
            String vatRate,
            String total,
            Double confidence,
            String source
    ) {
    }

    public record TaxResponse(String rate, String taxableAmount, String taxAmount) {
    }

    public static DraftInvoiceResponse from(
            DraftInvoice draft,
            List<ExtractedField> fields,
            List<DraftLine> lines,
            List<DraftTax> taxes) {

        Map<String, FieldResponse> fieldMap = fields.stream()
                .collect(Collectors.toMap(
                        ExtractedField::getFieldName,
                        f -> new FieldResponse(f.getValue(), f.getConfidence(), f.getSource())));

        List<LineResponse> lineList = lines.stream()
                .map(l -> new LineResponse(
                        l.getDescription(), l.getQuantity(), l.getUnitPrice(), l.getVatRate(),
                        l.getTotal(), l.getConfidence(), l.getSource()))
                .toList();

        List<TaxResponse> taxList = taxes.stream()
                .map(t -> new TaxResponse(
                        plain(t.getRate()), plain(t.getTaxableAmount()), plain(t.getTaxAmount())))
                .toList();

        return new DraftInvoiceResponse(
                draft.getId(), draft.getDocumentId(), draft.getSource(), draft.getCreatedAt(),
                draft.getUpdatedAt(), fieldMap, lineList, taxList);
    }

    // Taux "20.0000" -> "20" ; montants gardent leurs deux decimales ("120.00").
    private static String plain(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.scale() > 2
                ? value.stripTrailingZeros().toPlainString()
                : value.toPlainString();
    }
}
