# FacturX App

FacturX is a full-stack web application built with a React frontend, a Spring Boot backend, and PostgreSQL.
## Tech Stack

### Frontend

- React
- Vite
- JavaScript

### Backend

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Maven

### Database

- PostgreSQL

### Infrastructure

- Docker
- Docker Compose

---

# Architecture

```text
Browser
   ↓
Frontend
React + Vite
Port 5173
   ↓
REST API
   ↓
Backend
Spring Boot
Port 8080
   ↓
JPA / Hibernate
   ↓
PostgreSQL
Port 5432
```

The main communication flow is:

```text
Frontend
   ↓ HTTP / JSON
Backend
   ↓
PostgreSQL
```

---

# Project Structure

```text
facturx-app/
│
├── README.md
├── docker-compose.yml
│
├── frontend/
│   ├── Dockerfile
│   ├── package.json
│   └── src/
│
└── backend/
    ├── README.md
    ├── Dockerfile
    ├── pom.xml
    ├── mvnw
    └── src/
```

For detailed backend architecture and development instructions, see:

[Backend README](./backend/README.md)

---

# Quick Start With Docker

The easiest way to run the complete project is with Docker Compose.

From the project root:

```bash
docker compose up --build
```

This starts:

```text
frontend
backend
postgres
```

The services are available at:

```text
Frontend
http://localhost:5173

Backend
http://localhost:8080

PostgreSQL
localhost:5432
```

Test the backend health endpoint:

```bash
curl http://localhost:8080/api/healthcheck
```

---

# Docker Architecture

Inside Docker Compose:

```text
facturx-network
│
├── frontend
│      ↓
│   browser access through :5173
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

---

# Local Development

The frontend and backend can also be developed independently without running the full Docker Compose stack.

## Frontend

From:

```bash
cd frontend
```

install dependencies:

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

The project currently follows this structure:

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

More details are documented in the backend-specific README.

---

# Current API

## Health Check

```http
GET /api/healthcheck
```

Example:

```bash
curl http://localhost:8080/api/healthcheck
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

---

# Development Principle

When adding a feature, first define the use case and the data flow.

Example:

```text
Client request
      ↓
Controller
      ↓
Service
      ↓
Repository
      ↓
Database
```

Avoid putting business logic directly in controllers.

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