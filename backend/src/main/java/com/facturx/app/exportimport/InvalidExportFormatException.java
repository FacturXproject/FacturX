package com.facturx.app.exportimport;

public class InvalidExportFormatException extends RuntimeException {

    public InvalidExportFormatException(String allowedFormats) {
        super("Format d'export invalide. Formats acceptés : " + allowedFormats + ".");
    }
}
