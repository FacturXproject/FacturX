package com.facturx.app.exportimport;

import com.facturx.app.auth.AppUserPrincipal;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{orgId}/import")
public class ImportController {

    private final InvitationImportService importService;

    public ImportController(InvitationImportService importService) {
        this.importService = importService;
    }

    @PostMapping(
            value = "/invitations",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<List<InvitationImportResponse>> importInvitations(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long orgId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean dryRun) {

        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        List<InvitationImportResponse> result;

        try {
            result = importService.importInvitations(
                    file,
                    principal.getUser().getId(),
                    orgId,
                    dryRun
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage(),
                    e
            );
        }

        if (!dryRun && result.stream().anyMatch(row -> !row.valid())) {
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }
}