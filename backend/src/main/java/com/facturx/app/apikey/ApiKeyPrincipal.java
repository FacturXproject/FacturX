package com.facturx.app.apikey;

import java.util.Set;

/**
 * Identite portee par une requete de l'API publique : la cle, son proprietaire et
 * l'organisation dans laquelle elle agit. Jamais stockee en session (API sans etat).
 */
public record ApiKeyPrincipal(
        Long apiKeyId,
        String apiKeyName,
        Long userId,
        Long organizationId,
        Set<ApiKeyScope> scopes
) {
    public static ApiKeyPrincipal from(ApiKey apiKey) {
        return new ApiKeyPrincipal(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getOwner().getId(),
                apiKey.getOrganization().getId(),
                Set.copyOf(apiKey.getScopes())
        );
    }
}
