package com.facturx.app.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * F12 : regles de validation et de calcul du formulaire de correction (F12.txt),
 * sans base ni contexte Spring.
 */
class DraftCorrectionTest {

    private static Map<String, String> validFields() {
        Map<String, String> fields = new HashMap<>();
        fields.put("invoiceNumber", "FACT-2026-001");
        fields.put("invoiceDate", "15/03/2026");
        fields.put("sellerName", "ACME SAS");
        fields.put("buyerName", "Client SARL");
        fields.put("vatRate", "20");
        return fields;
    }

    private static DraftInvoiceUpdateRequest.LineUpdateRequest line(String qty, String price) {
        return new DraftInvoiceUpdateRequest.LineUpdateRequest("Prestation", qty, price, null);
    }

    private static DraftCorrection.Result validate(Map<String, String> fields,
                                                   DraftInvoiceUpdateRequest.LineUpdateRequest... lines) {
        return DraftCorrection.validate(new DraftInvoiceUpdateRequest(fields, List.of(lines), null));
    }

    private static List<String> errorsOf(Map<String, String> fields,
                                         DraftInvoiceUpdateRequest.LineUpdateRequest... lines) {
        try {
            validate(fields, lines);
        } catch (DraftValidationException exception) {
            return exception.getErrors();
        }
        throw new AssertionError("validation should have failed");
    }

    @Test
    void computesLineTotalsSubtotalVatAndTotal() {
        DraftCorrection.Result result = validate(validFields(), line("3", "33.335"), line("2", "10"));

        assertThat(result.lines()).extracting(DraftCorrection.Line::total)
                .containsExactly(new BigDecimal("100.01"), new BigDecimal("20.00"));
        assertThat(result.totalHt()).isEqualByComparingTo("120.01");
        assertThat(result.totalVat()).isEqualByComparingTo("24.00");
        assertThat(result.totalTtc()).isEqualByComparingTo("144.01");
    }

    @Test
    void subtotalIsTheSumOfRoundedLineTotals() {
        // 3 x 0.005 : chaque ligne vaut 0.01 une fois arrondie, le sous-total doit valoir 0.03.
        DraftCorrection.Result result = validate(validFields(),
                line("1", "0.005"), line("1", "0.005"), line("1", "0.005"));

        assertThat(result.totalHt()).isEqualByComparingTo("0.03");
    }

    @Test
    void groupsVatByRateWithLineRateOverridingTheInvoiceRate() {
        DraftCorrection.Result result = validate(validFields(),
                line("1", "100"),
                new DraftInvoiceUpdateRequest.LineUpdateRequest("Livre", "2", "10", "5,5"),
                new DraftInvoiceUpdateRequest.LineUpdateRequest("Autre", "1", "50", "20.00"));

        assertThat(result.taxes()).hasSize(2);
        assertThat(result.taxes().get(0).rate()).isEqualByComparingTo("20");
        assertThat(result.taxes().get(0).taxableAmount()).isEqualByComparingTo("150.00");
        assertThat(result.taxes().get(0).taxAmount()).isEqualByComparingTo("30.00");
        assertThat(result.taxes().get(1).rate()).isEqualByComparingTo("5.5");
        assertThat(result.taxes().get(1).taxableAmount()).isEqualByComparingTo("20.00");
        assertThat(result.taxes().get(1).taxAmount()).isEqualByComparingTo("1.10");
        assertThat(result.totalVat()).isEqualByComparingTo("31.10");
        assertThat(result.totalTtc()).isEqualByComparingTo("201.10");
    }

    @Test
    void reportsEveryMissingRequiredFieldAtOnce() {
        List<String> errors = errorsOf(new HashMap<>(Map.of("sellerSiren", "123456789")));

        assertThat(errors).contains(
                "Le numéro de facture est obligatoire.",
                "La date de facture est obligatoire.",
                "Le nom du vendeur est obligatoire.",
                "Le nom de l'acheteur est obligatoire.",
                "Le taux de TVA est obligatoire.",
                "La facture doit contenir au moins une ligne.");
    }

    @Test
    void blankRequiredFieldIsRefused() {
        Map<String, String> fields = validFields();
        fields.put("buyerName", "   ");

        assertThat(errorsOf(fields, line("1", "1"))).containsExactly("Le nom de l'acheteur est obligatoire.");
    }

    @Test
    void rejectsInvalidLineValues() {
        List<String> errors = errorsOf(validFields(),
                new DraftInvoiceUpdateRequest.LineUpdateRequest(" ", "0", "-1", "150"),
                new DraftInvoiceUpdateRequest.LineUpdateRequest("ok", "abc", "", null));

        assertThat(errors).containsExactly(
                "Ligne 1 : la description est obligatoire.",
                "Ligne 1 : la quantité doit être un nombre strictement positif.",
                "Ligne 1 : le prix unitaire doit être un nombre positif ou nul.",
                "Ligne 1 : le taux de TVA doit être un nombre compris entre 0 et 100.",
                "Ligne 2 : la quantité doit être un nombre strictement positif.",
                "Ligne 2 : le prix unitaire doit être un nombre positif ou nul.");
    }

