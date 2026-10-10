package com.facturx.app.publicapi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DocumentUpdateRequest(
        @NotBlank(message = "Le nom du fichier est obligatoire.")
        @Size(max = 255, message = "Le nom du fichier ne doit pas dépasser 255 caractères.")
        @Pattern(regexp = "[^/\\\\\\p{Cntrl}]*", message = "Le nom du fichier contient des caractères interdits.")
        String filename
) {
}
