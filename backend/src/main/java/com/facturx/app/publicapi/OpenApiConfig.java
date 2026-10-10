package com.facturx.app.publicapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * F17: the OpenAPI description served at /api/public/v1/openapi and rendered by
 * Swagger UI at /api/public/docs. Only /api/public/v1/** is described (see
 * springdoc.paths-to-match in application.properties) - the session API used by our
 * own frontend is not part of the public contract.
 */
@Configuration
public class OpenApiConfig {

    public static final String API_KEY_SCHEME = "ApiKey";

    @Bean
    public OpenAPI publicApiDescription(ApiRateLimiter rateLimiter) {
        return new OpenAPI()
                .info(new Info()
                        .title("Factur-X - API publique")
                        .version("v1")
                        .description("""
                                API destinée aux éditeurs de logiciels comptables : déposer une facture, \
                                lancer son contrôle de conformité Factur-X, lire le rapport et récupérer le fichier.

                                **Authentification** : une clé API dans l'en-tête `X-API-Key`. Une clé est \
                                créée depuis l'application (page « Clés API »), elle est liée à une seule \
                                organisation et agit avec le rôle de son propriétaire dans celle-ci.

                                **Permissions** : `documents:read` pour les requêtes GET, `documents:write` \
                                pour POST, PUT et DELETE.

                                **Limite de débit** : %d requêtes par minute et par clé. Au-delà, la réponse \
                                est `429` avec l'en-tête `Retry-After`. Chaque réponse porte les en-têtes \
                                `X-RateLimit-Limit`, `X-RateLimit-Remaining` et `X-RateLimit-Reset`.

                                **Erreurs** : toujours `{"error": "CODE", "message": "..."}`."""
                                .formatted(rateLimiter.getRequestsPerMinute())))
                // Relative server URL: Swagger UI then calls the same origin it was loaded
                // from (https://localhost:8443 behind nginx), whatever the backend's own host is.
                .servers(List.of(new Server().url("/")))
                .components(new Components().addSecuritySchemes(API_KEY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(ApiKeyAuthenticationFilter.HEADER)));
    }
}
