package com.facturx.app.validation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * {@code code} + {@code layer} + the raw (English/German) text Mustangproject
 * reports, plus the F09 columns: {@code title_fr} / {@code description_fr} /
 * {@code correction_hint_fr}. This is data, not logic - the French explanation for a
 * rule lives here, seeded from {@code data.sql}, not hardcoded in Java. A code with no
 * row here simply has no curated explanation yet; {@link ValidationReportService}
 * falls back to Mustang's raw message rather than showing a bare rule code.
 */
@Entity
@Table(name = "rule_catalog")
public class RuleCatalog {

    @Id
    @Column(length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ValidationLayer layer;

    @Column(name = "raw_text", length = 2000)
    private String rawText;

    @Column(name = "title_fr", length = 500)
    private String titleFr;

    @Column(name = "description_fr", columnDefinition = "TEXT")
    private String descriptionFr;

    @Column(name = "correction_hint_fr", columnDefinition = "TEXT")
    private String correctionHintFr;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ValidationLayer getLayer() {
        return layer;
    }

    public void setLayer(ValidationLayer layer) {
        this.layer = layer;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getTitleFr() {
        return titleFr;
    }

    public void setTitleFr(String titleFr) {
        this.titleFr = titleFr;
    }

    public String getDescriptionFr() {
        return descriptionFr;
    }

    public void setDescriptionFr(String descriptionFr) {
        this.descriptionFr = descriptionFr;
    }

    public String getCorrectionHintFr() {
        return correctionHintFr;
    }

    public void setCorrectionHintFr(String correctionHintFr) {
        this.correctionHintFr = correctionHintFr;
    }
}
