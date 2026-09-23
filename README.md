# FacturX App

FacturX is a full-stack web application built with a React frontend, a Spring Boot backend, PostgreSQL, Nginx, Docker Compose, and GitHub Actions.

The application follows an incremental architecture. Spring Boot is the main API exposed to the frontend, while specialized Python processing can later be delegated internally to a FastAPI service when required.

---

# Tech Stack

## Frontend

- React
- Vite
- JavaScript
- Tailwind CSS

## Backend

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Security
- Spring Session JDBC
- Maven

## Database

- PostgreSQL

## Infrastructure

- Nginx
- Docker
- Docker Compose
- GitHub Actions

## Internal Python Service

A FastAPI service can be introduced for features requiring specialized Python processing, such as:

- PDF extraction
- Factur-X generation

FastAPI remains internal. The frontend communicates only with Spring Boot.

---

# Architecture

Nginx is the single public entry point of the Docker stack and terminates HTTPS.

The browser does not communicate directly with Vite, Spring Boot, PostgreSQL, or the internal FastAPI service.

```text
Browser
   ↓
HTTP :8080
   ↓
Nginx
   ↓
301 redirect
   ↓
HTTPS :8443
   ↓
Nginx
   ├── /      ─────────────→ Frontend
   │                         React + Vite
   │                         Port 5173
   │
   └── /api/* ─────────────→ Backend
                             Spring Boot
                             Port 8080
                                ↓
                                ├── PostgreSQL
                                │   Port 5432
                                │
                                └── FastAPI
                                    Internal service
```

Only Nginx publishes ports to the host.

The host-to-container mappings are:

```text
Host :8080 → Nginx container :80
Host :8443 → Nginx container :443
```

The frontend, backend, PostgreSQL, and FastAPI remain on the internal Docker network.

---

# Communication Flow

For standard application features:

```text
React
   ↓
Nginx
   ↓
Spring Boot
   ↓
Service
   ↓
Repository
   ↓
JPA / Hibernate
   ↓
PostgreSQL
```

For features requiring Python processing:

```text
React
   ↓
Nginx
   ↓
Spring Boot
   ↓
FastAPI
   ↓
Spring Boot
   ↓
PostgreSQL if persistence is required
   ↓
Response to React
```

Spring Boot is responsible for:

- the public API;
- authentication;
- authorization;
- business logic;
- database access;
- orchestration of internal services.

---

# Incremental Architecture

The initial baseline is:

```text
React
   ↓
Spring Boot
   ↓
PostgreSQL
```

When Python-specific features are required, the architecture becomes:

```text
React
   ↓
Spring Boot
   ↓
   ├── PostgreSQL
   │
   └── FastAPI
```

---

# Project Structure

Current baseline:

```text
FacturX/
│
├── README.md
├── docker-compose.yml
├── .env
├── .env.example
├── .gitignore
│
├── .github/
│   └── workflows/
│       └── ci.yml
│
├── nginx/
│   ├── Dockerfile
│   ├── conf/
│   │   └── nginx.conf
│   └── certs/
│       ├── localhost.crt
│       └── localhost.key
│
├── scripts/
│   └── generate-certs.sh
│
├── frontend/
│   ├── Dockerfile
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│
├── backend/
│   ├── README.md
│   ├── Dockerfile
│   ├── pom.xml
│   ├── mvnw
│   └── src/
│
└── database/
```

For detailed backend architecture and development instructions, see:

[Backend README](./backend/README.md)

---

# Quick Start With Docker

From the project root, create the local environment file if needed:

```bash
cp .env.example .env
```

Generate the local TLS certificates if they are not already present:

```bash
./scripts/generate-certs.sh
```

Start the full stack:

```bash
docker compose up --build
```

The current baseline starts:

```text
nginx
frontend
backend
postgres
```

The HTTPS application entry point is:

```text
https://localhost:8443
```

The HTTP entry point is:

```text
http://localhost:8080
```

HTTP redirects to HTTPS:

```text
http://localhost:8080
        ↓
https://localhost:8443
```

---

# Health Check

The backend health endpoint is:

```http
GET /api/healthcheck
```

Through the full Docker stack:

```bash
curl -k https://localhost:8443/api/healthcheck
```

Current response:

```text
Yess i'm working
```

---

# PostgreSQL Healthcheck

Docker Compose checks PostgreSQL availability using:

```text
pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}
```

