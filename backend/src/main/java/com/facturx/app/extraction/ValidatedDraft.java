package com.facturx.app.extraction;

/**
 * F13 : brouillon pret pour la generation Factur-X, revalide par {@link DraftCorrection}.
 *
 * @param humanChecked vrai si le brouillon a ete enregistre au moins une fois depuis le
 *                     formulaire de correction (F12) : l'extraction seule ne suffit pas.
 */
public record ValidatedDraft(Long draftId, Long organizationId, boolean humanChecked,
                             DraftCorrection.Result data) {
}
