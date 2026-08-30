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

    //ana was here
    @ExceptionHandler(InvitationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleInvitationNotFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "error", "INVITATION_NOT_FOUND",
            "message", "Cette invitation n'existe pas."
    ));
    }

    @ExceptionHandler(InvitationExpiredException.class)
    public ResponseEntity<Map<String, String>> handleInvitationExpired() {
        return ResponseEntity.status(HttpStatus.GONE).body(Map.of(
                "error", "INVITATION_EXPIRED",
                "message", "Cette invitation a expiré."
    ));
    }

    @ExceptionHandler(InvitationNotPendingException.class)
    public ResponseEntity<Map<String, String>> handleInvitationNotPending() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
            "error", "INVITATION_NOT_PENDING",
            "message", "Cette invitation a déjà été traitée."
    ));
    }

    @ExceptionHandler(NoAccountFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoAccountFound(NoAccountFoundException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
            "error", "NO_ACCOUNT_FOUND",
            "message", "Aucun compte trouvé pour cet email.",
            "redirectUrl", "/register?invitationToken=" + ex.getMessage()
    ));
    }
    @ExceptionHandler(InvitationAlreadyPendingException.class)
    public ResponseEntity<Map<String, String>> handleInvitationAlreadyPending() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
            "error", "INVITATION_ALREADY_PENDING",
            "message", "Une invitation est déjà en attente pour cet email."
    ));
}
    
}