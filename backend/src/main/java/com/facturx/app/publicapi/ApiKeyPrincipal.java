package com.facturx.app.publicapi;

import java.util.Set;

/**
 * The authenticated caller of a public API request: which key was used, on behalf of
 * which user, and the single organization that key is bound to.
 */
public record ApiKeyPrincipal(
        Long keyId,
        Long userId,
        Long organizationId,
        Set<ApiKeyScope> scopes
) {
}
