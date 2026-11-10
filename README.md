*This project has been created as part of the 42 curriculum by sabenzar, feturbin, avalent2, iso-brie, yseddiki.*

# FacturX

**A Factur-X compliance platform for accounting firms: upload invoices, validate them,
understand every error, and generate compliant Factur-X files.**

---

## Table of Contents

1. [Description](#description)
2. [Instructions](#instructions)
3. [Team Information](#team-information)
4. [Project Management](#project-management)
5. [Technical Stack](#technical-stack)
6. [Architecture](#architecture)
7. [Database Schema](#database-schema)
8. [Features List](#features-list)
9. [Modules](#modules)
10. [Individual Contributions](#individual-contributions)
11. [API Reference](#api-reference)
12. [Resources](#resources)
13. [Known Limitations](#known-limitations)

---

## Description

The French e-invoicing reform requires companies to exchange structured electronic
invoices. **Factur-X** is one of the accepted formats: a human-readable PDF/A-3 that
embeds a machine-readable XML invoice (CII, EN 16931).

FacturX helps accounting firms and their clients handle this format. A firm creates an
organization, invites its accountants and clients, and every member works on the
documents of that organization according to their role.

### Key features

- **Factur-X validation** — PDF/A-3, XSD and EN 16931 Schematron checks, with a
  readable report in French that explains each error and how to fix it.
- **Factur-X generation** — fields are extracted from a plain PDF invoice, checked and
  corrected by a human, then turned into a Factur-X file that is validated again.
- **Multi-tenant organizations** — invitations by email and three roles (admin,
  accountant, client) with fine-grained permissions.
- **Asynchronous processing** — validation and generation run as background jobs, and
  status changes reach every connected user in real time over WebSocket.
- **Secure public API** — API keys with scopes, rate limiting and OpenAPI documentation,
  so accounting software can integrate with the platform.
- **Data tools** — advanced document search, CSV / JSON / PDF export, CSV invitation
  import, and GDPR data export and account deletion.

---

## Instructions

### Prerequisites

- Docker and Docker Compose
- OpenSSL (to generate the local TLS certificate)
- A POSIX shell (Linux or macOS)

### Quick start

From the project root:

```bash
./start.sh
```

This single command:

1. creates `.env` from `.env.example` if it is missing;
2. generates a self-signed TLS certificate in `nginx/certs/` if it is missing;
3. builds and starts the whole stack with `docker compose up --build`.

Extra arguments are passed to Docker Compose, for example `./start.sh -d` to run in the
background.

The same steps by hand:

```bash
cp .env.example .env
./scripts/generate-certs.sh
docker compose up --build
```

### Access

| Service | URL |
|---|---|
| Application | <https://localhost:8443> |
| HTTP (redirects to HTTPS) | <http://localhost:8080> |
| Mailpit (emails sent by the app) | <https://localhost:8443/mailpit/> |
| Public API documentation (Swagger UI) | <https://localhost:8443/api/docs/swagger-ui/index.html> |
| Health check | <https://localhost:8443/api/healthcheck> |

The certificate is self-signed: the browser shows a warning on the first visit.

### Configuration

All configuration comes from `.env`. The file is ignored by git;
`.env.example` is the versioned template and contains no real secret.

| Variable | Purpose | Default in `.env.example` |
|---|---|---|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | Database credentials | placeholders, change them |
| `POSTGRES_DB` | Database name (must stay `facturx`) | `facturx` |
| `MAIL_HOST` / `MAIL_PORT` | SMTP server | `mailpit` / `1025` |
| `MAIL_FROM` | Sender of application emails | `noreply@facturx.local` |
| `APP_BASE_URL` | Public URL used in email links | `https://localhost:8443` |

The TLS certificates in `nginx/certs/` are also generated locally and never committed.

### Stop and reset

```bash
docker compose down        # stop the stack, keep the data
docker compose down -v     # stop the stack and delete the database and uploaded files
```

### Running the tests

```bash
cd backend && ./mvnw test                                # backend (needs Docker for Testcontainers)
cd frontend && npm ci && npx vitest run                  # frontend
cd extractor && pip install -r requirements-dev.txt && pytest   # Python service
```

End-to-end scripts run against a started stack:

```bash
./scripts/test-f13-generation.sh
./scripts/test-f16-search.sh
./scripts/test-f17-public-api.sh
./scripts/test-f20-export-import.sh
```

### Local development without Docker

- **Frontend:** `cd frontend && npm ci && npm run dev` — Vite on <http://localhost:5173>,
  proxying `/api` and `/ws` to the backend on `localhost:8080`.
- **Backend:** `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` —
  needs a reachable PostgreSQL. The `dev` profile allows the Vite origin and HTTP
  session cookies.

---

## Team Information

| Login | Role | Responsibilities |
|---|---|---|
| **sabenzar** | Project Manager (PM), Developer | Planning, meetings, progress tracking; infrastructure and platform features |
| **feturbin** | Architect, Developer | Overall architecture and technical decisions; Factur-X engine and public API |
| **avalent2** | Product Owner (PO), Developer | Product vision and feature priorities; collaboration and data features |
| **yseddiki** | Developer | Organizations, documents and search |
| **iso-brie** | Developer | Legal pages, profile and correction form |

All five team members are developers.

---

## Project Management

- **Architecture first.** We defined the architecture of the project, then split the
  product into features (F00 to F21) and built them step by step, **one feature per
  person**. The full plan is in [roadmap.md](./roadmap.md).
- **One branch per feature.** Each feature was developed in its own branch. A pull
  request could be merged into `main` only after **two independent reviews** and a
  green CI.
- **Regular meetings.** The team met once a week or once every two weeks.
- **Tools.** Work was organized in a **GitHub Organization** (repository, branches,
  pull requests, reviews), together with a custom feature-tracking tool built by the
  team: <https://42dashboard-three.vercel.app>.

---

## Technical Stack

| Layer | Technologies |
|---|---|
| Frontend | React 19, React Router, Vite, Tailwind CSS, Vitest |
| Backend | Java 21, Spring Boot 4 (Web MVC, Security, Data JPA / Hibernate, Session JDBC, WebSocket, Mail, Validation), Maven |
| Factur-X | Mustangproject (validation), Apache PDFBox, Apache Commons CSV |
| Python service | Python 3.12, FastAPI, pdfplumber (extraction), factur-x and ReportLab (generation) |
| Database | PostgreSQL 17 |
| Infrastructure | Nginx (HTTPS reverse proxy), Docker Compose, Mailpit, GitHub Actions |
| API documentation | springdoc-openapi (OpenAPI 3, Swagger UI) |

**Why these choices**

- **Spring Boot** — a mature framework for security (sessions, CSRF, BCrypt password
  hashing), transactions and WebSocket, and the Java ecosystem has the reference
  Factur-X library, Mustangproject.
- **Python microservice** — the best libraries for PDF text extraction (pdfplumber) and
  Factur-X generation (factur-x) are in Python. The service is internal only, and is
  reached by the backend alone.
- **PostgreSQL** — a relational model fits organizations, members, documents and
  validation results; it also stores HTTP sessions (Spring Session JDBC).
- **React + Vite** — a fast development loop and a component model that suits a
  dashboard-style application.
- **Nginx** — the single public entry point: TLS termination, routing and upload size
  limits.

---

## Architecture

Nginx is the only service published on the host. Every other service stays on the
internal Docker network `facturx-network`.

```text
Browser
   │  HTTP :8080 ──301──▶ HTTPS :8443
   ▼
Nginx (TLS)
   ├── /           ──▶ frontend    React + Vite        :5173
   ├── /api/       ──▶ backend     Spring Boot         :8080
   ├── /ws         ──▶ backend     WebSocket
   └── /mailpit/   ──▶ mailpit     email catcher UI    :8025

backend ──▶ postgres    PostgreSQL 17           :5432
        ──▶ extractor   FastAPI (internal)      :8000   /extract, /generate
        ──▶ mailpit     SMTP                    :1025
```

- The frontend talks only to the backend, through Nginx.
- The backend owns authentication, authorization, business logic, database access and
  the orchestration of the Python service.
- Validation and generation run as **jobs** (table `jobs`): the API answers immediately,
  a background worker processes the job, and status changes are pushed to clients over
  WebSocket.

### Project structure

```text
.
├── start.sh                one-command start
├── docker-compose.yml
├── .env.example            configuration template (.env is local only)
├── .github/workflows/      CI
├── nginx/                  reverse proxy configuration (certs/ generated locally)
├── frontend/               React application
├── backend/                Spring Boot application, organized by domain
│   └── src/main/java/com/facturx/app/
│       ├── auth/  user/  organization/  permission/
│       ├── document/  validation/  extraction/  generation/  invoice/
│       ├── job/  websocket/  apikey/  publicapi/  exportimport/  gdpr/
│       └── config/
├── extractor/              Python FastAPI service (extraction and generation)
└── scripts/                certificate generation and end-to-end tests
```

Backend code follows the same flow in every domain:
`Controller → Service → Repository → JPA / Hibernate → PostgreSQL`.

### Continuous integration

Every push and pull request runs the CI pipeline (`.github/workflows/ci.yml`):
backend tests, Python service tests, Docker Compose validation, image build, stack
startup, HTTPS health check, and the end-to-end Factur-X generation and public API
scripts.

---

## Database Schema

PostgreSQL 17, managed through JPA / Hibernate. HTTP sessions are stored in the
`spring_session` tables created by Spring Session JDBC.

```text
users ─────────────┐
  │                │
  │ 1..n           │ 1..n
  ▼                ▼
organization_members ◀── n..1 ── organizations ── 1..n ──▶ invitations
                                     │
                                     ├── 1..n ──▶ api_keys (owner_id → users)
                                     │
                                     └── 1..n ──▶ documents (owner_id → users)
                                                     │
                     ┌───────────────┬───────────────┼────────────────────┐
                     ▼               ▼               ▼                    ▼
                   jobs        validation_runs   draft_invoices    generated_invoices
                                     │               │
                                     ▼               ├──▶ extracted_fields
                             validation_errors       ├──▶ draft_lines
                                     │               └──▶ draft_taxes
                                     ▼
                              rule_catalog (by rule_code)
```

| Table | Main columns | Purpose |
|---|---|---|
| `users` | id, email, password_hash, first_name, last_name, status, created_at, updated_at | Accounts; passwords hashed with BCrypt; status `active` or `anonymized` |
| `organizations` | id, name, created_at | Accounting firms (tenants) |
| `organization_members` | id, user_id, organization_id, role, joined_at | Membership with role `ADMIN`, `ACCOUNTANT` or `CLIENT` |
| `invitations` | id, organization_id, email, role, status, token, expires_at, created_at | Email invitations into an organization |
| `documents` | id, organization_id, owner_id, filename, type, size, status, storage_path, uploaded_at | Uploaded and generated files; status `UPLOADED` → `QUEUED` → `PROCESSING` → `VALID` / `INVALID` / `FAILED` |
| `jobs` | id, type, document_id, status, attempts, requested_by, profile, error, started_at, finished_at | Background jobs (`VALIDATION`, `GENERATION`) |
| `validation_runs` | id, document_id, requested_by, filename, valid, layer_reached, started_at, finished_at | One Factur-X validation |
| `validation_errors` | id, run_id, layer, severity, rule_code, message, field, actual_value, expected_value | Errors found by a validation run |
| `rule_catalog` | code, layer, raw_text, title_fr, description_fr, correction_hint_fr | French explanation of each validation rule |
| `draft_invoices` | id, document_id, organization_id, source, created_at, updated_at | Invoice data extracted from a PDF, before generation |
| `extracted_fields` | id, draft_invoice_id, field_name, value, confidence, source | Header fields of a draft (number, dates, parties, totals) |
| `draft_lines` | id, draft_invoice_id, position, description, quantity, unit_price, total, vat_rate | Invoice lines of a draft |
| `draft_taxes` | id, draft_invoice_id, position, rate, taxable_amount, tax_amount | VAT breakdown of a draft |
| `generated_invoices` | id, source_document_id, generated_document_id, draft_invoice_id, organization_id, profile, status, validation_run_id, created_by | Result of a Factur-X generation |
| `api_keys` | id, organization_id, owner_id, name, prefix, key_hash, scopes, created_at, last_used_at, revoked_at | Public API keys; only the SHA-256 hash is stored |

---

## Features List

| ID | Feature | Description | Owner |
|---|---|---|---|
| F00 | Technical baseline | Docker Compose stack, CI, health check end to end | sabenzar |
| F00+ | HTTPS / Nginx / TLS | Single HTTPS entry point, HTTP redirect, environment configuration | sabenzar |
| F01 | Authentication | Registration, login, server-side sessions, hashed passwords, login rate limit | feturbin |
| F02 | Organizations | Create, rename and delete organizations; data isolated per organization | yseddiki |
| F03 | Invitations and members | Invite by email, accept, resend, revoke; member management | avalent2 |
| F04 | Roles and permissions | Admin, accountant and client roles enforced on every action | sabenzar |
| F05 | Legal pages and profile | Privacy policy, terms of service, profile page | iso-brie |
| F06 | Document upload | PDF / XML / Factur-X upload, type and size checks (10 MB) | yseddiki |
| F07 | Document list and statuses | Document history, statuses and details | avalent2 |
| F08 | Factur-X validation | Multi-layer validation engine (Java, Mustangproject) | feturbin |
| F09 | Readable error report | Each rule explained in French, with a correction hint | feturbin |
| F09+ | "Verify" page | Validate a file directly from a dedicated page | feturbin |
| F10 | Invoice XML reader | Displays the embedded XML as a readable invoice | sabenzar |
| F11 | PDF extraction | Python service extracts invoice fields from a plain PDF | feturbin |
| F12 | Correction form | The user checks and corrects extracted fields | iso-brie |
| F13 | Factur-X generation | Python generates the Factur-X, Java validates it independently | yseddiki, feturbin |
| F14 | Queue and async processing | Background jobs with statuses, limited retries, one active job per document | sabenzar |
| F15 | Real-time | Live document statuses over WebSocket, per organization and role | avalent2 |
| F16 | Advanced search | Filters, sorting and pagination of documents | yseddiki |
| F17 | Secure public API | API keys, scopes, rate limiting, OpenAPI documentation | feturbin |
| F20 | Export / import | Documents and members export (CSV / JSON / PDF), CSV invitation import | avalent2 |
| F21 | GDPR | Personal data export, account deletion or anonymization, confirmation emails | sabenzar |

---

## Modules

| Module | Type | Pts | Features | Owner(s) |
|---|---|---|---|---|
| Frontend + backend frameworks (React, Spring Boot) | Major | 2 | F00 | sabenzar |
| ORM (Spring Data JPA / Hibernate) | Minor | 1 | F00 | sabenzar |
| Backend as microservices (internal Python service) | Major | 2 | F00, F11, F13 | sabenzar, feturbin, yseddiki |
| Organization system | Major | 2 | F02, F03 | yseddiki, avalent2 |
| Advanced permissions | Major | 2 | F04 | sabenzar |
| File upload and management | Minor | 1 | F06 | yseddiki |
| **Custom module: Factur-X compliance engine** | Major | 2 | F08, F09 | feturbin |
| Real-time features (WebSocket) | Major | 2 | F15 | avalent2 |
| **Subtotal (minimum required: 14)** | | **14** | | |
| Public API | Major | 2 | F17 | feturbin |
| Advanced search | Minor | 1 | F16 | yseddiki |
| Data export / import | Minor | 1 | F20 | avalent2 |
| GDPR compliance | Minor | 1 | F21 | sabenzar |
| **Total** | | **19** | | |

### Custom module (Major): Factur-X compliance engine

**Why this module.** Accounting firms must now know *whether* an invoice is a compliant
Factur-X and, above all, *why* it is not. No standard module of the subject covers this
need: it is the business core of the application.

**What it does (F08 + F09).**

- **Deterministic multi-layer validation** with the Mustangproject library (which
  relies on veraPDF for PDF/A): PDF/A-3 structure and embedded XML, CII XSD schema,
  EN 16931 Schematron business rules and Factur-X profile constraints. The layer
  reached is stored with each run.
- **Readable report**: each technical rule code (`BR-02`, …) is translated into a
  title, an explanation and a correction hint in French, the language of our users.
  These texts are data (table `rule_catalog`), not code.
- **Persistence** of every run and its errors (`validation_runs`, `validation_errors`),
  available from the document page and the "Verify" page.

**Technical challenges.**

- Interpreting Mustang's raw XML report (separate PDF and XML blocks) and mapping it to
  a single model: layer reached, severity, rule, field, value.
- Avoiding noise: every error is kept in the database, but rules outside our users'
  scope (`BR-DE-*` extensions, `PEPPOL-*` notices) are left out of the readable report.
- Testing precise rules: invoices deliberately broken on a single point check that the
  engine raises exactly the expected rule (`RuleCatalogCoverageTest`).
- Keeping the generator and the validator independent: invoices produced by the Python
  service (F13) are validated again by the Java engine, and CI fails if a generated
  invoice is not compliant.

**Why a Major.** The module is comparable in size and complexity to the subject's Major
modules: a complete backend service, three dedicated tables, a REST API, frontend pages,
its own test coverage, and integration with almost every other part of the project —
document upload (F06), queue (F14), real-time (F15), generation (F13) and the public
API (F17).

---

## Individual Contributions

- **sabenzar** (PM) — technical baseline (Docker, CI, health check), HTTPS / Nginx / TLS,
  roles and permissions, invoice XML reader, queue and asynchronous processing, GDPR.
- **feturbin** (Architect) — authentication, Factur-X validation engine and readable
  report, "Verify" page, PDF extraction in Python, Factur-X generation (with yseddiki),
  public API, bug fixes (BUGS_FIX_1, BUGS_FIX_2).
- **avalent2** (PO) — invitations and members, document list and statuses, real-time
  WebSocket, export / import.
- **yseddiki** — organizations, document upload, Factur-X generation (with feturbin),
  advanced search.
- **iso-brie** — legal pages and profile, human correction form.

---

## API Reference

The frontend uses session routes under `/api/` (session cookie + CSRF token). External
software uses the public API under `/api/public/v1` (API key). The main session routes:

| Area | Routes |
|---|---|
| Authentication | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me` |
| Organizations | `GET`, `POST /api/organizations` · `GET`, `PUT`, `DELETE /api/organizations/{id}` · `GET /api/organizations/{id}/members` · `PATCH /api/organizations/{id}/members/{userId}/role` · `DELETE /api/organizations/{id}/members/{userId}` |
| Invitations | `GET`, `POST /api/organizations/{orgId}/invitations` · `PATCH .../{invitationId}/revoke`, `.../resend` · `GET /api/invitations/{token}` · `POST /api/invitations/accept` |
| Documents | `POST /api/documents` · `GET /api/documents`, `/api/documents/{id}`, `/api/documents/search` · `GET /api/documents/{id}/download` · `DELETE /api/documents/{id}` |
| Validation | `POST /api/documents/{id}/validate` · `GET /api/documents/{id}/report` · `POST /api/validate` · `GET /api/validate/runs/{runId}/report` |
| Extraction and generation | `POST /api/documents/{id}/extract` · `GET`, `PUT /api/documents/{id}/draft` · `POST /api/documents/{id}/generate` · `GET /api/documents/{id}/generations` |
| Invoice view | `GET /api/documents/{id}/invoice-view`, `/api/documents/{id}/xml` |
| Jobs | `POST /api/jobs/validation`, `/api/jobs/generation` · `GET /api/jobs/{id}` · `POST /api/jobs/{id}/retry` |
| API keys | `GET`, `POST /api/api-keys` · `DELETE /api/api-keys/{id}` |
| Export / import | `GET /api/organizations/{orgId}/export/documents`, `.../export/members` · `POST /api/organizations/{orgId}/import/invitations` |
| GDPR | `GET /api/gdpr/export` · `POST /api/gdpr/delete` |
| Health | `GET /api/healthcheck` |

### Real-time events (F15)

#### Connection and authentication

Endpoint: `/ws` (`wss://localhost:8443/ws` over HTTPS).
The handshake uses the `EFACTURE_SESSION` session cookie.
An authenticated session is required. Allowed origins are defined by
`app.allowed-origins`, shared between CORS and WebSocket.

#### Event format

```json
{
  "type": "document.status.changed",
  "documentId": 100,
  "organizationId": 1,
  "status": "VALID"
}
```

`status` is the new status of the document:
`UPLOADED`, `QUEUED`, `PROCESSING`, `VALID`, `INVALID` or `FAILED`.
`ownerId` stays internal to the server and is not sent.

#### Visibility

Permissions are checked on the server for every event, for the document's
organization.

| Permission | Events received |
| --- | --- |
| VIEW_ALL_DOCUMENTS | All documents of the organization |
| VIEW_OWN_DOCUMENTS only | Documents owned by the user |
| None of these permissions | None |

The ADMIN, ACCOUNTANT and CLIENT roles follow the permissions assigned to them.
No event from another organization is sent without the matching rights.

#### Logout and reconnection

On logout, the server closes all of the user's WebSockets, including on their other
devices, with code 1008. The client also closes its connection and cancels
reconnection attempts once the user is no longer authenticated.

After a connection drop, the client retries after 2 seconds.
It does not retry after code 1008.
A restored connection emits `websocket.reconnected` locally, used to reload the
current data.
A new login opens a new connection without a page refresh.

#### Vite development

The Spring `dev` profile allows `http://localhost:5173` and disables the Secure flag of
the session cookie for HTTP development.
Vite forwards `/api` and `/ws` to the backend on `localhost:8080`, with `ws: true` for
`/ws`.
The backend must be able to reach PostgreSQL.

### Public API (F17)

REST API for accounting software: upload invoices, track their status, start
validation, read the report and download the files.

#### API keys

A key is created from the **API keys** page of the application ("Clés API",
`/cles-api`).

- A key belongs to a user and acts in **one** organization the user is a member of. It
  never has more rights than the user: the permissions of the user's role (F04) apply,
  restricted by the key's scopes.
- The secret (`fxk_` + 43 characters) is shown **only once**, at creation. Only its
  SHA-256 hash is stored (`api_keys.key_hash`).
- A revoked key, or a key whose owner has left the organization or has been
  deactivated, is rejected immediately (401).

| Scope | Allows |
| --- | --- |
| `DOCUMENTS_READ` | list, status, report, download |
| `DOCUMENTS_WRITE` | upload, rename, delete |
| `DOCUMENTS_VALIDATE` | start a Factur-X validation |

Management (session + CSRF, used by the frontend):
`GET /api/api-keys`, `POST /api/api-keys`, `DELETE /api/api-keys/{id}` (revocation).

#### Authentication and rate limiting

The key is sent in the `X-API-Key` header (or `Authorization: Bearer <key>`).
The session cookie is **not** accepted on `/api/public/**`: the API is stateless,
hence no CSRF on this security chain.

Limit: **60 requests per minute per key** (`app.public-api.rate-limit.*`, can be
overridden with `PUBLIC_API_RATE_LIMIT`). Every response carries `X-RateLimit-Limit`,
`X-RateLimit-Remaining` and `X-RateLimit-Reset`; beyond the limit, HTTP `429` with
`Retry-After`. The counter is in memory, like the F01 login limit (a single backend
instance).

#### Endpoints

Base: `https://localhost:8443/api/public/v1`. The organization is the key's
organization; no `organizationId` is needed.

| Method | Route | Scope | Response |
| --- | --- | --- | --- |
| `GET` | `/documents?page=0&size=20` | READ | page of documents (`size` ≤ 100) |
| `POST` | `/documents` (multipart `file`) | WRITE | `201` document created |
| `GET` | `/documents/{id}` | READ | metadata and status |
| `PUT` | `/documents/{id}` (JSON `{"filename": "..."}`) | WRITE | document renamed (owner or admin) |
| `DELETE` | `/documents/{id}` | WRITE | `204` document deleted (same rules as the application) |
| `GET` | `/documents/{id}/report` | READ | F09 report of the latest validation |
| `GET` | `/documents/{id}/download` | READ | the file |
| `POST` | `/documents/{id}/validate` | VALIDATE | `202` validation job (F14) |

A document from another organization returns `404`, like an unknown document.

#### curl examples

```bash
KEY=fxk_...                                  # copied from the API keys page
API=https://localhost:8443/api/public/v1

# Upload an invoice
curl -k -H "X-API-Key: $KEY" -F "file=@invoice.pdf" $API/documents

# List documents
curl -k -H "X-API-Key: $KEY" "$API/documents?page=0&size=20"

# Start validation, then follow the status
curl -k -X POST -H "X-API-Key: $KEY" $API/documents/42/validate
curl -k -H "X-API-Key: $KEY" $API/documents/42

# Read the compliance report
curl -k -H "X-API-Key: $KEY" $API/documents/42/report

# Download the file
curl -k -H "X-API-Key: $KEY" -OJ $API/documents/42/download
```

#### Errors

Always in the format `{"error": "CODE", "message": "..."}`.

| HTTP | `error` |
| --- | --- |
| 401 | `API_KEY_MISSING`, `INVALID_API_KEY` |
| 403 | `INSUFFICIENT_SCOPE` (scope), `ACCESS_DENIED` (owner's role) |
| 404 | `DOCUMENT_NOT_FOUND`, `NOT_FOUND` (no validation yet) |
| 409 | `JOB_CONFLICT` (processing already in progress) |
| 413 / 415 | `FILE_TOO_LARGE` / `INVALID_FILE_TYPE` |
| 429 | `RATE_LIMITED` |

#### OpenAPI documentation

- Swagger UI: `https://localhost:8443/api/docs/swagger-ui/index.html`
- OpenAPI 3 specification: `https://localhost:8443/api/docs/openapi`

Public and read-only; only the `/api/public/**` routes are listed.
**Authorize** button: paste the key to try the endpoints.

### Export / import (F20)

**Export / import** page of an organization (`/organisations/{id}/export-import`),
reachable from the organization page ("Export / import" button).
Session routes (cookie + CSRF), like the rest of the application.

#### Rights

| Action | Admin | Accountant | Client |
| --- | --- | --- | --- |
| Export documents (CSV, JSON, PDF) | yes | yes | no |
| Export members (CSV, JSON) | yes | no | no |
| Import invitations (CSV) | yes | no | no |

A user who is not a member of the organization gets `403`, like a client.
An export never contains data from another organization.

#### Export

| Method | Route | Formats |
| --- | --- | --- |
| `GET` | `/api/organizations/{orgId}/export/documents?format=csv` | `csv`, `json` (default), `pdf` |
| `GET` | `/api/organizations/{orgId}/export/members?format=csv` | `csv`, `json` (default) |

The file is sent as an attachment (`documents.csv`, `members.json`, ...).

**Documents**: one row per document, newest first. The compliance, errors and rules
columns come from the latest validation (F08 / F09).

The CSV headers are in French, the language of the application; the English meaning is
given in the last column.

| CSV column | JSON field | Content |
| --- | --- | --- |
| `Identifiant` | `id` | document ID |
| `Organisation` | `organizationName` | organization name |
| `Déposé par` | `uploadedBy` | first and last name of the uploader |
| `Nom du fichier` | `filename` | file name |
| `Type` | `type` | `PDF` or `XML` |
| `Taille (MB)` | `sizeMb` | size, rounded to 2 decimals |
| `Statut` | `status` | `UPLOADED`, `QUEUED`, `PROCESSING`, `VALID`, `INVALID`, `FAILED` |
| `Date de dépôt` | `uploadedAt` | upload date, `dd/mm/yyyy hh:mm`, Paris time |
| `Adresse e-mail` | `email` | email of the uploader |
| `Conforme` | `valid` | compliant: `Oui` (yes), `Non` (no) or `Non validé` (not validated) |
| `Nombre d’erreurs` | `errorCount` | number of errors, empty if never validated |
| `Codes des règles` | `ruleCodes` | rule codes; CSV: separated by `;`, JSON: list |

**Members**: `Nom complet` (full name), `Adresse e-mail` (email), `Rôle` (role)
(JSON: `name`, `email`, `role`).

CSV: UTF-8 with BOM (Excel displays accents correctly), `,` separator, every value in
quotes. A value starting with `=`, `+`, `-` or `@` is prefixed with `'` so that a
spreadsheet does not execute it as a formula.

PDF: A4 landscape table, paginated, with the same information (without the ID, the
organization and the email), for printing or archiving. Characters outside the Latin
alphabet (Cyrillic, emoji, ...) appear as `?` in it. The CSV and JSON keep them.

#### Invitation import

`POST /api/organizations/{orgId}/import/invitations?dryRun=true`, multipart, field
`file`.

File format:

```csv
email,role
firstname.lastname@example.com,CLIENT
accountant@example.com,ACCOUNTANT
```

- Required columns `email` and `role` (upper or lower case), in any order; extra
  columns are ignored. The member export headers (`Adresse e-mail`, `Rôle`) are also
  accepted: an export can be imported again into another organization.
- Roles: `ADMIN`, `ACCOUNTANT`, `CLIENT`, or the French labels `Administrateur`,
  `Comptable`, `Client`.
- Separator `,` or `;` (detected on the first line), UTF-8, with or without BOM.
- At most 500 data rows and 10 MB.

Each row is checked like a manual invitation (F03): valid email, no duplicate in the
file, not already a member, no invitation already pending.

Processing happens in two steps:

1. `dryRun=true` (default): **preview**, nothing is created. The response lists each
   row: `{"lineNumber": 2, "email": "...", "role": "CLIENT", "valid": true, "error": null}`.
2. `dryRun=false`: **import**, all or nothing. If a single row has an error, the
   response is `400` with the same list and **no** invitation is created. Otherwise the
   invitations are created in a single transaction (`200`) and the emails are sent only
   once this transaction is committed. A cancelled import therefore never sends a link
   to an invitation that does not exist.

The application page runs the preview as soon as the file is selected. It enables the
send button only if every row is valid.

#### Errors

Format `{"error": "CODE", "message": "..."}`.

| HTTP | `error` | Case |
| --- | --- | --- |
| 400 | `INVALID_EXPORT_FORMAT` | unknown format (`xlsx`, `pdf` for members, ...) |
| 400 | `INVALID_IMPORT_FILE` | missing or empty file, missing or duplicate columns, malformed CSV, more than 500 rows |
| 400 | *(list of rows)* | import with at least one invalid row |
| 403 | `ACCESS_DENIED` | insufficient role or not a member |
| 409 | `INVITATION_ALREADY_PENDING`, `USER_ALREADY_MEMBER` | conflict that appeared between the preview and the import; nothing is created |
| 413 | `FILE_TOO_LARGE` | file larger than 10 MB |

Tests: `backend/.../exportimport/ExportImportFlowTest.java` (columns, isolation, rights,
invalid import rejected without creating anything), `frontend/.../ExportImportPage.test.jsx`.

### GDPR (F21)

The **My data** page ("Mes données", `/my-data`, linked from the profile) lets users export their personal data
(`GET /api/gdpr/export`) and delete their account (`POST /api/gdpr/delete`).

- The JSON export contains the profile, organization memberships with roles, owned
  documents (metadata only), API keys (never the secret) and invitations sent to the
  user's email.
- Deletion requires typing `SUPPRIMER` ("DELETE"); the backend checks the same word
  (`400 INVALID_CONFIRMATION` otherwise). The last admin of an organization cannot
  delete their account (`409 LAST_ADMIN_REQUIRED`).
- A user without documents is deleted. A user with documents is **anonymized** (email,
  name and password replaced, status `anonymized`) so that documents keep their owner
  reference. Memberships and invitations are removed in both cases, and every session
  of the user is invalidated.
- Both operations send a confirmation email.

Details: [backend/.../gdpr/README.md](./backend/src/main/java/com/facturx/app/gdpr/README.md).

---

## Resources

### Documentation

1. MDN Web Docs — <https://developer.mozilla.org>
2. DevDocs — <https://devdocs.io>
3. W3Schools — <https://www.w3schools.com>
4. Oracle Java Documentation — <https://docs.oracle.com/en/java/>
5. Python Documentation — <https://docs.python.org/3/>
6. JavaScript.info — <https://javascript.info>
7. Mustangproject (Factur-X / ZUGFeRD library used by the validation engine) —
   <https://github.com/ZUGFeRD/mustangproject>

### AI usage

AI was used as an assistant for:

- writing tests;
- finding bugs;
- learning the project languages (Java, JavaScript, Python);
- preparing documentation.

---

## Known Limitations

- The user interface and the validation explanations are in French, the language of
  the target users.
- The rate limiters (login, public API) keep their counters in memory: they assume a
  single backend instance.
- The PDF export shows characters outside the Latin alphabet as `?`; CSV and JSON
  keep them.
- After a physical account deletion, `generated_invoices.created_by` can point to a
  user ID that no longer exists (plain number, no foreign key).
- The TLS certificate is self-signed and meant for local use only.
