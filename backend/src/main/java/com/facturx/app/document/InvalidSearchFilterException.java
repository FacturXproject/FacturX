package com.facturx.app.document;

/** F16: a search parameter has an unknown or out-of-range value (-> HTTP 400). */
public class InvalidSearchFilterException extends RuntimeException {

    private final String field;

    public InvalidSearchFilterException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
