package com.facturx.app.apikey;

/**
 * Reponse a la creation : la seule fois ou le secret complet ("key") est renvoye.
 * Il n'est stocke nulle part en clair, il ne pourra plus etre affiche ensuite.
 */
public record ApiKeyCreatedResponse(ApiKeyResponse apiKey, String key) {
}
