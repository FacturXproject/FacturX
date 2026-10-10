# F17 - Public API (API key, rate limit, documentation)

Subject module: **Public API** (2 points) - "a public API to interact with the
database with a secured API key, rate limiting, documentation, and at least 5
endpoints (GET, POST, PUT, DELETE)".

Lets a technical client - typically an accounting software vendor - deposit invoices,
run the Factur-X conformity check and read the result **without a browser session**.

Interactive documentation (Swagger UI): <https://localhost:8443/api/public/docs>
OpenAPI description (JSON): <https://localhost:8443/api/public/v1/openapi>

---

## 1. How it works

```text
Client (curl, accounting software)
   │  X-API-Key: fxk_...
   ▼
nginx  /api/  →  Spring Boot
   ▼
PublicApiSecurityConfig      second security chain, /api/public/** only (@Order(1))
   ▼
ApiKeyAuthenticationFilter   1. key → SHA-256 → lookup in api_keys      (401 if unknown/revoked)
                             2. ApiRateLimiter, per key                 (429 beyond the limit)
                             3. scopes become authorities
   ▼
authorizeHttpRequests        GET needs documents:read, the rest documents:write (403)
   ▼
PublicApiController → PublicApiService
                             4. document must belong to the key's organization (404)
                             5. PermissionService: the owner's role must allow it (403)
   ▼
DocumentService / FacturXValidationService / ValidationReportService   (F06-F09, unchanged)
```

The public API adds **no business logic**: it is a second entrance to the existing
services, with its own authentication and its own access rules in front.

### Design decisions worth knowing for the defence

- **The key is never stored.** Only its SHA-256 hash is (`api_keys.key_hash`). The
  full key is returned once, in the response of its creation. A database leak does
  not leak usable keys. SHA-256 (not BCrypt) is the right tool here: a key is 256
  random bits, so there is nothing to brute-force - slow hashes protect *weak*
  secrets such as passwords.
- **A key = one user + one organization.** It acts with its owner's role in that
  organization and can never do more. No route takes an `organizationId`: the key
  already says which one.
- **The role is re-checked on every request.** If the owner is downgraded or removed
  from the organization, the key stops working at once, even if nobody revoked it.
- **Scopes only narrow.** `documents:read` / `documents:write` restrict a key further
  (e.g. a read-only key for a dashboard); they never grant what the role does not.
- **Another organization's document answers 404, not 403** - a key must not even
  learn that the document exists.
- **Separate security chain.** The session chain (`SecurityConfig`) is untouched. The
  public chain is stateless and has no CSRF protection: CSRF abuses a cookie the
  browser sends by itself, and there is no cookie here. Conversely, an API key does
  not open the session API, and a session cookie does not open the public API.
- **Rate limit in memory**, fixed one-minute window per key (same choice as
  `LoginAttemptService`: no Redis at this scale). Known limit: counters reset when
  the backend restarts.
- **Errors never leave through `/error`.** Spring answers plain HTTP errors (malformed
  id or JSON, unsupported method, unknown route, `ResponseStatusException`) with
  `sendError()`, which makes Tomcat re-dispatch the request to `/error`. That second
  dispatch is outside `/api/public/**`, so it falls into the session chain, where an
  API-key request is anonymous: the client would get a misleading `401` instead of
  the real `400`/`404`/`405`. `JsonErrorResponseWrapper` intercepts `sendError()` and
  writes the JSON error directly. MockMvc does not perform that re-dispatch, so only
  a test against the real stack shows the problem - which is how it was found.
- **`api_keys` has no foreign keys** (`user_id`, `organization_id` are plain ids, like
  `validation_runs.document_id`), so deleting an organization or a member behaves
  exactly as before F17.

---

## 2. Files

```text
backend/src/main/java/com/facturx/app/publicapi/
├── ApiKey.java                      JPA entity, table "api_keys"
├── ApiKeyScope.java                 documents:read / documents:write
├── ApiKeyRepository.java
├── ApiKeyService.java               create / list / revoke / authenticate (hashing)
├── ApiKeyController.java            /api/api-keys - key management (session)
├── ApiKeyCreateRequest.java
├── ApiKeyResponse.java              never contains the key or its hash
├── ApiKeyCreatedResponse.java       the only response carrying the full key
├── ApiKeyNotFoundException.java
├── ApiKeyExceptionHandler.java
├── ApiKeyPrincipal.java             the authenticated caller of a public request
├── ApiKeyAuthenticationFilter.java  X-API-Key → authentication + rate limit
├── JsonErrorResponseWrapper.java    sendError() → JSON body, no /error re-dispatch
├── ApiRateLimiter.java
├── PublicApiSecurityConfig.java     security chain for /api/public/**
├── PublicApiController.java         /api/public/v1/documents - the public endpoints
├── PublicApiService.java            organization + role checks, then existing services
├── PublicDocumentPage.java
├── DocumentUpdateRequest.java
└── OpenApiConfig.java               title, description, X-API-Key scheme for Swagger

frontend/src/pages/ApiKeys.jsx       page "Clés API" (/cles-api)
```

