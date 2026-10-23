package com.facturx.app.extraction;

/**
 * F12 : le brouillon a ete modifie depuis que le client l'a charge (autre onglet,
 * autre membre du cabinet). On refuse plutot que d'ecraser silencieusement.
 */
public class DraftConflictException extends RuntimeException {
}