The healthcheck uses the PostgreSQL user and database defined in the environment configuration.

---

# Docker Network

The current Docker network is:

```text
facturx-network
│
├── nginx
│   ├── host :8080 → container :80
│   └── host :8443 → container :443
│
├── frontend
│   └── internal port 5173
│
├── backend
│   └── internal port 8080
│
└── postgres
    └── internal port 5432
```

Only Nginx is published to the host.

The backend reaches PostgreSQL using the Docker service name:

```text
postgres
```

Each Docker container has its own network namespace, so services communicate through Docker service names.

---

# Environment Variables

The project uses `.env` for local values and `.env.example` as the reference template.

Example:

```env
POSTGRES_DB=facturx
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
```

For the current development branch, `.env` is kept available to simplify development across team machines.

Before the final project submission:

```text
.env          → ignored
.env.example  → versioned
```

---

# Local Development

The frontend and backend can also be developed independently without running the full Docker Compose stack.

## Frontend

From:

```bash
cd frontend
```

Install dependencies:

```bash
npm ci
```

Start Vite:

```bash
npm run dev
```

The standalone frontend runs on:

```text
http://localhost:5173
```

In standalone development, Vite proxies `/api/*` requests to the local Spring Boot backend:

```text
http://localhost:8080
```

The frontend uses relative API paths such as:

```text
/api/users
/api/healthcheck
/api/auth/login
```

## Backend

From:

```bash
cd backend
```

Start Spring Boot:

```bash
./mvnw spring-boot:run
```

The standalone backend runs on:

```text
http://localhost:8080
```

Test it directly:

```bash
curl http://localhost:8080/api/healthcheck
```

Expected response:

```text
Yess i'm working
```

---

# Current API

## Health Check

```http
GET /api/healthcheck
```

Public endpoint.

Docker stack:

```bash
curl -k https://localhost:8443/api/healthcheck
```

Standalone backend:

```bash
curl http://localhost:8080/api/healthcheck
```

Current response:

```text
Yess i'm working
```

---

## Register User

```http
POST /api/auth/register
```

Public endpoint.

Example request body:

```json
{
  "email": "john@test.com",
  "password": "strongpassword",
  "firstName": "John",
  "lastName": "Doe"
}
```

---

## Login

```http
POST /api/auth/login
```

Public endpoint.

A successful login creates a server-side session.

---

## Current User

```http
GET /api/auth/me
```

Returns the currently authenticated user.

Authentication is required.

---

## Logout

```http
POST /api/auth/logout
```

Invalidates the current authenticated session.

---

## Get Users

```http
GET /api/users
```

Authentication is required.

The endpoint returns a JSON array directly:

```text
List<User>
```

Example response:

```json
[
  {
    "id": 1,
    "email": "user@test.com",
    "firstName": "John",
    "lastName": "Doe",
    "status": "active"
  }
]
```

This endpoint is temporary and will be replaced by organization-scoped membership access when tenant isolation is introduced.

---

# Authentication

Authentication is based on server-side sessions.

The general flow is:

```text
Browser
   ↓
POST /api/auth/login
   ↓
Nginx
   ↓
Spring Security
   ↓
AuthService
   ↓
UserRepository
   ↓
PostgreSQL
   ↓
Server-side session
   ↓
Session cookie returned to browser
```

Spring Session JDBC stores session data in PostgreSQL.

The application also uses CSRF protection for state-changing requests.

The frontend API client sends credentials and the CSRF token automatically.

---

# Current Backend Structure

Backend features are organized by domain.

Example:

```text
auth/
├── AuthController
├── AuthService
└── ...

user/
├── User
├── UserController
├── UserService
└── UserRepository
```

The typical backend flow is:

```text
Client Request
      ↓
Controller
      ↓
Service
      ↓
Repository
      ↓
JPA / Hibernate
      ↓
PostgreSQL
```

Business logic belongs in services, while data access belongs in repositories.

---

# Continuous Integration

Every change goes through a Pull Request.

The workflow is:

```text
Feature branch
      ↓
Pull Request
      ↓
CI
      ↓
Review
      ↓
Merge
      ↓
main
```

The CI pipeline validates:

```text
backend tests
frontend dependencies
frontend lint
frontend build
Docker Compose configuration
Docker image build
container startup
HTTPS backend healthcheck
```

The HTTPS healthcheck uses:

```text
https://localhost:8443/api/healthcheck
```

---

# Development Principle

