package com.facturx.app.publicapi;

import java.time.Instant;
import java.util.Set;

// Never carries the key or its hash - only the prefix, to recognise a key in a list.
public record ApiKeyResponse(
        Long id,
        String name,
        Long organizationId,
        String organizationName,
        String keyPrefix,
        Set<ApiKeyScope> scopes,
        Instant createdAt,
        Instant lastUsedAt,
        Instant revokedAt
) {
    public static ApiKeyResponse from(ApiKey apiKey, String organizationName) {
        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getOrganizationId(),
                organizationName,
                apiKey.getKeyPrefix(),
                apiKey.getScopes(),
                apiKey.getCreatedAt(),
                apiKey.getLastUsedAt(),
                apiKey.getRevokedAt()
        );
    }
}
