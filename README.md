 # FacturX App

FacturX is a full-stack web application built with a React frontend, a Spring Boot backend, and PostgreSQL.



The application follows an incremental architecture: Spring Boot is the main API exposed to the frontend, while specialized Python processing can later be delegated internally to a FastAPI service when required.

## Tech Stack

### Frontend

- React
- Vite
- JavaScript
- Tailwind CSS

### Backend

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Maven

### Internal Python Service

Planned for features requiring specialized Python processing:

- Python
- FastAPI

FastAPI is not directly exposed to the frontend. Spring Boot remains the main API and delegates specific processing tasks to FastAPI when needed.

### Database

- PostgreSQL

### Infrastructure

- Nginx
- Docker
- Docker Compose
- GitHub Actions

Nginx is the single public entry point and terminates HTTPS. The subject requires that
any connection reaching the backend from outside use HTTPS, so the browser never talks
to Vite or Spring Boot directly in the deployed stack.

---

# Architecture

The frontend communicates only with the Spring Boot API.

Spring Boot handles the main application logic, database access, authentication, permissions, document management, and orchestration of internal services.

```text
Browser
   ↓
HTTPS
   ↓
Nginx
Single public entry point
TLS termination
   ↓
   ├── static assets ──→ Frontend
   │                     React + Vite
   │
   └── /api/* ─────────→ Backend
                         Spring Boot
                         ↓
                         ├──────────────→ PostgreSQL
                         │                Port 5432
                         │
                         └──────────────→ FastAPI
                                          Internal Python service
                                          Specialized processing
```

Only Nginx is exposed to the host. The frontend, the backend, PostgreSQL and the
future FastAPI service all live on the internal Docker network.

The main communication flow is:

```text
Frontend
   ↓ HTTP / JSON
Spring Boot
   ↓
   ├── JPA / Hibernate → PostgreSQL
   │
   └── HTTP / JSON → FastAPI
                     when Python processing is required
```

The frontend does not communicate directly with FastAPI.

Spring Boot acts as the central API and decides whether a request:

- is handled directly in Java;
- requires access to PostgreSQL;
- or must be delegated to the internal Python service.

---

# Backend Responsibilities

Spring Boot is responsible for the main application API and business logic.

Examples include:

```text
Authentication
Organizations
Users
Roles and permissions
Documents
Validation
Reports
Database access
API error handling
Internal service orchestration
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

For features requiring Python:

```text
Client Request
      ↓
Spring Boot Controller
      ↓
Spring Boot Service
      ↓
FastAPI internal service
      ↓
Processing result
      ↓
Spring Boot
      ↓
PostgreSQL if persistence is required
      ↓
Response to frontend
```

This keeps the API exposed to the frontend stable while allowing specialized processing to use Python when it provides a technical advantage.

---

# FastAPI Internal Service

FastAPI will be introduced when features require specialized Python processing.

Examples include:

```text
PDF extraction
Factur-X generation
```

A typical flow for PDF extraction will be:

```text
React
   ↓
Spring Boot
   ↓
Validate user / permissions / document
   ↓
FastAPI
   ↓
Extract invoice information
   ↓
Return structured JSON
   ↓
Spring Boot
   ↓
Persist required data in PostgreSQL
   ↓
Return response to React
```

For Factur-X generation:

```text
React
   ↓
Spring Boot
   ↓
FastAPI
Generate XML / Factur-X document
   ↓
Spring Boot
   ↓
Java validation
   ↓
PostgreSQL
Store status / report
   ↓
React
```

Spring Boot remains responsible for the public API contract and application-level authorization.

FastAPI focuses only on specialized processing.

---

# Incremental Architecture

FastAPI is not required for the initial technical baseline.

The project evolves incrementally.

```text
Initial baseline

React
   ↓
Spring Boot
   ↓
PostgreSQL
```

Then, when Python-specific features are implemented:

```text
React
   ↓
Spring Boot
   ↓
   ├── PostgreSQL
   │
   └── FastAPI
```

This allows the initial baseline to remain simple and stable while adding Python only when a feature requires it.

## Open point: when the FastAPI container appears

Two positions were discussed. Adding the container in the baseline proves the Docker
network path, the third image in CI, and the compose orchestration early — those are
the parts that cost a day when discovered late, not the HTTP call itself. Adding it at
F11 keeps the baseline smaller and avoids paying for complexity before a feature needs
it.

A middle option: a skeleton container in the baseline that answers `/health` and
nothing else, roughly thirty lines, with the real extraction logic arriving at F11.

To be decided as a team. It is a scheduling question, not an architectural one — the
architecture above holds either way.

---

# Project Structure

Current baseline:

```text
FacturX/
│
├── README.md
├── docker-compose.yml
├── .env.example
├── .gitignore              (.env is ignored)
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

The architecture can later evolve to:

