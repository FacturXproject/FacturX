package com.facturx.app.generation;

/** F13 : le service Python de generation est indisponible ou a refuse la demande. */
public class GenerationFailedException extends RuntimeException {
    public GenerationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
