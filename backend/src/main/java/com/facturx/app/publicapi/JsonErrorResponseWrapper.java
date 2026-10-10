package com.facturx.app.publicapi;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * Turns every sendError() of a public API request into our usual JSON error body.
 *
 * Why it is needed: Spring answers "plain" HTTP errors (malformed id or JSON, missing
 * file part, unsupported method, unknown route, ResponseStatusException...) with
 * response.sendError(), which makes Tomcat re-dispatch the request to /error. That
 * second dispatch is outside /api/public/**, so it lands in the session security
 * chain, where an API-key request is anonymous - and the client would receive a
 * misleading 401 instead of the real 400/404/405. Writing the body here means the
 * /error dispatch never happens.
 */
class JsonErrorResponseWrapper extends HttpServletResponseWrapper {

    JsonErrorResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void sendError(int status) throws IOException {
        sendError(status, null);
    }

    @Override
    public void sendError(int status, String message) throws IOException {
        if (isCommitted()) {
            return;
        }
        resetBuffer();
        PublicApiSecurityConfig.writeError(
                (HttpServletResponse) getResponse(),
                status,
                errorCode(status),
                message != null && !message.isBlank() ? message : defaultMessage(status));
    }

    private static String errorCode(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 406 -> "NOT_ACCEPTABLE";
            case 413 -> "FILE_TOO_LARGE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> status >= 500 ? "INTERNAL_ERROR" : "REQUEST_REFUSED";
        };
    }

    private static String defaultMessage(int status) {
        return switch (status) {
            case 400 -> "Requête invalide : vérifiez les paramètres et le corps envoyés.";
            case 404 -> "Ressource introuvable.";
            case 405 -> "Méthode HTTP non autorisée sur cette route.";
            case 406 -> "Format de réponse demandé non disponible.";
            case 413 -> "Le fichier dépasse la taille maximale autorisée (10 Mo).";
            case 415 -> "Type de contenu non pris en charge.";
            default -> status >= 500
                    ? "Erreur interne du serveur."
                    : "La requête a été refusée.";
        };
    }
}
