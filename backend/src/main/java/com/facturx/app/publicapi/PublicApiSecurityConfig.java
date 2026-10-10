package com.facturx.app.publicapi;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * F17: a second security chain, for /api/public/** only. It has priority (@Order(1))
 * over the session chain in SecurityConfig, which keeps handling everything else
 * exactly as before.
 *
 * Differences with the session chain, and why:
 * - no session, no cookie: each request is authenticated by its X-API-Key header;
 * - no CSRF: CSRF abuses a cookie the browser sends on its own, and there is none here;
 * - scopes are enforced right here, by HTTP method, before any controller runs.
 */
@Configuration
public class PublicApiSecurityConfig {

    public static final String BASE_PATH = "/api/public/v1";

    @Bean
    @Order(1)
    public SecurityFilterChain publicApiSecurityFilterChain(HttpSecurity http,
                                                            ApiKeyService apiKeyService,
                                                            ApiRateLimiter rateLimiter) throws Exception {
        http
                .securityMatcher("/api/public/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .addFilterBefore(new ApiKeyAuthenticationFilter(apiKeyService, rateLimiter),
                        UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        // Documentation (OpenAPI description + Swagger UI) is readable without a key.
                        .requestMatchers(HttpMethod.GET,
                                "/api/public/docs", "/api/public/docs/**",
                                "/api/public/swagger-ui/**",
                                BASE_PATH + "/openapi", BASE_PATH + "/openapi/**").permitAll()
                        .requestMatchers(HttpMethod.GET, BASE_PATH + "/**")
                                .hasAuthority(ApiKeyScope.DOCUMENTS_READ.authority())
                        .requestMatchers(BASE_PATH + "/**")
                                .hasAuthority(ApiKeyScope.DOCUMENTS_WRITE.authority())
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        "API_KEY_REQUIRED",
                                        "Clé API requise : envoyez-la dans l'en-tête X-API-Key."))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN,
                                        "INSUFFICIENT_SCOPE",
                                        "Cette clé API n'a pas la permission requise pour cette action.")));

        return http.build();
    }

    // Same {"error","message"} shape as every other error of the API.
    static void writeError(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"error":"%s","message":"%s"}""".formatted(error, escapeJson(message)));
    }

    // The message can come from an exception, so it is escaped rather than trusted.
    private static String escapeJson(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                default -> {
                    if (c < 0x20) {
                        escaped.append(' ');
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
