package com.facturx.app.organization;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/organizations/{orgId}/invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping
    public ResponseEntity<InvitationResponse> create(
            @PathVariable Long orgId,
            @Valid @RequestBody InvitationRequest request) {
        InvitationResponse response = invitationService.create(orgId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<InvitationResponse>> list(@PathVariable Long orgId) {
        List<InvitationResponse> invitations = invitationService.getByOrganization(orgId);
        return ResponseEntity.ok(invitations);
    }
}