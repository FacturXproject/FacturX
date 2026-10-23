package com.facturx.app.extraction;

import java.util.List;
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

    @ExceptionHandler(DraftValidationException.class)
    public ResponseEntity<Map<String, Object>> handleDraftInvalid(DraftValidationException exception) {
        List<String> errors = exception.getErrors();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "DRAFT_INVALID",
                "message", exception.getMessage(),
                "errors", errors
        ));
    }

    @ExceptionHandler(DraftConflictException.class)
    public ResponseEntity<Map<String, String>> handleDraftConflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "DRAFT_CONFLICT",
                "message", "Ce brouillon a été modifié entre-temps (autre onglet ou autre utilisateur). "
                        + "Rechargez la page pour récupérer la dernière version."
        ));
    }
}
