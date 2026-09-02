package com.facturx.app.organization;

import java.time.LocalDateTime;

public record InvitationResponse(
    Long id,
    String email,
    Role role,
    InvitationStatus status,
    LocalDateTime expiresAt
) {}