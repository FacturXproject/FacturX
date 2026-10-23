package com.facturx.app.apikey;

/**
 * F17 : ce qu'une cle API a le droit de faire. Les scopes s'ajoutent aux permissions du
 * role (F04), ils ne les remplacent pas : une cle ne fait jamais plus que son proprietaire.
 */
public enum ApiKeyScope {
    /** Lister les documents, lire leur statut, leur rapport, les telecharger. */
    DOCUMENTS_READ,
    /** Deposer un document. */
    DOCUMENTS_WRITE,
    /** Lancer une validation Factur-X (job asynchrone F14). */
    DOCUMENTS_VALIDATE;

    /** Nom de l'autorite Spring Security portee par une cle qui a ce scope. */
    public String authority() {
        return "SCOPE_" + name();
    }
}