```text
FacturX/
│
├── README.md
├── docker-compose.yml
├── .env.example
│
├── nginx/
│
├── frontend/
│
├── backend/
│
└── python-service/
    ├── Dockerfile
    ├── requirements.txt
    └── app/
```

The exact FastAPI directory structure will be defined when the first Python feature is implemented.

For detailed backend architecture and development instructions, see:

[Backend README](./backend/README.md)

---

# Quick Start With Docker

The easiest way to run the current baseline is with Docker Compose.

From the project root, first create your local environment file:

```bash
cp .env.example .env
```

Generate the local TLS certificates if they are not already present:

```bash
./scripts/generate-certs.sh
```

Then start the stack:

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

The application is available at a single address:

```text
https://localhost
```

Test the health endpoint:

```bash
curl -k https://localhost/api/healthcheck
```

The current health check response is:

```text
Yess i'm working
```

FastAPI will be added to Docker Compose when the first Python-based feature is
implemented.

---

# Environment variables

Real values live in `.env`, which is **ignored by Git**. A committed `.env.example`
holds the same keys with placeholder values, so anyone cloning the repository knows
what to fill in. Both are explicit subject requirements.

```bash
# .env.example
POSTGRES_DB=facturx
POSTGRES_USER=facturx
POSTGRES_PASSWORD=changeme
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/facturx
```

Never commit a real `.env`. If one was ever committed, deleting the file is not enough
— the credentials must be rotated, because they remain in the Git history.

---

# Docker Architecture

Inside the current Docker Compose environment:

```text
facturx-network
│
├── nginx
│      ↓
│   the only container exposed to the host (:80 and :443)
│      ↓
│   frontend, backend
│
├── frontend
│
├── backend
│      ↓
│   postgres:5432
│
└── postgres
```

The backend communicates with PostgreSQL using the Docker service name:

```text
postgres
```

instead of:

```text
localhost
```

because each Docker container has its own network namespace.

When FastAPI is introduced, it will also run inside the internal Docker network:

```text
facturx-network
│
├── nginx          ← only container published to the host
│
├── frontend
│
├── backend
│      ├── postgres:5432
│      └── fastapi:<internal-port>
│
├── postgres
│
└── fastapi
```

The FastAPI service does not need to be directly exposed to the browser.

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
npm install
```

Start the Vite development server:

```bash
npm run dev
```

Frontend:

```text
http://localhost:5173
```

---

## Backend

For backend development instructions, database configuration, architecture, and API development conventions, see:

[Backend README](./backend/README.md)

The backend normally runs on:

```text
http://localhost:8080
```

---

# Current Development Architecture

The current baseline follows this structure:

```text
Frontend
   ↓
REST API
   ↓
Spring Boot Controllers
   ↓
Services
   ↓
Repositories
   ↓
JPA / Hibernate
   ↓
PostgreSQL
```

Backend features are organized by domain.

Example:

```text
auth/
├── AuthController
└── AuthService

user/
├── User
├── UserController
├── UserService
└── UserRepository
```

This keeps each feature and its related logic together.

When Python-specific features are introduced, Spring Boot will orchestrate calls to the internal FastAPI service without changing the frontend API entry point.

More details are documented in the backend-specific README.

---

# Current API

## Health Check

```http
GET /api/healthcheck
```

Example:

```bash
curl -k https://localhost/api/healthcheck
```

---

## Register User

```http
POST /api/auth/register
```

---

## Get Users

```http
GET /api/users
```

> **Temporary — to be replaced in F02.** This endpoint returns every user in the
> system. Once organisations exist, that would let an accountant from one firm list
> the clients of another. Hiding the page in the UI is not access control: anyone can
> call the endpoint with curl.
>
> It will become:
>
> ```http
> GET /api/organizations/{id}/members
> ```
>
> authorised by membership, with the query filtered by `organization_id`.

---

# Local Development Rules

## Tenant isolation

Once organisations exist, **every query touching organisation data must filter by
`organization_id`**, and every sensitive route must check permissions **server side**.
This is not defensive style — the permissions module is one of our Major modules and
will be probed directly at the evaluation.

## Continuous Integration

Every change goes through a Pull Request. `main` is protected: a PR merges only when
the pipeline is green.

```text
Feature branch → Pull Request → CI → Merge → main
```

The pipeline runs:

```text
build frontend
build backend
build images
start the stack
health check
tests
```

CI is what keeps `main` deployable while five people work in parallel. It also gives a
clear signal before a merge, instead of discovering an integration problem days later.

---

# Development Principle

When adding a feature, first define the use case and the complete data flow.

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

For a feature requiring Python processing:

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

Avoid putting business logic directly in controllers.

Avoid exposing internal implementation details to the frontend.

The frontend communicates with Spring Boot, while Spring Boot is responsible for coordinating the different internal components of the application.

Backend-specific conventions and examples are available in:

[Backend README](./backend/README.md)

---

# Stop the Docker Environment

```bash
docker compose down
```

To rebuild after changing Docker configuration or dependencies:

```bash
docker compose up --build
```
