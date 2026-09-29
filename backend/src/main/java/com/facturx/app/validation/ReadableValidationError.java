package com.facturx.app.validation;

/**
 * One validation error enriched with its French explanation, if the rule has one in
 * {@link RuleCatalog}. This is what F09 adds on top of F08's raw {@link ValidationError}:
 * a non-expert reads {@code titleFr}/{@code descriptionFr}/{@code correctionHintFr},
 * a developer debugging Mustang's output still has {@code rawMessage}.
 */
public record ReadableValidationError(
        ValidationLayer layer,
        ValidationSeverity severity,
        String ruleCode,
        String rawMessage,
        String field,
        String actualValue,
        String expectedValue,
        String titleFr,
        String descriptionFr,
        String correctionHintFr
) {
}
