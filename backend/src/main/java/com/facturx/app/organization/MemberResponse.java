package com.facturx.app.organization;

import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String email,
        Role role,
        LocalDateTime joinedAt
) {
    public static MemberResponse from(OrganizationMember member) {
        return new MemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getFirstName(),
                member.getUser().getLastName(),
                member.getUser().getEmail(),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}