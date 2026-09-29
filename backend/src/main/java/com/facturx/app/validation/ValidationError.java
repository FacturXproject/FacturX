package com.facturx.app.validation;

public record ValidationError(
        ValidationLayer layer,
        ValidationSeverity severity,
        String ruleCode,
        String message,
        String field,
        String actualValue,
        String expectedValue
) {
}
