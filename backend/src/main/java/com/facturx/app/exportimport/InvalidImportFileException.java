package com.facturx.app.exportimport;

public class InvalidImportFileException extends RuntimeException {

    public InvalidImportFileException(String message) {
        super(message);
    }

    public InvalidImportFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
