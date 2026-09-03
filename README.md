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