When adding a new feature, start from the use case and define the complete data flow.

For a standard backend feature:

```text
Client Request
      ↓
Controller
      ↓
Service
      ↓
Repository
      ↓
Database
```

For a feature requiring Python:

```text
Client Request
      ↓
Spring Boot Controller
      ↓
Spring Boot Service
      ↓
FastAPI
      ↓
Spring Boot
      ↓
Repository / Database
      ↓
Client Response
```

The frontend communicates with Spring Boot only.

---

# Stop the Docker Environment

Stop the stack:

```bash
docker compose down
```

Rebuild after changing Docker configuration or dependencies:

```bash
docker compose up --build
```





---------------------------------------------------------------------------------------------------------





FEATURES LIST

**Priorities**

| | Meaning |
|---|---|
| **P0** | Required for the mandatory 14 points, or required by the subject. Non-negotiable. |
| **P1** | Bonus points. Droppable without breaking anything. Build in the listed order. |
| **Reserve** | Do not start. Listed only so nobody re-proposes them as new ideas. |

---



## Overview

### P0 — the mandatory core (17 features → 14 points)

| ID | Feature | Scope | Module earned | Pts |
|---|---|---|---|---|
| **F00** | Baseline technique | Repo, Docker, CI, health end-to-end | Frameworks + ORM + microservices | 5 |
| **F00+** | HTTPS / Nginx / TLS | Single public entry point, TLS, `.env` | *subject requirement* | 0 |
| **F01** | Authentification | Register, login, session, hashed passwords | *subject requirement* | 0 |
| **F02** | Organisations / cabinets | Multi-tenant model | Organisation system | 2 |
| **F03** | Invitations et membres | Invite client or colleague | part of F02 | ↑ |
| **F04** | Rôles et permissions | Admin, comptable, client | Advanced permissions | 2 |
| **F05** | Pages légales et profil | Privacy Policy, Terms, profile | *subject requirement* | 0 |
| **F06** | Dépôt de documents | Upload PDF / XML / Factur-X | File upload and management | 1 |
| **F07** | Liste documents et statuts | History, status, details | *product* | 0 |
| **F08** | Validation Factur-X Java | PDF/A-3, XSD, Schematron, profiles | Module of choice, part 1 | 2 |
| **F09** | Rapport lisible | Errors a human understands | Module of choice, part 2 | ↑ |
| **F10** | Lecteur XML facture | XML → readable invoice | *product* | 0 |
| **F11** | Extraction PDF Python | Extract fields from a plain PDF | *product* | 0 |
| **F12** | Formulaire de correction | Human checks and fixes fields | *product* | 0 |
| **F13** | Génération Factur-X | Python generates, Java validates | *product* | 0 |
| **F14** | Queue et traitement async | Jobs, statuses, retries, locking | *our solution to the concurrency requirement* | 0 |
| **F15** | Temps réel WebSocket | Live statuses, multi-user | Real-time features | 2 |
| | | | **Total** | **14** |



### P1 — bonus (4 features → 5 points), in this order

| ID | Feature | Scope | Module earned | Pts |
|---|---|---|---|---|
| **F17** | API publique sécurisée | API key, rate limit, docs, 5+ endpoints | Public API | 2 |
| **F16** | Recherche avancée | Filters, sort, pagination | Advanced search | 1 |
| **F20** | Export / import | CSV / JSON / PDF | Export / import | 1 |
| **F21** | RGPD | Data export and deletion | GDPR compliance | 1 |
| | | | **Total** | **5** |

**Ceiling: 19 points.** Bonus is capped at 5 by the subject. Anything beyond that is
wasted effort.



### Reserve — do not start

| ID | Feature | Why not |
|---|---|---|
| **F18** | Assistant IA RAG | The subject requires "a large dataset of information". A few hundred rules is arguable, and a module that fails its demo scores zero. Also an LLM inside a compliance tool, where determinism is the whole point. |
| **F19** | Journal d'audit | Earns **no module**. Good B2B practice, real cost, zero points. |
| **F22** | 2FA | We chose other bonus modules. Only if week 5 finishes early. |
| **F23** | Dashboard analytics | Worth 2 points we cannot use — we are already at the 19 ceiling. For *team* tracking we use GitHub Projects and Issues, not a dashboard inside the application. |

---



## Two things to understand before assigning work

