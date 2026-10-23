package com.facturx.app.apikey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record ApiKeyCreateRequest(
        @NotBlank(message = "Le nom de la clé est obligatoire.")
        @Size(max = 100, message = "Le nom de la clé ne doit pas dépasser 100 caractères.")
        String name,

        @NotNull(message = "L'organisation est obligatoire.")
        Long organizationId,

        @NotEmpty(message = "Choisissez au moins un scope.")
        Set<ApiKeyScope> scopes
) {
}
