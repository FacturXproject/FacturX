package com.facturx.app.publicapi;

import com.facturx.app.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * F17: key management for the logged-in user (session cookie + CSRF, like the rest of
 * the app). The public API itself lives in {@link PublicApiController}.
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

    // GET /api/api-keys - mes cles (jamais la cle complete, seulement son prefixe)
    @GetMapping
    public List<ApiKeyResponse> getMyKeys(Authentication authentication) {
        return apiKeyService.listMine(currentUserId(authentication));
    }

    // POST /api/api-keys - cree une cle ; la cle complete n'est renvoyee qu'ici, une seule fois
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKeyCreatedResponse create(@Valid @RequestBody ApiKeyCreateRequest request,
                                        Authentication authentication) {
        return apiKeyService.create(
                currentUserId(authentication),
                request.name(),
                request.organizationId(),
                request.scopes());
    }

    // DELETE /api/api-keys/{id} - revoque la cle
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable Long id, Authentication authentication) {
        apiKeyService.revoke(id, currentUserId(authentication));
    }
}
