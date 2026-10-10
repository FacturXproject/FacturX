package com.facturx.app.publicapi;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates a public API request from its X-API-Key header and then applies the
 * per-key rate limit. Not a Spring bean on purpose: a Filter bean would be registered
 * by Spring Boot on every request of the application, and this one must only run
 * inside the public API security chain (see {@link PublicApiSecurityConfig}).
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    private final ApiKeyService apiKeyService;
    private final ApiRateLimiter rateLimiter;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService, ApiRateLimiter rateLimiter) {
        this.apiKeyService = apiKeyService;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse originalResponse,
                                    FilterChain filterChain) throws ServletException, IOException {

        // From here on, any sendError() becomes a JSON error instead of a /error dispatch.
        HttpServletResponse response = new JsonErrorResponseWrapper(originalResponse);

        try {
            authenticateAndContinue(request, response, filterChain);
        } catch (ServletException | RuntimeException e) {
            // An unexpected failure (e.g. a stored file that cannot be read) must not
            // escape either: it would also end up re-dispatched to /error.
            log.error("Public API request failed: {} {}", request.getMethod(), request.getRequestURI(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void authenticateAndContinue(HttpServletRequest request,
                                         HttpServletResponse response,
                                         FilterChain filterChain) throws ServletException, IOException {

        String rawKey = request.getHeader(HEADER);

        // No key at all: carry on unauthenticated. The documentation is public; every
        // other route is refused further down the chain with a 401.
        if (rawKey == null || rawKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<ApiKeyPrincipal> principal = apiKeyService.authenticate(rawKey.trim());
        if (principal.isEmpty()) {
            PublicApiSecurityConfig.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_API_KEY", "Clé API invalide ou révoquée.");
            return;
        }

        ApiRateLimiter.Decision decision = rateLimiter.consume(principal.get().keyId());
        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(decision.secondsUntilReset()));

        if (!decision.allowed()) {
            response.setHeader("Retry-After", String.valueOf(decision.secondsUntilReset()));
            PublicApiSecurityConfig.writeError(response, 429,
                    "RATE_LIMIT_EXCEEDED", "Trop de requêtes. Réessayez dans quelques instants.");
            return;
        }

        List<SimpleGrantedAuthority> authorities = principal.get().scopes().stream()
                .map(scope -> new SimpleGrantedAuthority(scope.authority()))
                .toList();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal.get(), null, authorities));
        SecurityContextHolder.setContext(context);

        filterChain.doFilter(request, response);
    }
}
