package com.facturx.app.apikey;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * F17 : authentifie chaque requete de l'API publique par son en-tete X-API-Key (ou
 * "Authorization: Bearer fxk_..."), puis applique la limite de debit de la cle.
 *
 * Volontairement pas un @Component : Spring Boot l'enregistrerait alors comme filtre
 * servlet sur TOUTES les requetes. Il n'existe que dans la chaine /api/public/**.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";
    private static final String BEARER = "Bearer ";

    private final ApiKeyService apiKeyService;
    private final ApiKeyRateLimiter rateLimiter;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService, ApiKeyRateLimiter rateLimiter) {
        this.apiKeyService = apiKeyService;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String secret = extractKey(request);
        if (secret == null) {
            ApiErrorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "API_KEY_MISSING", "Clé API manquante : envoyez-la dans l'en-tête X-API-Key.");
            return;
        }

        Optional<ApiKeyPrincipal> principal = apiKeyService.authenticate(secret);
        if (principal.isEmpty()) {
            ApiErrorWriter.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_API_KEY", "Clé API invalide ou révoquée.");
            return;
        }

        ApiKeyRateLimiter.Decision decision = rateLimiter.tryConsume(principal.get().apiKeyId());
        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(decision.resetInSeconds()));
        if (!decision.allowed()) {
            response.setHeader("Retry-After", String.valueOf(decision.resetInSeconds()));
            ApiErrorWriter.write(response, 429,
                    "RATE_LIMITED", "Trop de requêtes pour cette clé API. Réessayez dans "
                            + decision.resetInSeconds() + " s.");
            return;
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new ApiKeyAuthenticationToken(principal.get()));
        SecurityContextHolder.setContext(context);

        chain.doFilter(request, response);
    }

    private static String extractKey(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER)) {
            String token = authorization.substring(BEARER.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
