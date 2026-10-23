package com.facturx.app.exportimport;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice(assignableTypes = {ExportController.class, ImportController.class})
public class ExportImportExceptionHandler {

    @ExceptionHandler(InvalidExportFormatException.class)
    public ResponseEntity<Map<String, String>> handleInvalidFormat(InvalidExportFormatException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "INVALID_EXPORT_FORMAT",
                "message", e.getMessage()
        ));
    }

    @ExceptionHandler(InvalidImportFileException.class)
    public ResponseEntity<Map<String, String>> handleInvalidFile(InvalidImportFileException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "INVALID_IMPORT_FILE",
                "message", e.getMessage()
        ));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, String>> handleMissingFile() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "INVALID_IMPORT_FILE",
                "message", "Aucun fichier envoyé."
        ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleTooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of(
                "error", "FILE_TOO_LARGE",
                "message", "Le fichier dépasse la taille maximale autorisée (10 Mo)."
        ));
    }
}
