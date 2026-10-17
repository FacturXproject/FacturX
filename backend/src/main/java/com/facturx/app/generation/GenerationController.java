package com.facturx.app.generation;

import com.facturx.app.auth.AppUserPrincipal;
import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentService;
import com.facturx.app.permission.Permission;
import com.facturx.app.permission.PermissionService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class GenerationController {

    private final GenerationService generationService;
    private final DocumentService documentService;
    private final PermissionService permissionService;

    public GenerationController(GenerationService generationService,
                                DocumentService documentService,
                                PermissionService permissionService) {
        this.generationService = generationService;
        this.documentService = documentService;
        this.permissionService = permissionService;
    }

    private Long currentUserId(Authentication authentication) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return principal.getUser().getId();
    }

    private Long requireGenerationPermission(Long documentId, Authentication authentication) {
        Document document = documentService.getDocument(documentId);
        Long userId = currentUserId(authentication);
        permissionService.requirePermission(userId, document.getOrganization().getId(),
                Permission.GENERATE_DOCUMENT);
        return userId;
    }

    // POST /api/documents/{id}/generate - genere le Factur-X a partir du brouillon
    // corrige (F12) du document, puis le fait valider par le moteur Java (F13).
    @PostMapping("/{id}/generate")
    public GenerationResponse generate(@PathVariable Long id,
                                       @RequestBody(required = false) GenerationRequest request,
                                       Authentication authentication) {
        Long userId = requireGenerationPermission(id, authentication);
        return generationService.generate(id, request != null ? request.profile() : null, userId);
    }

    // GET /api/documents/{id}/generations - historique des generations du document source.
    @GetMapping("/{id}/generations")
    public List<GenerationResponse> history(@PathVariable Long id, Authentication authentication) {
        requireGenerationPermission(id, authentication);
        return generationService.history(id);
    }
}