**Six P0 features earn zero points** — F07, F10, F11, F12, F13, F14. That is fine:
they *are* the product. Without them the conformity engine has nothing to demonstrate,
and a Major module with nothing to show scores zero.

**F11 to F13 carry 2 points indirectly.** They are not mandatory features in the
subject. They are P0 because the Python service needs a real responsibility for
*Backend as microservices* to be demonstrable — a service that only answers `/health`
is not a microservice, and a module that cannot be demonstrated scores zero. Drop
extraction and generation and the module collapses with them.

---

# P0 — detailed

## F00 — Baseline technique

**Priority** P0, first sprint · **Owner** whole team · **Depends on** nothing

**Goal.** A common base that starts end to end — Frontend → Backend → Database — with
Docker and a green CI.

> HTTPS, Nginx and `.env` handling are **F00+**, a separate feature that runs in
> parallel. F00 does not wait for it.

**Mini-tasks**

*Frontend*
- Minimal page displaying backend and database status
- Call `GET /api/health`
- Clear message on error
- Tailwind CSS installed and configured ← **added; the subject requires a CSS framework**

*Backend*
- `GET /api/health`
- Minimal DB connection with `SELECT 1`
- JSON response: `{ "backend": "ok", "database": "ok" }`

*Extractor (Python)* ← **added, skeleton only — see the note below**
- FastAPI container answering `/health` and nothing else
- On the internal Docker network, not exposed
- Included in the aggregate health response

*Database*
- PostgreSQL container
- Minimal `init.sql`
- `pg_isready` healthcheck

*Tests / CI*
- Build frontend and backend
- `docker compose up` in CI
- Automated health check
- A PR does not merge if the health check fails

*Docs*
- README with the launch command
- Simple architecture diagram

**Definition of Done**
- `docker compose up --build` works
- The frontend page shows Backend OK and Database OK
- CI is green on `main`

> **Open point — when the Python container first appears.** Still undecided.
>
> *For including a skeleton now:* thirty lines proves the Docker network path, a third
> image in CI and the compose orchestration — the parts that cost a day when discovered
> in week four with several branches open. The HTTP call itself is trivial; the
> plumbing is not.
>
> *For waiting until F11:* the baseline stays smaller and we do not pay for complexity
> before a feature needs it.
>
> Salma's position is that F00+ should land before we attack FastAPI and the
> microservices part, which sequences HTTPS first either way.
>
> **This is a scheduling question, not an architectural one** — the architecture is
> settled. If the team prefers F11, delete the *Extractor* block above and move it
> into F11.

---

## F00+ — HTTPS / Nginx / TLS

**Priority** P0 · **Owner** 1 person — **must be assigned this week** ·
**Depends on** F00 containers existing · **Runs in parallel with** F02, F03, F04

**Goal.** Put the whole stack behind HTTPS with a single public entry point.

> **Why this is its own feature.** HTTPS is explicitly mandatory in the subject's
> technical requirements — its absence causes rejection, not a lower mark. **Nginx is
> not required by the subject**; it is our chosen way of providing TLS termination and
> a single entry point. Kept inside F00 it had no owner and could be forgotten before
> the later features; as its own feature it is tracked and does not block the team.

**Mini-tasks**

*Nginx*
- Reverse proxy as the single public entry point
- TLS termination, self-signed certificate for development
- Route `/` to the frontend, `/api/*` to Spring Boot
- WebSocket upgrade passthrough for F15 ← easy to forget, painful to debug later
- The **only** container published to the host

*Docker*
- Remove the direct host port mappings for backend and frontend
- Keep PostgreSQL on the internal network only
- Everything else reachable by service name inside the Docker network

*Environment*
- `.env` in `.gitignore`
- `.env.example` committed, same keys with placeholder values
- Check `git log --all -- .env` — if a real `.env` was ever committed, the credentials
  must be rotated, not just the file deleted

*Frontend*
- Axios `baseURL` pointing at the same origin
- `withCredentials: true` — our auth is cookie-based

*Tests / CI*
- CI starts the stack and hits `https://localhost/api/health`
- Assert `:8080` is **not** reachable from outside

*Docs*
- One command, one address in the README
- How to trust the self-signed certificate in development

**Definition of Done**
- The app is reachable at `https://localhost`
- `:8080` and `:5173` are **not** reachable from the host
- `.env.example` is committed and `.env` is ignored
- CI proves it

---

## F01 — Authentification

**Priority** P0 · **Owner** 1 person · **Depends on** F00

