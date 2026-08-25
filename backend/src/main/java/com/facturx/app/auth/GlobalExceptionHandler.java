package com.facturx.app.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(Map.of(
                "error", "VALIDATION_FAILED",
                "fields", fields
        ));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<Map<String, String>> handleEmailTaken() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "EMAIL_TAKEN",
                "message", "Cette adresse email est déjà utilisée."
        ));
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<Map<String, String>> handleTooManyAttempts() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "error", "TOO_MANY_ATTEMPTS",
                "message", "Trop de tentatives. Réessayez dans 15 minutes."
        ));
    }

    // Covers BadCredentialsException, DisabledException, etc. On purpose the body
    // is byte-identical whether the email is unknown or the password is wrong -
    // see F01 brief §4, don't let a reviewer (or an attacker) tell them apart.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationFailure() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "INVALID_CREDENTIALS",
                "message", "Identifiants invalides."
        ));
    }
}
