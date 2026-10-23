package com.facturx.app.apikey;

import java.time.Instant;
import java.util.Set;

/** Une cle telle que l'interface la voit : jamais le secret, seulement son prefixe. */
public record ApiKeyResponse(
        Long id,
        String name,
        String prefix,
        Long organizationId,
        String organizationName,
        Set<ApiKeyScope> scopes,
        Instant createdAt,
        Instant lastUsedAt,
        Instant revokedAt,
        boolean active
) {
    public static ApiKeyResponse from(ApiKey apiKey) {
        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getPrefix(),
                apiKey.getOrganization().getId(),
                apiKey.getOrganization().getName(),
                apiKey.getScopes(),
                apiKey.getCreatedAt(),
                apiKey.getLastUsedAt(),
                apiKey.getRevokedAt(),
                !apiKey.isRevoked()
        );
    }
}
