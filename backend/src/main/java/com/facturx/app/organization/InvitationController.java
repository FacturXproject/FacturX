package com.facturx.app.organization;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.facturx.app.auth.AppUserPrincipal;
import java.util.List;

@RestController
@RequestMapping("/api/organizations/{orgId}/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    private Long currentUserId(Authentication authentication) {
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        return principal.getUser().getId();
    }

    @PostMapping
    public ResponseEntity<InvitationResponse> create(
            @PathVariable Long orgId,
            @Valid @RequestBody InvitationRequest request,
            Authentication authentication) {
        InvitationResponse response = invitationService.create(orgId, request, currentUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<InvitationResponse>> list(
            @PathVariable Long orgId,
            Authentication authentication) {
        List<InvitationResponse> invitations = invitationService.getByOrganization(orgId, currentUserId(authentication));
        return ResponseEntity.ok(invitations);
    }
    
    @PatchMapping("/{invitationId}/revoke")
    public ResponseEntity<InvitationResponse> revoke(
        @PathVariable Long invitationId,
        Authentication authentication) {
        
        InvitationResponse response = invitationService.revoke(
                    invitationId, currentUserId(authentication));

    return ResponseEntity.ok(response);
}
}