package com.facturx.app.organization;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.facturx.app.auth.AppUserPrincipal;

@RestController
@RequestMapping("/api/invitations")
public class InvitationAcceptController {

    private final InvitationService invitationService;

    public InvitationAcceptController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    // PUBLIC : consultation avant authentification
    @GetMapping("/{token}")
    public ResponseEntity<InvitationResponse> check(@PathVariable String token) {
        return ResponseEntity.ok(invitationService.check(token));
    }

    // AUTHENTIFIÉ : acceptation réelle
    @PostMapping("/accept")
    public ResponseEntity<InvitationResponse> accept(
            @RequestParam String token,
            Authentication authentication) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        Long currentUserId = principal.getUser().getId();
        return ResponseEntity.ok(invitationService.accept(token, currentUserId));
    }
}