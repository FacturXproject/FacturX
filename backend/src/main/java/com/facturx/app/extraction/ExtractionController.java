package com.facturx.app.extraction;

import com.facturx.app.auth.AppUserPrincipal;
import com.facturx.app.document.Document;
import com.facturx.app.document.DocumentService;
import com.facturx.app.permission.Permission;
import com.facturx.app.permission.PermissionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class ExtractionController {

    private final ExtractionService extractionService;
    private final DocumentService documentService;
    private final PermissionService permissionService;

    public ExtractionController(
            ExtractionService extractionService,
            DocumentService documentService,
            PermissionService permissionService) {

        this.extractionService = extractionService;
        this.documentService = documentService;
        this.permissionService = permissionService;
    }

    private Long currentUserId(Authentication authentication) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return principal.getUser().getId();
    }

    // POST /api/documents/{id}/extract - lance l'extraction PDF (F11) sur un document
    // deja depose, via le service Python interne, et persiste le brouillon obtenu.
    @PostMapping("/{id}/extract")
    public DraftInvoiceResponse extract(@PathVariable Long id, Authentication authentication) {
        Document document = documentService.getDocument(id);
        permissionService.requirePermission(
                currentUserId(authentication),
                document.getOrganization().getId(),
                Permission.EXTRACT_DOCUMENT);
        return extractionService.extract(id);
    }

    // GET /api/documents/{id}/draft - dernier brouillon extrait pour ce document.
    @GetMapping("/{id}/draft")
    public DraftInvoiceResponse getDraft(@PathVariable Long id, Authentication authentication) {
        Document document = documentService.getDocument(id);
        permissionService.requirePermission(
                currentUserId(authentication),
                document.getOrganization().getId(),
                Permission.EXTRACT_DOCUMENT);
        return extractionService.getLatestDraft(id);
    }
}
