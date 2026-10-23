package com.facturx.app.exportimport;
import com.facturx.app.organization.OrganizationMember;
import com.facturx.app.organization.Role;

public record MemberExportResponse (
    String name,
    String email,
    Role role
)
{
    public static MemberExportResponse from(OrganizationMember member)
    {
        return new MemberExportResponse(
            member.getUser().getFirstName() + " " + member.getUser().getLastName(),
            member.getUser().getEmail(),
            member.getRole());
    }
}
