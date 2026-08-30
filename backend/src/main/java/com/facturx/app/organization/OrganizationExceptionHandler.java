package com.facturx.app.organization;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrganizationExceptionHandler {

    @ExceptionHandler(OrganizationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOrganizationNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "ORGANIZATION_NOT_FOUND",
                "message", "Cette organisation n'existe pas."
        ));
    }

    @ExceptionHandler(UserAlreadyMemberException.class)
    public ResponseEntity<Map<String, String>> handleUserAlreadyMember() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "USER_ALREADY_MEMBER",
                "message", "Cet utilisateur est déjà membre de l'organisation."
        ));
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleMemberNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "MEMBER_NOT_FOUND",
                "message", "Ce membre n'existe pas dans l'organisation."
        ));
    }
}