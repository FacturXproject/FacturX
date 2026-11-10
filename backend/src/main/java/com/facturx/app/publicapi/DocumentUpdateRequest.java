package com.facturx.app.publicapi;

import io.swagger.v3.oas.annotations.media.Schema;

/** F17 : corps de PUT /documents/{id}. Seul le nom du fichier est modifiable. */
public record DocumentUpdateRequest(
        @Schema(description = "Nouveau nom du fichier (255 caractères maximum).", example = "facture-2026-001.pdf")
        String filename) {
}