**Goal.** Let a user create an account and sign in securely.

**Mini-tasks**

*Frontend*
- Register and Login pages
- Form validation on email and password
- API error handling
- Redirect after login

*Backend*
- `POST /api/auth/register`, `POST /api/auth/login`
- `POST /api/auth/logout`, `GET /api/auth/me` ← **added**
- Password hashing with salt (BCrypt — the salt lives inside the hash, no separate column)
- **Session cookie, not JWT** ← see the note below
- Auth middleware protecting routes
- CSRF enabled — do **not** call `.csrf().disable()`

*Database*
- Table `users`: id, email, password_hash, first_name, last_name, created_at, status
- Unique index on email, **stored lowercased and trimmed**

*Tests / CI*
- Register and login success
- Wrong password, email already used
- **Unknown email and wrong password return byte-identical responses** ← otherwise
  anyone can probe which emails are registered
- Password is never stored in clear, never returned in a response

*Docs*
- Auth routes and error format

**Definition of Done**
- A user can register and sign in
- A protected route refuses an unauthenticated user
- No clear-text password anywhere in the database
- `GET /me` drives the SPA boot; no token stored in the frontend

> **Why sessions and not JWT.** Logout must actually work, and a JWT cannot be revoked
> without a server-side blocklist — which reintroduces state anyway. An `HttpOnly`
> cookie is unreachable from JavaScript, unlike a token in `localStorage`. And the
> WebSocket handshake in F15 sends the cookie automatically.

---

## F02 — Organisations / cabinets

**Priority** P0 · **Owner** 1 person · **Depends on** F01

**Goal.** The multi-tenant model: an accounting firm has its own members, clients and
documents.

**Mini-tasks**

*Frontend*
- Organisation creation page
- Organisation selection page
- Display name, role, main members

*Backend*
- Minimal organisation CRUD
- Creator becomes owner/admin
- Middleware loading the current organisation
- **Replace `GET /api/users` with `GET /api/organizations/{id}/members`** ← the global
  endpoint would let one firm list another firm's clients

*Database*
- Tables `organizations`, `organization_members`
- Relations user_id, organization_id, role
- Index on organization_id

*Tests / CI*
- A user can create an organisation
- A user does not see other organisations
- Multi-tenant isolation tests

*Docs*
- Organisation and member schema

**Definition of Done**
- Data is always tied to an organisation
- A user cannot access an organisation they do not belong to

---

## F03 — Invitations et membres

**Priority** P0 · **Owner** 1 person · **Depends on** F01, F02

**Goal.** Let a firm invite clients or colleagues into its organisation.

**Mini-tasks**

*Frontend*
- Invite-by-email interface
- Pending invitations list
- Accept / decline action

*Backend*
- `POST /organizations/{id}/invitations`
- Invitation token
- Acceptance by a signed-in user
- Expiry and status

*Database*
- Table `invitations`: email, organization_id, role, token, status, expires_at
- Members table updated on acceptance

*Tests / CI*
- Only an admin can create an invitation
- Acceptance adds the member
- Expired invitation refused

*Docs*
- Invitation flow in the README

**Definition of Done**
- An admin invites a client, the client joins, the role is correct

---

## F04 — Rôles et permissions

**Priority** P0 · **Owner** 1 person · **Depends on** F02, F03

**Goal.** Control actions by role: admin, comptable, client.

**Mini-tasks**

*Frontend*
- Show or hide actions by role
- Role management page for admins
- Access-denied messages

*Backend*
- Permission middleware
- Policies: `can_upload`, `can_validate`, `can_invite`, `can_manage_users`, `can_delete`
- **Every sensitive route checked server-side** — hiding a button is not access control
- Every query touching tenant data filters by `organization_id`

*Database*
- Role in `organization_members`
- Optional `permissions` table if the model grows

*Tests / CI*
- A client cannot invite
- An accountant sees client documents
- An admin can change a role
- 403 tests

*Docs*
- Permission matrix

**Definition of Done**
- Every sensitive action has a backend check
- Roles are visible in the interface

---

## F05 — Pages légales et profil utilisateur

**Priority** P0 · **Owner** 1 person · **Depends on** F01

**Goal.** Cover the subject's basic obligations and add a simple profile page.

**Mini-tasks**

*Frontend*
- **Privacy Policy and Terms of Service pages, linked in the footer, with real content**
  — placeholder pages cause rejection
