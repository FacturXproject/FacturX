package com.facturx.app.validation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Schema shell only for now: {@code code} + {@code layer} + the raw (English/German)
 * text Mustangproject reports. F09 owns populating title_fr / description_fr /
 * correction_hint_fr for the rules that need a human-readable French explanation.
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
}