    @Test
    void rejectsVatRateOutOfRange() {
        Map<String, String> fields = validFields();
        fields.put("vatRate", "100.01");

        assertThat(errorsOf(fields, line("1", "1")))
                .containsExactly("Le taux de TVA doit être un nombre compris entre 0 et 100.");
    }

    @Test
    void acceptsBoundaryValues() {
        Map<String, String> fields = validFields();
        fields.put("vatRate", "100");

        DraftCorrection.Result result = validate(fields, line("0.001", "0"));

        assertThat(result.totalTtc()).isEqualByComparingTo("0");
        assertThat(result.fields().get("vatRate")).isEqualTo("100");
    }

    @Test
    @Timeout(2)
    void rejectsScientificNotationAndOversizedNumbersQuickly() {
        assertThat(DraftCorrection.parseDecimal("1E+100000000")).isNull();
        assertThat(DraftCorrection.parseDecimal("1e3")).isNull();
        assertThat(DraftCorrection.parseDecimal("1".repeat(13))).isNull();
        assertThat(DraftCorrection.parseDecimal("1.1234567")).isNull();
        assertThat(DraftCorrection.parseDecimal("NaN")).isNull();
    }

    @Test
    void parsesFrenchAndEnglishNumberFormats() {
        assertThat(DraftCorrection.parseDecimal("1 080,50")).isEqualByComparingTo("1080.50");
        assertThat(DraftCorrection.parseDecimal("1 080,50")).isEqualByComparingTo("1080.50");
        assertThat(DraftCorrection.parseDecimal("1.080,50")).isEqualByComparingTo("1080.50");
        assertThat(DraftCorrection.parseDecimal("1,080.50")).isEqualByComparingTo("1080.50");
        assertThat(DraftCorrection.parseDecimal("12,5")).isEqualByComparingTo("12.5");
        assertThat(DraftCorrection.parseRate("5,5 %")).isEqualByComparingTo("5.5");
    }

    @Test
    void validatesDates() {
        assertThat(DraftCorrection.parseDate("15/03/2026")).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(DraftCorrection.parseDate("15.03.2026")).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(DraftCorrection.parseDate("2026-03-15")).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(DraftCorrection.parseDate("31/02/2026")).isNull();
        assertThat(DraftCorrection.parseDate("demain")).isNull();

        Map<String, String> fields = validFields();
        fields.put("invoiceDate", "not a date");
        assertThat(errorsOf(fields, line("1", "1")))
                .containsExactly("La date de facture doit être une date valide au format JJ/MM/AAAA.");

        fields = validFields();
        fields.put("dueDate", "14/03/2026");
        assertThat(errorsOf(fields, line("1", "1")))
                .containsExactly("La date d'échéance ne peut pas précéder la date de facture.");
    }

    @Test
    void refusesUnknownFieldsButIgnoresClientTotals() {
        Map<String, String> fields = validFields();
        fields.put("totalHt", "999999");
        fields.put("totalTtc", "1");

        DraftCorrection.Result result = validate(fields, line("1", "10"));
        assertThat(result.fields()).doesNotContainKeys("totalHt", "totalTtc");
        assertThat(result.totalTtc()).isEqualByComparingTo("12.00");

        fields.put("evilField", "x");
        assertThat(errorsOf(fields, line("1", "10"))).containsExactly("Champ inconnu : evilField.");
    }

    @Test
    void rejectsOverlongTextAndNormalizesWhitespace() {
        Map<String, String> fields = validFields();
        fields.put("sellerAddress", "a".repeat(501));
        List<String> errors = errorsOf(fields,
                new DraftInvoiceUpdateRequest.LineUpdateRequest("d".repeat(501), "1", "1", null));
        assertThat(errors).containsExactly(
                "Le champ sellerAddress ne doit pas dépasser 500 caractères.",
                "Ligne 1 : la description ne doit pas dépasser 500 caractères.");

        fields = validFields();
        fields.put("sellerName", "  ACME\n  SAS ");
        fields.put("dueDate", "");
        DraftCorrection.Result result = validate(fields,
                new DraftInvoiceUpdateRequest.LineUpdateRequest(" Conseil\nexpert ", "1", "1", " "));
        assertThat(result.fields().get("sellerName")).isEqualTo("ACME SAS");
        assertThat(result.fields().get("dueDate")).isNull();
        assertThat(result.lines().get(0).description()).isEqualTo("Conseil expert");
        assertThat(result.lines().get(0).vatRate()).isNull();
    }

    @Test
    void rejectsAbsurdTotals() {
        assertThatThrownBy(() -> validate(validFields(), line("999999999999", "999999999999")))
                .isInstanceOf(DraftValidationException.class)
                .hasMessage("Le montant total de la facture est trop élevé.");
    }

    @Test
    void rejectsMissingBody() {
        assertThatThrownBy(() -> DraftCorrection.validate(null))
                .isInstanceOf(DraftValidationException.class);
        assertThatThrownBy(() -> DraftCorrection.validate(new DraftInvoiceUpdateRequest(validFields(), null, null)))
                .isInstanceOf(DraftValidationException.class);
    }
}
