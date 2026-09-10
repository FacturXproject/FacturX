package com.facturx.app.organization;

import java.time.LocalDateTime;

public record OrganizationResponse(
        Long id,
        String name,
        LocalDateTime createdAt
) {
    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getCreatedAt()
        );
    }
}