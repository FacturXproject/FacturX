package com.facturx.app.generation;

import java.util.List;

/**
 * F13 : le brouillon ne peut pas encore etre transforme en Factur-X (jamais verifie
 * par un humain, invalide, ou incomplet pour la norme). Messages affichables tels quels.
 */
public class GenerationPreconditionException extends RuntimeException {

    private final String code;
    private final List<String> errors;

    public GenerationPreconditionException(String code, List<String> errors) {
        super(String.join(" ", errors));
        this.code = code;
        this.errors = List.copyOf(errors);
    }

    public String getCode() { return code; }
    public List<String> getErrors() { return errors; }
}
