package com.facturx.app.publicapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * What a key is allowed to do. A scope never grants more than the key owner's role in
 * the organization (see PermissionService) - it can only narrow it down.
 */
public enum ApiKeyScope {
    // GET endpoints: list, status, report, download
    DOCUMENTS_READ("documents:read"),
    // POST / PUT / DELETE endpoints: upload, validate, rename, delete
    DOCUMENTS_WRITE("documents:write");

    private final String value;

    ApiKeyScope(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    // Name of the Spring Security authority carried by a key holding this scope.
    public String authority() {
        return "SCOPE_" + value;
    }

    @JsonCreator
    public static ApiKeyScope fromValue(String value) {
        for (ApiKeyScope scope : values()) {
            if (scope.value.equals(value)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("Unknown scope: " + value);
    }
}