- Profile page: email, name, active organisation
- Edit first and last name

*Backend*
- `GET` / `PUT /me`
- Validation of editable fields

*Database*
- Profile columns: first_name, last_name, created_at, updated_at

*Tests / CI*
- Footer contains the legal links
- Pages are reachable and not empty
- Profile update validated

*Docs*
- Legal content adapted to this project — invoices contain commercial data, say what
  we store and for how long

**Definition of Done**
- Privacy Policy and Terms are visible and not empty
- The user profile works

---

## F06 — Dépôt de documents

**Priority** P0 · **Owner** 1 person · **Depends on** F01–F04

**Goal.** Let an authorised member upload an invoice: PDF, XML or Factur-X.

**Mini-tasks**

*Frontend*
- Drag-and-drop upload zone
- Client-side extension and size checks
- Upload progress
- Success and failure messages
- File preview and delete ← required by the module description

*Backend*
- Secure upload endpoint
- Server-side MIME, extension and size checks
- File storage on a volume
- Document record creation

*Database*
- Table `documents`: id, organization_id, owner_id, filename, type, size, status,
  storage_path
- Statuses: uploaded, queued, processing, valid, invalid, failed

*Tests / CI*
- PDF and XML accepted
- Oversized file refused
- User without permission refused

*Docs*
- Accepted types and limits

**Definition of Done**
- An authorised user uploads a file
- The document appears in the database and in the document list

---

## F07 — Liste documents et statuts

**Priority** P0 · **Owner** 1 person · **Depends on** F06

**Goal.** Show the document history with processing status.

**Mini-tasks**

*Frontend*
- Documents page
- Table: name, type, date, user, status
- Document detail page
- Readable status badges

*Backend*
- `GET /documents` with pagination
- `GET /documents/{id}`
- Filtered by organisation

*Database*
- Indexes on organization_id, created_at, status
- Relations to users and reports

*Tests / CI*
- A user sees only their organisation's documents
- Pagination works
- Unknown document → 404

*Docs*
- Status descriptions

**Definition of Done**
- The user sees their documents, statuses are clear and persistent

---

## F08 — Validation Factur-X (Java)

**Priority** P0 — core domain · **Owner** 1 backend person · **Depends on** F06

**Goal.** Build the deterministic validation engine that checks invoices and produces
usable results.

**Mini-tasks**

*Frontend*
- Validate button, or automatic validation after upload
- Loading and status display

*Backend (Java)*
- Validation service, four chained layers:
  1. PDF/A-3 structure — is the XML embedded, is the XMP metadata correct (veraPDF)
  2. XSD — is the XML schema-valid CII
  3. Schematron EN 16931 — the CEN business rules
  4. Factur-X profile constraints
- Stop reporting downstream noise once an upstream layer fails hard

*Database*
- Tables `validation_runs`, `validation_errors`, `rule_catalog`
- Link document → run → errors

*Tests / CI*
- Corpus of valid and invalid invoices from FNFE-MPE
- **Mutation tests**: take a valid invoice, break exactly one thing, assert the
  validator catches *that specific rule*
- CI fails if a generated invoice is invalid

*Docs*
- Explain the four validation layers
- **`LICENSES` section** for Mustangproject, veraPDF, Saxon ← check before merging

**Definition of Done**
- A document can be validated, the result is stored
- At least one valid case and one invalid case can be demonstrated

---

## F09 — Rapport d'erreurs lisible

**Priority** P0 — core domain · **Owner** 1 person · **Depends on** F08

**Goal.** Turn technical errors into clear explanations for a non-expert. **This is our
differentiator** — validators exist, readable diagnostics do not.

**Mini-tasks**

*Frontend*
- Report page with a summary: valid / invalid
- Error list with severity, message, field, value
- Never a bare rule code with no explanation

*Backend*
- Mapping rule code → French message
- Include actual and expected values wherever possible
- `GET /documents/{id}/report`

*Database*
- `rule_catalog`: code, title_fr, description_fr, correction_hint_fr
- Enriched `validation_errors`
- **French text lives in the table, not in the code** — it is data, not logic

*Tests / CI*
- A known BR error produces a readable message
- Empty report when the invoice is valid
- Snapshot tests of the JSON format

*Docs*
- Error examples
- Rule coverage: how many rules have a French explanation, out of the total

**Definition of Done**
- The user understands why an invoice was refused
- The report is reachable from the document page

---

## F10 — Lecteur XML facture

