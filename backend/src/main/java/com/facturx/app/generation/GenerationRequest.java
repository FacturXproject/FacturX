package com.facturx.app.generation;

/** F13 : corps (optionnel) de POST /api/documents/{id}/generate. Profil EN16931 par defaut. */
public record GenerationRequest(String profile) {
}
