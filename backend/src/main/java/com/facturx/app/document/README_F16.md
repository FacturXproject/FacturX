# F16 — Recherche avancée

`GET /api/documents/search` — retrouver une facture par nom, statut, date, type,
déposant ou erreur de conformité, dans une organisation.

## Paramètres

| Paramètre        | Exemple                  | Effet |
|------------------|--------------------------|-------|
| `organizationId` | `5`                      | **Obligatoire.** Organisation dans laquelle chercher. |
| `q`              | `acme`                   | Sous-chaîne du nom de fichier, insensible à la casse. `%` et `_` sont pris littéralement. |
| `status`         | `INVALID,FAILED`         | Un ou plusieurs statuts (OU) : `UPLOADED`, `QUEUED`, `PROCESSING`, `VALID`, `INVALID`, `FAILED`. Aussi accepté en répété : `status=A&status=B`. |
| `type`           | `PDF`                    | `PDF` ou `XML` (plusieurs possibles). `XML` couvre `application/xml` et `text/xml`. |
| `ownerId`        | `12`                     | Documents déposés par cet utilisateur. |
| `from` / `to`    | `2026-03-01`             | Date de dépôt, format `AAAA-MM-JJ`, **bornes incluses**, jour calendaire à l'heure de Paris. |
| `error`          | `BR-02`, `numéro`        | Sous-chaîne du code de règle, du message Mustang ou du titre français (`rule_catalog.title_fr`) d'une erreur du **dernier** contrôle du document. Les familles `BR-DE-*` / `PEPPOL-*`, masquées du rapport F09, sont ignorées. |
| `sort`           | `filename,asc`           | `uploadedAt`, `filename`, `status`, `type` ou `size`, suivi de `,asc` / `,desc` (défaut `asc` si omis). Par défaut : `uploadedAt,desc`. |
| `page`           | `0`                      | Numéro de page, à partir de 0. |
| `size`           | `20`                     | Taille de page, 1 à 100 (défaut 20). |

Les filtres se combinent en **ET** ; les valeurs d'un même filtre en **OU**.

## Réponse

```json
{
  "content": [ { "id": 42, "organizationId": 5, "organizationName": "Cabinet A",
                 "ownerId": 12, "ownerName": "Jean Dupont", "filename": "facture-001.pdf",
                 "type": "application/pdf", "size": 20480, "status": "INVALID",
                 "uploadedAt": "2026-03-15T10:00:00Z" } ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1, "sort": "uploadedAt,desc"
}
```

## Erreurs

| Cas | HTTP | Corps |
|-----|------|-------|
| Filtre invalide (statut inconnu, date mal formée, `from` > `to`, tri non autorisé, `size` hors 1..100, texte > 255 caractères…) | 400 | `{"error":"INVALID_SEARCH_FILTER","field":"status","message":"Statut inconnu : …"}` |
| Pas membre de l'organisation (ou organisation inexistante) | 403 | `ACCESS_DENIED` |
| Client qui filtre sur le `ownerId` d'un autre | 403 | `ACCESS_DENIED` |
| Non connecté | 401 | |

## Isolation et rôles

- La requête porte **toujours** `organization_id = :organizationId` : aucun document
  d'une autre organisation ne peut remonter.
- Admin / comptable (`VIEW_ALL_DOCUMENTS`) : tous les documents de l'organisation,
  filtre `ownerId` libre.
- Client (`VIEW_OWN_DOCUMENTS` seulement) : restreint d'office à ses propres dépôts.

## Pagination stable

Pagination par offset, sur un ordre total : la colonne de tri demandée **puis l'id**.
Deux documents avec la même date / le même statut ne changent donc jamais de place
entre deux pages (testé dans `DocumentSearchTest.paginationIsStableWhenSortKeysTie`).

## Index

Déclarés sur les entités (`@Table(indexes = …)`, créés par Hibernate au démarrage) :

- `documents` : `(organization_id, uploaded_at)`, `(organization_id, status)`,
  `(organization_id, type)`, `(organization_id, filename)`, `(organization_id, owner_id)`
- `validation_runs(document_id)`, `validation_errors(run_id)` — pour le filtre `error`

Et dans `data.sql` (non exprimable en JPA) : index trigramme GIN `pg_trgm` sur
`lower(filename)`, qui sert la recherche `LIKE '%…%'` du paramètre `q`.

## Code

| Fichier | Rôle |
|---------|------|
| `DocumentSearchController` | Endpoint, paramètres reçus en chaînes brutes |
| `DocumentSearchService` | Validation des paramètres, périmètre selon le rôle, tri/pagination |
| `DocumentSpecifications` | Traduction des critères en prédicat JPA (Criteria API) |
| `DocumentSearchCriteria` / `DocumentSearchResponse` | Critères validés / page de résultats |
| Front : `frontend/src/pages/Documents.jsx` | Page `/documents`, état de la recherche dans l'URL |
