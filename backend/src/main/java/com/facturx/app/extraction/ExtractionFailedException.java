package com.facturx.app.extraction;

public class ExtractionFailedException extends RuntimeException {
    public ExtractionFailedException(String message) {
        super(message);
    }

    public ExtractionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
