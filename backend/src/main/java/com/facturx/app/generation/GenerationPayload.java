package com.facturx.app.generation;

import java.util.List;

/**
 * F13 : corps de POST /generate du service Python (extractor/app/schemas.py,
 * GenerationRequest). Montants en texte decimal : pas de flottant entre les services.
 */
public record GenerationPayload(String profile, Invoice invoice) {

    public record Invoice(
            String number,
            String issueDate,
            String dueDate,
            String currency,
            Party seller,
            Party buyer,
            List<Line> lines,
            List<Tax> taxes,
            String totalHt,
            String totalVat,
            String totalTtc
    ) {
    }

    public record Party(String name, String siren, String vatNumber, String address) {
    }

    public record Line(String description, String quantity, String unitPrice, String vatRate, String total) {
    }

    public record Tax(String rate, String taxableAmount, String taxAmount) {
    }
}
