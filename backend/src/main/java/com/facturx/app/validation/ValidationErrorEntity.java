package com.facturx.app.validation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "validation_errors")
public class ValidationErrorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private ValidationRun run;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ValidationLayer layer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ValidationSeverity severity;

    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    // message/field/actualValue are Mustang/Schematron-generated free text (XPath
    // locations get longer with deeper/repeated invoice line structures) - no fixed
    // cap is safe. Confirmed the hard way: a real invalid invoice produced a
    // location string past the original varchar(500) and broke the insert.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String field;

    @Column(name = "actual_value", columnDefinition = "TEXT")
    private String actualValue;

    @Column(name = "expected_value", columnDefinition = "TEXT")
    private String expectedValue;

    public Long getId() {
        return id;
    }

    public ValidationRun getRun() {
        return run;
    }

    public void setRun(ValidationRun run) {
        this.run = run;
    }

    public ValidationLayer getLayer() {
        return layer;
    }

    public void setLayer(ValidationLayer layer) {
        this.layer = layer;
    }

    public ValidationSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ValidationSeverity severity) {
        this.severity = severity;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getActualValue() {
        return actualValue;
    }

    public void setActualValue(String actualValue) {
        this.actualValue = actualValue;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }
}
