package com.facturx.app.generation;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GenerationExceptionHandler {

    @ExceptionHandler(GenerationPreconditionException.class)
    public ResponseEntity<Map<String, Object>> handlePrecondition(GenerationPreconditionException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", exception.getCode(),
                "message", exception.getMessage(),
                "errors", exception.getErrors()
        ));
    }
}
