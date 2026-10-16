package com.facturx.app.extraction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F12 : regles de validation et de calcul du formulaire de correction (voir F12.txt).
 * Classe pure, sans acces base : le service l'appelle avant toute ecriture, et le
 * frontend applique les memes regles (frontend/src/utils/invoiceDraft.js) pour
 * afficher les erreurs avant l'envoi. En cas de divergence, c'est le serveur qui fait foi.
 */
final class DraftCorrection {

    /** Champs modifiables par l'utilisateur (liste blanche : tout autre nom est refuse). */
    static final Set<String> EDITABLE_FIELDS = Set.of(
            "invoiceNumber", "invoiceDate", "dueDate",
            "sellerName", "sellerSiren", "sellerVat", "sellerAddress",
            "buyerName", "buyerSiren", "buyerAddress",
            "vatRate");

    /** Totaux toujours recalcules : ignores s'ils sont envoyes par le client. */
    static final Set<String> CALCULATED_FIELDS = Set.of("totalHt", "totalVat", "totalTtc");

    private static final Map<String, String> REQUIRED_FIELDS = orderedMap(
            "invoiceNumber", "Le numéro de facture est obligatoire.",
            "invoiceDate", "La date de facture est obligatoire.",
            "sellerName", "Le nom du vendeur est obligatoire.",
            "buyerName", "Le nom de l'acheteur est obligatoire.",
            "vatRate", "Le taux de TVA est obligatoire.");

    static final int MAX_LINES = 500;
    static final int MAX_TEXT_LENGTH = 500;

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    // Garde-fou : borne les montants bien en dessous de la precision des colonnes
    // draft_taxes (numeric(19,2)).
    private static final BigDecimal MAX_TOTAL = new BigDecimal("1000000000000000");

    // Apres normalisation : pas de notation scientifique ("1E+100000000" ferait
    // construire un nombre de cent millions de chiffres), 12 chiffres avant la
    // virgule et 6 apres au maximum.
    private static final Pattern DECIMAL = Pattern.compile("-?\\d{1,12}(\\.\\d{1,6})?");
    private static final Pattern SPACES = Pattern.compile("[\\s\\u00A0\\u202F]+");

    private static final Pattern FRENCH_DATE = Pattern.compile("(\\d{2})[/.\\-](\\d{2})[/.\\-](\\d{4})");
    private static final Pattern ISO_DATE = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");

    record Line(String description, BigDecimal quantity, BigDecimal unitPrice,
                BigDecimal vatRate, BigDecimal total) {
    }

    record Tax(BigDecimal rate, BigDecimal taxableAmount, BigDecimal taxAmount) {
    }

    /**
     * @param fields champs modifiables normalises (trim ; null si vide), dans l'ordre de la requete
     */
    record Result(Map<String, String> fields, List<Line> lines, List<Tax> taxes,
                  BigDecimal totalHt, BigDecimal totalVat, BigDecimal totalTtc) {
    }

    private DraftCorrection() {
    }

    /**
     * Valide la requete et calcule lignes, ventilation de TVA et totaux.
     *
     * @throws DraftValidationException avec la liste complete des erreurs (pas seulement la premiere)
     */
    static Result validate(DraftInvoiceUpdateRequest request) {
        if (request == null || request.fields() == null || request.lines() == null) {
            throw new DraftValidationException(List.of("Les champs et les lignes de la facture sont obligatoires."));
        }

        List<String> errors = new ArrayList<>();
        Map<String, String> fields = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : request.fields().entrySet()) {
            String name = entry.getKey();
            if (CALCULATED_FIELDS.contains(name)) {
                continue;
            }
            if (!EDITABLE_FIELDS.contains(name)) {
                errors.add("Champ inconnu : " + name + ".");
                continue;
            }
            String value = normalizeText(entry.getValue());
            if (value != null && value.length() > MAX_TEXT_LENGTH) {
                errors.add("Le champ " + name + " ne doit pas dépasser " + MAX_TEXT_LENGTH + " caractères.");
            }
            fields.put(name, value);
        }

        REQUIRED_FIELDS.forEach((name, message) -> {
            if (fields.get(name) == null) {
                errors.add(message);
            }
        });

        LocalDate invoiceDate = checkDate(fields.get("invoiceDate"), "La date de facture", errors);
        LocalDate dueDate = checkDate(fields.get("dueDate"), "La date d'échéance", errors);
        if (invoiceDate != null && dueDate != null && dueDate.isBefore(invoiceDate)) {
            errors.add("La date d'échéance ne peut pas précéder la date de facture.");
        }

        BigDecimal defaultRate = null;
        if (fields.get("vatRate") != null) {
            defaultRate = parseRate(fields.get("vatRate"));
            if (defaultRate == null) {
                errors.add("Le taux de TVA doit être un nombre compris entre 0 et 100.");
            } else {
                fields.put("vatRate", plain(defaultRate));
            }
        }

