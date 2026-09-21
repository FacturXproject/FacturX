package com.facturx.app.permission;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PermissionExceptionHandler {

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Map<String, String>> handleAccessDenied() {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
			Map.of(
				"error", "ACCESS_DENIED",
				"message", "You do not have permission to perform this action."
			)
		);
	}
}