**Priority** P0 · **Owner** 1 person · **Depends on** F06

**Goal.** Display a raw XML as a readable invoice.

**Mini-tasks**

*Frontend*
- Invoice view: seller, buyer, dates, lines, VAT, totals
- Responsive layout
- Message when a field is missing

*Backend*
- CII XML parser
- `GET /documents/{id}/invoice-view`
- Amount and date normalisation

*Database*
- Not required to store everything; on-demand read is fine
- Optional `invoice_views` cache

*Tests / CI*
- Simple XML → readable view
- Malformed XML → clean error
- Totals and lines tests

*Docs*
- Supported formats and limits

**Definition of Done**
- An uploaded XML becomes readable as a normal invoice

---

## F11 — Extraction PDF (Python)

**Priority** P0 · **Owner** 1 Python person · **Depends on** F06

**Goal.** Automatically extract the important fields from a plain PDF.

**Mini-tasks**

*Frontend*
- "Convertir en Factur-X" button
- Extraction progress

*Backend (Python)*
- `POST /extract` on the internal service
- Extract seller, buyer, date, number, lines, VAT, totals
- **Confidence score per field**
- **No database access, no auth** — receives a file, returns JSON

*Backend (Java)*
- Checks user, organisation, permissions and document **before** delegating
- Validates the shape of the returned JSON, persists the draft

*Database*
- Table `extracted_fields` or `draft_invoices`
- Keep source, value, confidence

*Tests / CI*
- Sample PDF → expected fields
- Difficult PDF → missing fields but a clean error
- **Measure and publish extraction accuracy per field**

*Docs*
- State plainly that extraction must be checked by a human

**Definition of Done**
- A PDF produces a structured draft invoice
- Low-confidence fields are visibly flagged

---

## F12 — Formulaire de correction humaine

**Priority** P0 · **Owner** 1 person · **Depends on** F11

**Goal.** Let the user check and correct extracted fields before generation. We do not
promise perfect extraction — we guarantee a conformant result.

**Mini-tasks**

*Frontend*
- Invoice form: header, parties, lines, taxes, totals
- Add and remove a line
- Required-field validation
- **Visible confidence indicator per field**

*Backend*
- Draft save API
- Server-side validation of amounts and types
- Total recalculation

*Database*
- Tables `draft_invoices`, `draft_lines`, `draft_taxes`
- Version or updated_at

*Tests / CI*
- Missing required field refused
- Line sums consistent with totals
- Draft save and reload

*Docs*
- Form validation rules

**Definition of Done**
- A user corrects the fields and saves a valid draft

---

## F13 — Génération Factur-X

**Priority** P0 · **Owner** 1 Python person + Java integration · **Depends on** F12, F08

**Goal.** Generate a Factur-X from a valid draft, then have Java validate it.

**Mini-tasks**

*Frontend*
- Generate button
- Result and download link
- Validation report shown after generation

*Backend (Python)*
- `POST /generate`
- CII XML generation
- Embed the XML in a PDF/A-3
- **Profiles BASIC and EN 16931 only**

*Backend (Java)*
- **Independent validation of the generated file**
- Return errors if generation is non-conformant

*Database*
- Generated document linked to the source document
- Store generation status and report

*Tests / CI*
- Golden sample: PDF → draft → Factur-X → validation OK
- **CI fails if a generated sample is non-conformant**

*Docs*
- Limits: supported profiles, excluded formats

**Definition of Done**
- A generated invoice can be downloaded
- It is validated by the Java engine after generation

> **This is the elegant part of the architecture.** Python generates, Java validates,
> and the two share no code. The generator can never mark its own homework.

---

## F14 — Queue et traitement asynchrone

**Priority** P0 · **Owner** 1 backend person · **Depends on** F06, F08, F13

**Goal.** Stop the user waiting during long validations and conversions, and handle
concurrent actions correctly.

> **What the subject actually requires:** multiple users active simultaneously,
> concurrent actions handled properly, real-time updates propagated, and no race
> conditions or data corruption. **It does not require a queue.** The queue is *our*
> architectural answer to that requirement — worth stating precisely, because at the
> defence we will be asked why we built it this way, not told that we had to.

**Mini-tasks**

*Frontend*
- Show queued / processing / done
- Retry button on failure

*Backend*
- Job model
- Validation and conversion worker
- Limited retries
- Error handling
- **Locking so two accountants cannot process the same document concurrently** ←
  this is the part that answers the no-race-conditions requirement

