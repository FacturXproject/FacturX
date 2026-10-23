package com.facturx.app.publicapi;

import com.facturx.app.document.DocumentNotFoundException;
import com.facturx.app.document.FileTooLargeException;
import com.facturx.app.document.InvalidFileTypeException;
import com.facturx.app.job.JobConflictException;
import com.facturx.app.permission.AccessDeniedException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

/**
 * F17 : toutes les erreurs de l'API publique au meme format JSON {"error","message"}.
 *
 * Prioritaire sur les handlers globaux, et limite a PublicApiController. Il couvre aussi
 * les erreurs Spring MVC (parametre manquant, fichier trop gros...) : sans lui, elles
 * partiraient vers /error, qui est derriere la chaine session et repondrait 401 a un
 * client authentifie par cle.
 */
@RestControllerAdvice(assignableTypes = PublicApiController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublicApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PublicApiExceptionHandler.class);

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound() {
        return error(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Ce document n'existe pas.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied() {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "Le propriétaire de cette clé n'a pas la permission d'effectuer cette action.");
    }

    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<Map<String, String>> handleTooLarge() {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "Le fichier dépasse la taille maximale autorisée (10 Mo).");
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<Map<String, String>> handleInvalidType() {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVALID_FILE_TYPE",
                "Seuls les fichiers PDF et XML sont acceptés.");
    }

    @ExceptionHandler(JobConflictException.class)
    public ResponseEntity<Map<String, String>> handleJobConflict() {
        return error(HttpStatus.CONFLICT, "JOB_CONFLICT", "Un traitement est déjà en cours pour ce document.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize() {
        return handleTooLarge();
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, String>> handleMissingFile() {
        return error(HttpStatus.BAD_REQUEST, "FILE_MISSING", "Envoyez le fichier dans le champ multipart \"file\".");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "Paramètre invalide : " + e.getName() + ".");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException e) {
        HttpStatusCode status = e.getStatusCode();
        String code = status.value() == 404 ? "NOT_FOUND" : "ERROR_" + status.value();
        return error(status, code, e.getReason() != null ? e.getReason() : "Erreur.");
    }

    // Autres erreurs Spring MVC (HttpRequestMethodNotSupportedException, ...) et toute erreur inattendue.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleOther(Exception e) {
        if (e instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            String detail = errorResponse.getBody().getDetail();
            return error(status, "REQUEST_ERROR_" + status.value(), detail != null ? detail : "Requête invalide.");
        }
        log.error("Unexpected error on the public API", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Erreur interne.");
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatusCode status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("error", code, "message", message));
    }
}
