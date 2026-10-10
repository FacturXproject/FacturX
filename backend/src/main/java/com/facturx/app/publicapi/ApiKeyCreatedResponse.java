package com.facturx.app.publicapi;

// Returned once, at creation: the only moment the full key exists outside the client.
public record ApiKeyCreatedResponse(
        String key,
        ApiKeyResponse apiKey
) {
}
