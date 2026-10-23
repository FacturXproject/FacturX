package com.facturx.app.config;

import com.facturx.app.apikey.ApiErrorWriter;
import com.facturx.app.apikey.ApiKeyAuthenticationFilter;
import com.facturx.app.apikey.ApiKeyRateLimiter;
import com.facturx.app.apikey.ApiKeyScope;
import com.facturx.app.apikey.ApiKeyService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

/**
 * F17 : chaines de securite de l'API publique, evaluees AVANT la chaine session de
 * {@link SecurityConfig} (qui reste la chaine par defaut pour tout le reste).
 *
 * - /api/public/** : authentification par cle API uniquement, sans etat. Le cookie de
 *   session est ignore (le contexte n'est jamais lu depuis la session), donc le CSRF
 *   n'a pas d'objet ici : aucune authentification n'est envoyee automatiquement par le
 *   navigateur. Les scopes de la cle sont verifies ici, les permissions du role (F04)
 *   dans le service.
 * - /api/docs/** : la specification OpenAPI et Swagger UI, publiques et en lecture seule.
 */
@Configuration
public class PublicApiSecurityConfig {

    private static final String DOCUMENTS = "/api/public/v1/documents";

    @Bean
    @Order(1)
    public SecurityFilterChain publicApiDocsFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/docs/**")
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(
                        new RequestAttributeSecurityContextRepository()))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/docs/**").permitAll()
                        .anyRequest().denyAll());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain publicApiFilterChain(
            HttpSecurity http,
            ApiKeyService apiKeyService,
            ApiKeyRateLimiter rateLimiter) throws Exception {

        http
                .securityMatcher("/api/public/**")
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(
                        new RequestAttributeSecurityContextRepository()))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .anonymous(anonymous -> anonymous.disable())
                .addFilterBefore(
                        new ApiKeyAuthenticationFilter(apiKeyService, rateLimiter),
                        UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, DOCUMENTS + "/*/validate")
                                .hasAuthority(ApiKeyScope.DOCUMENTS_VALIDATE.authority())
                        .requestMatchers(HttpMethod.POST, DOCUMENTS)
                                .hasAuthority(ApiKeyScope.DOCUMENTS_WRITE.authority())
                        // HEAD est servi par les handlers @GetMapping : meme scope que GET.
                        .requestMatchers(HttpMethod.GET, DOCUMENTS, DOCUMENTS + "/**")
                                .hasAuthority(ApiKeyScope.DOCUMENTS_READ.authority())
                        .requestMatchers(HttpMethod.HEAD, DOCUMENTS, DOCUMENTS + "/**")
                                .hasAuthority(ApiKeyScope.DOCUMENTS_READ.authority())
                        // Toute autre route ou methode est refusee : rien ne doit passer
                        // avec une simple cle authentifiee sans regle de scope explicite.
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, e) -> ApiErrorWriter.write(response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "API_KEY_MISSING", "Clé API manquante : envoyez-la dans l'en-tête X-API-Key."))
                        .accessDeniedHandler((request, response, e) -> ApiErrorWriter.write(response,
                                HttpServletResponse.SC_FORBIDDEN,
                                "INSUFFICIENT_SCOPE", "Cette clé API n'a pas le scope requis pour cette opération.")));

        return http.build();
    }
}
