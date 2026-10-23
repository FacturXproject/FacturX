package com.facturx.app.apikey;

import com.facturx.app.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * F17 : gestion des cles API depuis l'application (session + CSRF, comme le reste de
 * l'interface). Les cles elles-memes s'utilisent sur /api/public/v1/**.
 */
@RestController
@RequestMapping("/api/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    private Long currentUserId(Authentication authentication) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return principal.getUser().getId();
    }

    // GET /api/api-keys - mes cles (actives et revoquees), sans les secrets
    @GetMapping
    public List<ApiKeyResponse> list(Authentication authentication) {
        return apiKeyService.listForUser(currentUserId(authentication))
                .stream()
                .map(ApiKeyResponse::from)
                .toList();
    }

    // POST /api/api-keys - {name, organizationId, scopes} ; le secret n'est renvoye qu'ici
    @PostMapping
    public ResponseEntity<ApiKeyCreatedResponse> create(
            @Valid @RequestBody ApiKeyCreateRequest request,
            Authentication authentication) {

        ApiKeyService.CreatedApiKey created =
                apiKeyService.create(currentUserId(authentication), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiKeyCreatedResponse(ApiKeyResponse.from(created.apiKey()), created.secret()));
    }

    // DELETE /api/api-keys/{id} - revocation (la cle reste listee, marquee revoquee)
    @DeleteMapping("/{id}")
    public ApiKeyResponse revoke(@PathVariable Long id, Authentication authentication) {
        return ApiKeyResponse.from(apiKeyService.revoke(currentUserId(authentication), id));
    }
}
