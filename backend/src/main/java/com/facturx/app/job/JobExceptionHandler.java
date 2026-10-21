package com.facturx.app.job;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.facturx.app.job")
public class JobExceptionHandler {

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, String>> handleJobConflict(
			IllegalStateException exception) {

		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
				"error", "JOB_CONFLICT",
				"message", "Un traitement est déjà en cours pour ce document."
		));
	}
}
