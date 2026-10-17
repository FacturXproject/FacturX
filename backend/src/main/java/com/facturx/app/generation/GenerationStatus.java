package com.facturx.app.generation;

/** F13 : issue d'une generation Factur-X. */
public enum GenerationStatus {
    /** Fichier genere et declare conforme par le moteur de validation Java. */
    VALID,
    /** Fichier genere mais refuse par le moteur de validation (rapport d'erreurs disponible). */
    INVALID,
    /** Generation ou validation impossible (service indisponible, erreur technique). */
    FAILED
}
