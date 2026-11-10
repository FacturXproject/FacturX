package com.facturx.app.organization;

public class InvalidOrganizationNameException extends RuntimeException {

    public InvalidOrganizationNameException() {
        super("Organization name must be between 1 and "
                + OrganizationService.MAX_NAME_LENGTH + " characters.");
    }
}
