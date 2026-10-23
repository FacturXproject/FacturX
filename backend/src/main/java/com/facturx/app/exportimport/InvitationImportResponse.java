package com.facturx.app.exportimport;

public record InvitationImportResponse(
    int lineNumber,
    String email,
    String role,
    boolean valid,
    String error
)
{

}