### Table `api_keys`

| Column | Type | Description |
|---|---|---|
| id | bigint | Primary key |
| user_id | bigint | Owner of the key |
| organization_id | bigint | The single organization the key works in |
| name | varchar(100) | Label chosen by the owner |
| key_hash | varchar(64), unique | SHA-256 of the key (hex) |
| key_prefix | varchar(16) | First 12 characters, shown in the list |
| scopes | varchar(100) | `documents:read,documents:write` |
| created_at | timestamp | |
| last_used_at | timestamp | Updated on each authenticated request |
| revoked_at | timestamp | Null while the key is active |

---

## 3. Key management (session + CSRF, used by the "Clés API" page)

| Method | Route | Description |
|---|---|---|
| GET | `/api/api-keys` | My keys (prefix only) |
| POST | `/api/api-keys` | Create a key - the full key is returned **here only** |
| DELETE | `/api/api-keys/{id}` | Revoke a key (soft delete, stays listed as revoked) |

```json
POST /api/api-keys
{ "name": "Logiciel comptable", "organizationId": 12, "scopes": ["documents:read", "documents:write"] }

201 Created
{
  "key": "fxk_3f9c...(64 hex chars)",
  "apiKey": { "id": 5, "name": "Logiciel comptable", "organizationId": 12,
              "organizationName": "Cabinet Dupont", "keyPrefix": "fxk_3f9c1a2b",
              "scopes": ["documents:read", "documents:write"],
              "createdAt": "2026-10-10T17:00:00Z", "lastUsedAt": null, "revokedAt": null }
}
```

Any member of an organization can create a key for it; the key is then limited to
that member's role.

---

## 4. Public endpoints (X-API-Key)

Base URL: `https://localhost:8443/api/public/v1`

| # | Method | Route | Scope | Role rule |
|---|---|---|---|---|
| 1 | GET | `/documents?page=0&size=20` | read | all documents (admin, accountant) or own only (client) |
| 2 | POST | `/documents` | write | `UPLOAD_DOCUMENT` |
| 3 | GET | `/documents/{id}` | read | status and metadata |
| 4 | PUT | `/documents/{id}` | write | rename: own document, or any for an admin |
| 5 | DELETE | `/documents/{id}` | write | same rules as the web app (`DocumentService`) |
| 6 | POST | `/documents/{id}/validate` | write | `VALIDATE_DOCUMENT` (admin, accountant) |
| 7 | GET | `/documents/{id}/report` | read | latest validation report (F09) |
| 8 | GET | `/documents/{id}/download` | read | the original file |

### curl examples

`-k` because the local certificate is self-signed.

```bash
KEY="fxk_..."                       # shown once when the key is created
API="https://localhost:8443/api/public/v1"

# 1. List documents
curl -k -H "X-API-Key: $KEY" "$API/documents?page=0&size=20"

# 2. Upload a document (PDF or XML, 10 MB max)
curl -k -H "X-API-Key: $KEY" -F "file=@facture.pdf" "$API/documents"

# 3. Read its status
curl -k -H "X-API-Key: $KEY" "$API/documents/42"

# 4. Rename it
curl -k -X PUT -H "X-API-Key: $KEY" -H "Content-Type: application/json" \
     -d '{"filename":"facture-2026-001.pdf"}' "$API/documents/42"

# 5. Run the Factur-X conformity check
curl -k -X POST -H "X-API-Key: $KEY" "$API/documents/42/validate"

# 6. Read the validation report
curl -k -H "X-API-Key: $KEY" "$API/documents/42/report"

# 7. Download the original file
curl -k -H "X-API-Key: $KEY" -o facture.pdf "$API/documents/42/download"

# 8. Delete it
curl -k -X DELETE -H "X-API-Key: $KEY" "$API/documents/42"
```

