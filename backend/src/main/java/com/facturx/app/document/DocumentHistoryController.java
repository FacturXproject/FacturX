package com.facturx.app.document;

import com.facturx.app.auth.AppUserPrincipal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping; //Get
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RequestMapping("/api/documents")
@RestController
public class DocumentHistoryController {

    private final DocumentHistoryService documentHistoryService;

    public DocumentHistoryController(DocumentHistoryService documentHistoryService) {
        this.documentHistoryService = documentHistoryService;
    }

    private Long currentUserId(Authentication authentication) {
        AppUserPrincipal principal =
                (AppUserPrincipal) authentication.getPrincipal();

        return principal.getUser().getId();
    }

    @GetMapping
    public Page<DocumentHistoryResponse> getDocuments(
            @RequestParam Long organizationId,
            Authentication authentication,
            Pageable pageable) {

        return documentHistoryService.getDocuments(
                organizationId,
                currentUserId(authentication),
                pageable
        );
    }
    //i change the Yanis endpoint for down 
    @GetMapping("/{id}")
    public DocumentHistoryResponse getDocument(@PathVariable Long id, Authentication authentication)
    {
        return documentHistoryService.detail(id, currentUserId(authentication));
    }


}