        List<Line> lines = new ArrayList<>();
        if (request.lines().isEmpty()) {
            errors.add("La facture doit contenir au moins une ligne.");
        } else if (request.lines().size() > MAX_LINES) {
            errors.add("Une facture ne peut pas dépasser " + MAX_LINES + " lignes.");
        } else {
            for (int i = 0; i < request.lines().size(); i++) {
                Line line = checkLine(request.lines().get(i), i + 1, errors);
                if (line != null) {
                    lines.add(line);
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new DraftValidationException(errors);
        }

        // Ventilation par taux (taux de la ligne, sinon taux de la facture), du plus
        // eleve au plus bas. TVA d'un taux = base HT du taux x taux / 100, arrondie.
        Map<BigDecimal, BigDecimal> baseByRate = new TreeMap<>(Comparator.reverseOrder());
        BigDecimal totalHt = BigDecimal.ZERO.setScale(2);
        for (Line line : lines) {
            BigDecimal rate = line.vatRate() != null ? line.vatRate() : defaultRate;
            baseByRate.merge(rate, line.total(), BigDecimal::add);
            totalHt = totalHt.add(line.total());
        }

        List<Tax> taxes = new ArrayList<>();
        BigDecimal totalVat = BigDecimal.ZERO.setScale(2);
        for (Map.Entry<BigDecimal, BigDecimal> entry : baseByRate.entrySet()) {
            BigDecimal tax = entry.getValue()
                    .multiply(entry.getKey())
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            taxes.add(new Tax(entry.getKey(), entry.getValue(), tax));
            totalVat = totalVat.add(tax);
        }

        BigDecimal totalTtc = totalHt.add(totalVat);
        if (totalTtc.compareTo(MAX_TOTAL) >= 0) {
            throw new DraftValidationException(List.of("Le montant total de la facture est trop élevé."));
        }

        return new Result(fields, lines, taxes, totalHt, totalVat, totalTtc);
    }

    private static Line checkLine(DraftInvoiceUpdateRequest.LineUpdateRequest input, int number,
                                  List<String> errors) {
        String prefix = "Ligne " + number + " : ";
        if (input == null) {
            errors.add(prefix + "ligne vide.");
            return null;
        }

        int errorsBefore = errors.size();

        String description = normalizeText(input.description());
        if (description == null) {
            errors.add(prefix + "la description est obligatoire.");
        } else if (description.length() > MAX_TEXT_LENGTH) {
            errors.add(prefix + "la description ne doit pas dépasser " + MAX_TEXT_LENGTH + " caractères.");
        }

        BigDecimal quantity = parseDecimal(input.quantity());
        if (quantity == null || quantity.signum() <= 0) {
            errors.add(prefix + "la quantité doit être un nombre strictement positif.");
        }

        BigDecimal unitPrice = parseDecimal(input.unitPrice());
        if (unitPrice == null || unitPrice.signum() < 0) {
            errors.add(prefix + "le prix unitaire doit être un nombre positif ou nul.");
        }

        BigDecimal vatRate = null;
        if (normalizeText(input.vatRate()) != null) {
            vatRate = parseRate(input.vatRate());
            if (vatRate == null) {
                errors.add(prefix + "le taux de TVA doit être un nombre compris entre 0 et 100.");
            }
        }

        if (errors.size() > errorsBefore) {
            return null;
        }

        BigDecimal total = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        return new Line(description, quantity, unitPrice, vatRate, total);
    }

    /**
     * Nombre au format francais ou anglais : "1 080,50", "1.080,50", "1,080.50",
     * "1080.5". Le dernier separateur est la decimale. Retourne null si la valeur
     * est vide ou invalide.
     */
    static BigDecimal parseDecimal(String value) {
        if (value == null) {
            return null;
        }
        String normalized = SPACES.matcher(value).replaceAll("");
        int comma = normalized.lastIndexOf(',');
        int dot = normalized.lastIndexOf('.');
        if (comma >= 0 && dot >= 0) {
            normalized = comma > dot
                    ? normalized.replace(".", "").replace(',', '.')
                    : normalized.replace(",", "");
        } else if (comma >= 0) {
            normalized = normalized.replace(',', '.');
        }

        if (!DECIMAL.matcher(normalized).matches()) {
            return null;
        }
        return new BigDecimal(normalized);
    }

    /** Taux de TVA entre 0 et 100 inclus, "%" final tolere ("5,5 %"). Null si invalide. */
    static BigDecimal parseRate(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.endsWith("%")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        BigDecimal rate = parseDecimal(trimmed);
        if (rate == null || rate.signum() < 0 || rate.compareTo(HUNDRED) > 0) {
            return null;
        }
        return rate;
    }

    /** "20.50" -> "20.5", "20.00" -> "20", "0.0" -> "0". */
    static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    /** Trim + espaces multiples (dont sauts de ligne) reduits a un seul ; null si vide. */
    static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = SPACES.matcher(value).replaceAll(" ").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    // Formats acceptes : ceux produits par l'extracteur (JJ/MM/AAAA, JJ.MM.AAAA,
    // JJ-MM-AAAA, voir extractor/app/extraction.py) et l'ISO AAAA-MM-JJ.
    private static LocalDate checkDate(String value, String label, List<String> errors) {
        if (value == null) {
            return null;
        }
        LocalDate date = parseDate(value);
        if (date == null) {
            errors.add(label + " doit être une date valide au format JJ/MM/AAAA.");
        }
        return date;
    }

    static LocalDate parseDate(String value) {
        try {
            Matcher french = FRENCH_DATE.matcher(value);
            if (french.matches()) {
                return LocalDate.of(
                        Integer.parseInt(french.group(3)),
                        Integer.parseInt(french.group(2)),
                        Integer.parseInt(french.group(1)));
            }
            Matcher iso = ISO_DATE.matcher(value);
            if (iso.matches()) {
                return LocalDate.of(
                        Integer.parseInt(iso.group(1)),
                        Integer.parseInt(iso.group(2)),
                        Integer.parseInt(iso.group(3)));
            }
        } catch (DateTimeException exception) {
            return null;
        }
        return null;
    }

    private static Map<String, String> orderedMap(String... keysAndValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }
}
