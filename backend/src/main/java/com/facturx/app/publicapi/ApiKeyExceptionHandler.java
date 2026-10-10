package com.facturx.app.publicapi;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiKeyExceptionHandler {

    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "API_KEY_NOT_FOUND",
                "message", "Cette clé API n'existe pas."
        ));
    }
}
