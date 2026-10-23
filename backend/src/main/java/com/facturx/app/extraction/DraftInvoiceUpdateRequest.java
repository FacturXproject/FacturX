package com.facturx.app.extraction;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * F12 : corps de PUT /api/documents/{id}/draft. Les montants arrivent en texte
 * (formats francais acceptes, voir DraftCorrection) ; les totaux ne sont pas
 * attendus : le serveur les recalcule toujours.
 *
 * @param updatedAt valeur de updatedAt lue par le client au chargement. Si elle
 *                  ne correspond plus au brouillon en base, la sauvegarde est
 *                  refusee (409) au lieu d'ecraser une correction plus recente.
 */
public record DraftInvoiceUpdateRequest(
        Map<String, String> fields,
        List<LineUpdateRequest> lines,
        OffsetDateTime updatedAt
) {
    /**
     * @param vatRate taux de la ligne ; vide = taux par defaut de la facture (champ vatRate).
     */
    public record LineUpdateRequest(
            String description,
            String quantity,
            String unitPrice,
            String vatRate
    ) {
    }
}
