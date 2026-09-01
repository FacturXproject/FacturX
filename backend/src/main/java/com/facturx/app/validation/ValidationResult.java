package com.facturx.app.validation;

import java.util.List;

public record ValidationResult(
        boolean valid,
        ValidationLayer layerReached,
        List<ValidationError> errors
) {
}
