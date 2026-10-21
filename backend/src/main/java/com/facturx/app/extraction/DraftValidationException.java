package com.facturx.app.extraction;

import java.util.List;

/** F12 : correction refusee ; porte toutes les erreurs, affichables telles quelles. */
public class DraftValidationException extends RuntimeException {

    private final List<String> errors;

    public DraftValidationException(List<String> errors) {
        super(String.join(" ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
