# F10 — Lecteur XML facture

## Objectif

F10 permet de transformer un document XML CII déjà stocké dans l'application en une représentation de facture lisible.

La feature ne valide pas la conformité Factur-X.  
Elle lit uniquement les données métier présentes dans le XML.

---

## Endpoint

```http
GET /api/documents/{id}/invoice-view
```

L'utilisateur doit être authentifié et avoir accès au document.

---

## Fonctionnement

```text
documentId
   ↓
récupération du Document
   ↓
vérification des permissions
   ↓
lecture du fichier depuis le stockage F06
   ↓
vérification du format XML
   ↓
parsing CII
   ↓
InvoiceViewResponse
```

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

```json
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
```

---

## Architecture

```text
invoice/
├── CiiInvoiceParser.java
├── InvoiceViewController.java
├── InvoiceViewService.java
├── InvoiceViewResponse.java
├── InvalidInvoiceXmlException.java
└── InvoiceViewExceptionHandler.java
```

### `InvoiceViewController`
Expose l'endpoint HTTP.

### `InvoiceViewService`
Récupère le document, vérifie les permissions et lit le fichier.

### `CiiInvoiceParser`
Parse le XML CII avec DOM/XPath et extrait les données de facture.

### `InvoiceViewResponse`
Définit la structure retournée au frontend.

---

## Permissions

F10 respecte les permissions existantes :

```text
VIEW_ALL_DOCUMENTS
```

ou :

```text
VIEW_OWN_DOCUMENTS
```

si l'utilisateur est propriétaire du document.

Un utilisateur sans accès reçoit un `403 Forbidden`.

---

## Gestion des erreurs

| Cas | Réponse |
|---|---|
| Document inexistant | `404 DOCUMENT_NOT_FOUND` |
| Utilisateur sans permission | `403 Forbidden` |
| XML invalide ou document non XML | `422 INVALID_INVOICE_XML` |

---

## Tests

Deux types de tests sont présents :

```text
CiiInvoiceParserTest
InvoiceViewIntegrationTest
```

Ils couvrent notamment :

- XML CII valide
- plusieurs lignes
- champs manquants
- XML mal formé
- document inexistant
- PDF refusé
- contrôle des permissions

Lancer les tests F10 :

```bash
cd backend
./mvnw -Dtest=CiiInvoiceParserTest,InvoiceViewIntegrationTest test
```

---

## Résumé

F10 ajoute la lecture métier d'un XML CII :

```text
XML stocké
   ↓
permissions
   ↓
parser
   ↓
facture lisible
```

F10 est indépendant de F08/F09 :

```text
F08/F09 → validation de conformité
F10     → lecture des données de la facture
```
