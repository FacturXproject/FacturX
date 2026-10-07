package com.facturx.app.extraction;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExtractionExceptionHandler {

    @ExceptionHandler(UnsupportedDocumentTypeException.class)
    public ResponseEntity<Map<String, String>> handleUnsupportedType() {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Map.of(
                "error", "EXTRACTION_UNSUPPORTED_TYPE",
                "message", "Seuls les fichiers PDF peuvent être convertis en Factur-X."
        ));
    }

    @ExceptionHandler(ExtractionFailedException.class)
    public ResponseEntity<Map<String, String>> handleFailed() {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "error", "EXTRACTION_FAILED",
                "message", "Le service d'extraction est indisponible ou a échoué à traiter ce document."
        ));
    }

    @ExceptionHandler(DraftNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleDraftNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "DRAFT_NOT_FOUND",
                "message", "Aucune extraction n'a encore été effectuée pour ce document."
        ));
    }
}
