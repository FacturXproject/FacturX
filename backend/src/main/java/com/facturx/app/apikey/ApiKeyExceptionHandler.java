package com.facturx.app.apikey;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.facturx.app.apikey")
public class ApiKeyExceptionHandler {

    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "API_KEY_NOT_FOUND",
                "message", "Cette clé API n'existe pas."
        ));
    }

    // Scope inconnu ou null dans le tableau "scopes", JSON malforme : Jackson refuse le
    // corps avant la validation. Sans ce handler la reponse serait un 400 sans corps,
    // hors du format d'erreur de l'API.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableBody() {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "VALIDATION_FAILED",
                "message", "Corps de requête invalide : vérifiez le JSON et les scopes "
                        + "(DOCUMENTS_READ, DOCUMENTS_WRITE, DOCUMENTS_VALIDATE)."
        ));
    }
}