List response:

```json
{
  "items": [
    { "id": 42, "organizationId": 12, "ownerId": 3, "filename": "facture.pdf",
      "type": "application/pdf", "size": 48213, "status": "VALID",
      "uploadedAt": "2026-10-10T17:02:11Z" }
  ],
  "page": 0, "size": 20, "totalItems": 1, "totalPages": 1
}
```

`size` is capped at 100. `status` is one of `UPLOADED`, `QUEUED`, `PROCESSING`,
`VALID`, `INVALID`, `FAILED`.

---

## 5. Rate limit

60 requests per minute and per key by default
(`app.public-api.rate-limit.requests-per-minute`, or the `PUBLIC_API_RATE_LIMIT`
environment variable).

Every authenticated response carries:

| Header | Meaning |
|---|---|
| `X-RateLimit-Limit` | Requests allowed per minute |
| `X-RateLimit-Remaining` | Requests left in the current window |
| `X-RateLimit-Reset` | Seconds until the window resets |
| `Retry-After` | Only on a `429`: seconds to wait |

---

## 6. Errors

Always `{"error": "CODE", "message": "..."}`.

| Case | HTTP | Error |
|---|---|---|
| No `X-API-Key` header | 401 | `API_KEY_REQUIRED` |
| Unknown or revoked key, or disabled account | 401 | `INVALID_API_KEY` |
| Key lacks the scope for this method | 403 | `INSUFFICIENT_SCOPE` |
| Owner's role does not allow the action | 403 | `ACCESS_DENIED` |
| Document missing, or in another organization | 404 | `DOCUMENT_NOT_FOUND` |
| No validation run yet for this document | 404 | `NOT_FOUND` |
| Unknown route | 404 | `NOT_FOUND` |
| Invalid body (e.g. empty filename) | 400 | `VALIDATION_FAILED` |
| Malformed id, parameter or JSON, missing `file` part | 400 | `BAD_REQUEST` |
| HTTP method not supported on the route | 405 | `METHOD_NOT_ALLOWED` |
| Wrong `Content-Type` | 415 | `UNSUPPORTED_MEDIA_TYPE` |
| Unexpected server failure | 500 | `INTERNAL_ERROR` |
| File larger than 10 MB | 413 | `FILE_TOO_LARGE` |
| File is neither a PDF nor XML | 415 | `INVALID_FILE_TYPE` |
| Too many requests | 429 | `RATE_LIMIT_EXCEEDED` |

---

## 7. Tests

`PublicApiFlowTest` (JUnit + MockMvc + Testcontainers), 26 tests:

- **Key management** - the key is returned once and never listed; a session is
  required; no key for an organization you do not belong to; invalid name / scope
  rejected; nobody can revoke someone else's key.
- **Authentication** - valid key accepted; missing, unknown, revoked key refused;
  key of a disabled account refused; a session cookie is not an API key and an API
  key does not open the session API.
- **Scopes** - a read-only key cannot upload, rename, delete or validate; a
  write-only key cannot read.
- **Endpoints** - upload → list → status → rename → download → delete; validate →
  status `VALID` → report; wrong file type; invalid filename; pagination and size cap;
  malformed requests (bad id, bad JSON, missing file, wrong method, unknown route)
  answer a JSON error with the real status.
- **Isolation and roles** - a key never reaches another organization's documents
  (all 6 routes answer 404); a client's key sees only its own documents and cannot
  validate; a key stops working when its owner leaves the organization.
- **Rate limit** - the request after the limit gets `429` + `Retry-After`; another
  key is unaffected.
- **Documentation** - the OpenAPI description is public, lists the 8 operations and
  nothing outside `/api/public/v1`; Swagger UI is reachable without a key.

```bash
cd backend
./mvnw test -Dtest=PublicApiFlowTest
```

Frontend: `frontend/src/pages/__tests__/ApiKeys.test.jsx` (8 tests) - loading, empty
state, error, list with states, documentation link, creation (key shown once),
no creation without a scope, revocation after confirmation.

---

## 8. Not covered

- Only documents are exposed. Extraction drafts (F11) and generation (F13) are not
  part of the public API yet.
- A key has no expiry date: it lives until it is revoked.
- Validation runs synchronously in the request, as in the web app. Once the queue
  (F14) exists, `POST /documents/{id}/validate` is the natural place to enqueue a job
  instead - the `QUEUED` / `PROCESSING` statuses are already part of the contract.
