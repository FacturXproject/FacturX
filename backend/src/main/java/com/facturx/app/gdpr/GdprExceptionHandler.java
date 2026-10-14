package com.facturx.app.gdpr;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GdprExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(
			IllegalArgumentException exception) {

		return ResponseEntity
				.badRequest()
				.body(Map.of(
						"error",
						exception.getMessage()
				));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, String>> handleConflict(
			IllegalStateException exception) {

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(Map.of(
						"error",
						exception.getMessage()
				));
	}
}
