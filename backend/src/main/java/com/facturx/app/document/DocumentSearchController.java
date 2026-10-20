package com.facturx.app.document;

import com.facturx.app.auth.AppUserPrincipal;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * F16 - GET /api/documents/search. Supported filters are documented in README_F16.md
 * (this package). Parameters are taken as raw strings on purpose: the service
 * validates them and answers 400 INVALID_SEARCH_FILTER with a readable message,
 * instead of Spring's generic type-mismatch error.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentSearchController {

    private final DocumentSearchService documentSearchService;

    public DocumentSearchController(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    @GetMapping("/search")
    public DocumentSearchResponse search(
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> type,
            @RequestParam(required = false) String ownerId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size,
            Authentication authentication) {

        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();

        return documentSearchService.search(
                new DocumentSearchService.SearchParams(
                        organizationId, q, status, type, ownerId, from, to, error, sort, page, size),
                principal.getUser().getId());
    }
}
