package com.facturx.app.generation;

import java.time.OffsetDateTime;

/**
 * F13 : resultat d'une generation. Le fichier se telecharge via
 * GET /api/documents/{generatedDocumentId}/download et son rapport complet via
 * GET /api/documents/{generatedDocumentId}/report (F09).
 *
 * @param errorCount   erreurs du rapport de validation (0 si le fichier est conforme)
 * @param errorMessage renseigne seulement si status = FAILED
 */
public record GenerationResponse(
        Long id,
        Long sourceDocumentId,
        Long generatedDocumentId,
        String filename,
        String profile,
        GenerationStatus status,
        Long validationRunId,
        long errorCount,
        long warningCount,
        String errorMessage,
        OffsetDateTime createdAt
) {
}
