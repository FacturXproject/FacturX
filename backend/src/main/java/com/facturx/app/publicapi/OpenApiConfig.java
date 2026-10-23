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
 * F17 : en-tete de la specification OpenAPI servie sur /api/docs/openapi (Swagger UI :
 * /api/docs/swagger-ui/index.html). Seules les routes /api/public/** y figurent
 * (springdoc.paths-to-match).
 */
@Configuration
public class OpenApiConfig {

    public static final String API_KEY_SCHEME = "apiKey";

    @Bean
    public OpenAPI publicApiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FacturX - API publique")
                        .version("v1")
                        .description("""
                                API pour les éditeurs de logiciels comptables : déposer des factures, \
                                suivre leur statut, lire le rapport de conformité Factur-X et télécharger les fichiers.

                                **Authentification** : une clé API créée depuis la page "Clés API" de l'application, \
                                envoyée dans l'en-tête `X-API-Key` (ou `Authorization: Bearer <clé>`). \
                                La clé agit dans une seule organisation, avec au plus les droits de son propriétaire, \
                                limités par ses scopes (DOCUMENTS_READ, DOCUMENTS_WRITE, DOCUMENTS_VALIDATE).

                                **Limite de débit** : 60 requêtes par minute et par clé par défaut. Chaque réponse \
                                porte `X-RateLimit-Limit`, `X-RateLimit-Remaining` et `X-RateLimit-Reset` ; \
                                au-delà, HTTP 429 avec `Retry-After`.

                                **Erreurs** : corps JSON `{"error": "CODE", "message": "..."}`."""))
                // URL relative : Swagger UI appelle l'API sur l'origine qui l'a servie (Nginx).
                .servers(List.of(new Server().url("/")))
                .components(new Components().addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-API-Key")
                        .description("Clé API au format fxk_...")));
    }
}
