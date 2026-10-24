package com.facturx.app.gdpr;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.facturx.app.gdpr")
public class GdprExceptionHandler {

	@ExceptionHandler(InvalidGdprConfirmationException.class)
	public ResponseEntity<Map<String, String>> handleInvalidConfirmation() {

		return ResponseEntity.badRequest().body(Map.of(
				"error", "INVALID_CONFIRMATION",
				"message", "La confirmation de suppression est invalide."
		));
	}

	@ExceptionHandler(GdprUserNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleUserNotFound() {

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
				"error", "USER_NOT_FOUND",
				"message", "Utilisateur introuvable."
		));
	}
}
