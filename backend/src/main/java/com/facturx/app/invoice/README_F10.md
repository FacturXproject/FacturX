# F10 — Lecteur XML facture

## Objectif

F10 permet de transformer un document XML CII déjà stocké dans l'application en une représentation de facture lisible.

La feature ne valide pas la conformité Factur-X.
Elle lit uniquement les données métier présentes dans le XML.

F10 permet également de consulter le XML brut afin de comparer le document original avec la vue lisible.

---

## Endpoints

### Vue lisible

GET /api/documents/{id}/invoice-view

Retourne les données de la facture sous forme structurée.

### XML brut

GET /api/documents/{id}/xml

Retourne le fichier XML original.

L'utilisateur doit être authentifié et avoir accès au document.

---

## Fonctionnement

documentId
   ↓
récupération du Document
   ↓
vérification des permissions
   ↓
lecture du fichier depuis le stockage
   ↓
vérification du format XML
   ↓
parsing CII
   ↓
InvoiceViewResponse
   ↓
affichage frontend

F10 réutilise les documents déjà gérés par F06/F07 et ne crée pas de nouvelle table en base.

---

## Données extraites

Le parser retourne notamment :

- numéro de facture
- date
- devise
- vendeur
- acheteur
- lignes de facture
- quantité
- prix unitaire
- total par ligne
- sous-total
- TVA
- total général

Exemple de réponse :

{
  "invoiceNumber": "INV-001",
  "invoiceDate": "2026-09-14",
  "currency": "EUR",
  "seller": {
    "name": "Seller Company",
    "address": "1 Seller Street, 06000 Nice, FR",
    "vatId": "FR123456789"
  },
  "buyer": {
    "name": "Buyer Company",
    "address": "2 Buyer Street, 75001 Paris, FR",
    "vatId": "FR987654321"
  },
  "lines": [
    {
      "description": "Consulting service",
      "quantity": 2,
      "unitPrice": 100.00,
      "lineTotal": 200.00
    }
  ],
  "subtotal": 200.00,
  "vat": 40.00,
  "total": 240.00
}

---

## Backend

invoice/
├── CiiInvoiceParser.java
├── InvoiceViewController.java
├── InvoiceViewService.java
├── InvoiceViewResponse.java
├── InvalidInvoiceXmlException.java
└── InvoiceViewExceptionHandler.java

### InvoiceViewController

Expose les endpoints :

GET /api/documents/{id}/invoice-view
GET /api/documents/{id}/xml

### InvoiceViewService

- récupère le document
- vérifie les permissions
- lit le fichier stocké
- vérifie qu'il s'agit d'un XML
- appelle le parser pour la vue lisible
- retourne le XML original pour la vue XML

### CiiInvoiceParser

Parse le XML CII avec DOM/XPath et extrait les données métier de la facture.

### InvoiceViewResponse

Définit la structure retournée au frontend.

---

## Frontend

La page :

/documents/:id/invoice

affiche la facture sous une forme lisible.

Elle présente notamment :

- vendeur
- acheteur
- informations générales
- lignes de facture
- TVA
- totaux

Un bouton permet de basculer entre :

Voir XML

et :

Vue lisible

La vue XML affiche directement le contenu original retourné par :

GET /api/documents/{id}/xml

---

## Permissions

F10 utilise les permissions existantes :

VIEW_ALL_DOCUMENTS

ou :

VIEW_OWN_DOCUMENTS

si l'utilisateur est propriétaire du document.

Un utilisateur sans accès reçoit :

403 Forbidden

Les permissions sont vérifiées côté backend.

---

## Gestion des erreurs

| Cas | Réponse |
|---|---|
| Document inexistant | 404 DOCUMENT_NOT_FOUND |
| Utilisateur sans permission | 403 Forbidden |
| XML invalide ou document non XML | 422 INVALID_INVOICE_XML |

---

## Tests

Deux types de tests sont présents :

CiiInvoiceParserTest
InvoiceViewIntegrationTest

Ils couvrent notamment :

- XML CII valide
- plusieurs lignes
- champs manquants
- XML mal formé
- document inexistant
- PDF refusé
- contrôle des permissions

Lancer les tests F10 :

cd backend
./mvnw -Dtest=CiiInvoiceParserTest,InvoiceViewIntegrationTest test

---

## Résumé

XML stocké
   ↓
permissions
   ↓
parser CII
   ↓
facture lisible
   ↓
vue lisible / XML brut

F10 reste indépendant de la validation de conformité :

F08/F09 → validation de conformité Factur-X
F10     → lecture et affichage des données de la facture
