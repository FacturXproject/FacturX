package com.facturx.app.document;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class DocumentExceptionHandler {

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "DOCUMENT_NOT_FOUND",
                "message", "Ce document n'existe pas."
        ));
    }

    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<Map<String, String>> handleTooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of(
                "error", "FILE_TOO_LARGE",
                "message", "Le fichier dépasse la taille maximale autorisée (10 Mo)."
        ));
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<Map<String, String>> handleInvalidType() {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Map.of(
                "error", "INVALID_FILE_TYPE",
                "message", "Seuls les fichiers PDF et XML sont acceptés."
        ));
    }
}