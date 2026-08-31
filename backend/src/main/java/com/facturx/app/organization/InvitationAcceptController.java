package com.facturx.app.organization;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/invitations")
public class InvitationAcceptController {

    private final InvitationService invitationService;

    public InvitationAcceptController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/accept")
    public ResponseEntity<InvitationResponse> accept(@RequestParam String token) {
        return ResponseEntity.ok(invitationService.accept(token));
    }
}