*Database*
- Table `jobs`: type, document_id, status, attempts, error, started_at, finished_at
- Index on status

*Tests / CI*
- A job moves from queued to done
- Failure stores the error
- Retry works or is capped
- **Concurrent processing of the same document does not corrupt data**

*Docs*
- Job lifecycle

**Definition of Done**
- Validation and generation run in the background
- Statuses stay persistent

---

## F15 — Temps réel WebSocket

**Priority** P0 · **Owner** 1 fullstack person · **Depends on** F14, F07

**Goal.** Notify connected users when a document changes status.

**Mini-tasks**

*Frontend*
- WebSocket connection
- Automatic document table update
- Reconnection handling

*Backend*
- WebSocket endpoint
- **Broadcast scoped by organisation**
- `document.status.changed` events

*Database*
- Not required

*Tests / CI*
- Two clients receive the update
- **A client from another organisation receives nothing**
- Clean disconnection

*Docs*
- Real-time event format

**Definition of Done**
- The accountant sees the status change without refreshing
- Organisation isolation respected

---

# P1 — bonus, in this order

## F17 — API publique sécurisée

**Priority** P1, take first · **Owner** 1 backend person · **Depends on** F01–F09

**Goal.** Let a technical client interact with documents via API. Coherent for a B2B
tool — accounting software vendors are the natural users.

**Mini-tasks**

*Frontend* — API keys page, create and revoke, link to documentation
*Backend* — API key auth, rate limiting, at least 5 endpoints (list, upload, status,
report, download), OpenAPI/Swagger
*Database* — `api_keys`: key hash, owner, scopes, revoked_at
*Tests* — valid key accepted, invalid refused, rate limit tested, scopes respected
*Docs* — endpoint documentation with curl examples

**Definition of Done** — an API key gives access to at least 5 documented, rate-limited
endpoints

---

## F16 — Recherche avancée

**Priority** P1 · **Owner** 1 person · **Depends on** F07, F09

**Mini-tasks**

*Frontend* — search bar, filters (status, date, type, user, error), sort, pagination
*Backend* — `GET /documents/search`, filter validation, efficient pagination
*Database* — indexes on status, date, type, filename
*Tests* — filters combine, pagination stable, **organisation isolation**
*Docs* — supported filters

**Definition of Done** — the user finds an invoice by name, status or error

---

## F20 — Export / import

**Priority** P1 · **Owner** 1 person · **Depends on** F07, F09

**Mini-tasks**

*Frontend* — export CSV/JSON/PDF button, import page with validation preview
*Backend* — export endpoints, CSV/JSON generation, import with error report
*Database* — optional `imports` table with status and errors
*Tests* — export has the right columns, invalid import refused without corruption
*Docs* — export and import formats

**Definition of Done** — an admin exports their data and imports a valid file

---

## F21 — RGPD

**Priority** P1 · **Owner** 1 person · **Depends on** F01, F02

**Mini-tasks**

*Frontend* — "mes données" page, request export, request deletion with confirmation
*Backend* — user data export, deletion or anonymisation by role, strong confirmation
*Database* — `deletion_requested` / `anonymized` status
*Tests* — export contains the user's data, deletion respects organisations and documents
*Docs* — simplified retention policy

**Definition of Done** — a user can export their data and request deletion or
anonymisation, with mandatory confirmation

---

# Milestones

| Week | Milestone | Features |
|---|---|---|
| 1 | **M0** Baseline | F00, F00+ + Factur-X research spike |
| 2 | **M1** Auth and tenancy | F01, F02, F03, F04, F05 |
| 3 | **M2** Documents and validation | F06, F07, F08 |
| 4 | **M3** Reports and reading | F09, F10, F11 |
| 5 | **M4** Conversion and real-time | F12, F13, F14, F15 — **the mandatory 14 are complete here** |
| 6 | **M5** Freeze Monday | Bonus only if already started (F17, F16, F20, F21). Then tests, final README, deployment, defence rehearsal |




---





---

# Numbers to collect as we go

Do not leave these to week 6; they are much easier to gather while building.

- Extraction accuracy per field, over N invoices (F11)
- Round-trip: N generated invoices, 100% pass independent validation (F13)
- Validation latency p50 / p95 (F08)
- Rule coverage: rules with a French explanation, out of the total (F09)
- Number of mutation tests passing (F08)

---

