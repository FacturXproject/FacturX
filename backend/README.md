# FacturX Backend

Spring Boot backend for the FacturX application.

This README explains:

- how to run the backend locally;
- how to run it with Docker;
- how the backend is structured;
- how authentication and persistence work;
- how to approach new backend features.

[Back to main project README](../README.md)

---

# Tech Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Security
- Spring Session JDBC
- PostgreSQL
- Maven
- Docker

---

# Backend Mental Model

The backend follows this general flow:

```text
HTTP Request
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

Each layer has one main responsibility:

```text
Controller
= receives HTTP requests and returns HTTP responses

Service
= contains application and business logic

Repository
= reads and writes persistent data

JPA / Hibernate
= maps Java objects to database tables

PostgreSQL
= stores persistent application data
```

Controllers should remain thin.

Business logic belongs in services, while database access belongs in repositories.

---

# Project Structure

The backend is organized by feature/domain.

Current structure:

```text
src/main/java/com/facturx/app/
│
├── BackendApplication.java
│
├── config/
│
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   └── ...
│
└── user/
    ├── User.java
    ├── UserController.java
    ├── UserService.java
    └── UserRepository.java
```

The main Spring Boot application remains at:

```text
com.facturx.app.BackendApplication
```

Feature-specific code is placed inside dedicated packages:

```text
auth/
user/
organization/
invitation/
document/
...
```

This keeps the code organized by application domain instead of grouping every controller, service, or repository into large global folders.

---

# How To Think Before Writing Code

Start from the use case, not from the Spring classes.

For example:

```text
"I want to retrieve the users."
```

Ask:

```text
1. What request comes from the client?

   → GET /api/users

2. Who receives the request?

   → UserController

3. What application logic is required?

   → UserService

4. Is database access required?

   → UserRepository

5. What object represents the stored data?

   → User
```

The resulting flow becomes:

```text
GET /api/users
      ↓
UserController
      ↓
UserService
      ↓
UserRepository
      ↓
PostgreSQL
      ↓
List<User>
      ↓
JSON response
```

The same reasoning should be reused when implementing new features.

---

# Controller

A controller is the HTTP entry point into the backend.

Example:

```text
GET /api/users
      ↓
UserController
```

Its main responsibilities are:

```text
receive request
      ↓
validate / extract request data
      ↓
call the appropriate service
      ↓
return the HTTP response
```

Controllers should not contain large amounts of business logic.

---

# Service

A service contains application and business logic.

Example:

```text
AuthController
      ↓
AuthService
      ↓
register user
```

Or:

```text
UserController
      ↓
UserService
      ↓
retrieve users
```

Services use repositories when persistent data must be read or modified.

Multiple services can use the same repository when they operate on the same domain data.

For example:

```text
AuthService ──────┐
                  ↓
             UserRepository
                  ↑
UserService ──────┘
```

---

# Repository

A repository provides access to persistent data.

Example:

```text
UserService
     ↓
UserRepository
     ↓
PostgreSQL
```

`UserRepository` uses Spring Data JPA.

Typical repository operations include:

```text
save(...)
findAll()
findById(...)
delete(...)
```

Repositories should focus on data access rather than business rules.

---

# Entity

An entity represents data persisted in PostgreSQL.

The current `User` entity contains:

```text
User
├── id
├── email
├── passwordHash
├── firstName
├── lastName
├── status
├── createdAt
└── updatedAt
```

The corresponding PostgreSQL table contains fields such as:

```text
users
├── id
├── email
├── password_hash
├── first_name
├── last_name
├── status
├── created_at
└── updated_at
```

The password hash is stored in the database but is not exposed in API JSON responses.

Mental model:

```text
Java User object
       ↓
JPA / Hibernate
       ↓
PostgreSQL users table
```

---

# Current Feature Flows

## Register User

```text
POST /api/auth/register
        ↓
AuthController
        ↓
AuthService
        ↓
UserRepository
        ↓
PostgreSQL
```

The authentication domain currently handles operations such as:

```text
register
login
logout
current authenticated user (/me)
session management
```

Authentication uses server-side sessions.

---

## User Management

```text
GET /api/users
      ↓
UserController
      ↓
UserService
      ↓
UserRepository
      ↓
PostgreSQL
```

The current `/api/users` endpoint requires authentication.

It is temporary and will evolve when organization membership and tenant isolation are introduced.

---

# Authentication

Authentication is session-based.

The general login flow is:

```text
Client
   ↓
POST /api/auth/login
   ↓
Spring Security
   ↓
AuthService
   ↓
UserRepository
   ↓
PostgreSQL
   ↓
Server-side session created
   ↓
Session cookie returned to browser
```

Spring Session JDBC stores session information in PostgreSQL.

The application also uses CSRF protection for state-changing requests.

The frontend sends credentials and the CSRF token automatically through the shared API client.

---

# Running Locally

## Requirements

You need:

- Java 21
- PostgreSQL

Check Java:

```bash
java -version
```

---

# PostgreSQL Configuration

The local development configuration uses PostgreSQL on port:

```text
5432
```

Typical local values are:

```text
Database: facturx
User: postgres
Password: postgres
Port: 5432
```

The Spring configuration supports environment variables with local defaults.

Example:

```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:5432/facturx
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
```

When running Spring Boot directly on the host:

```text
DB_HOST not defined
        ↓
localhost
        ↓
jdbc:postgresql://localhost:5432/facturx
```

---

# Start Backend Locally

From the `backend/` directory:

```bash
./mvnw spring-boot:run
```

The standalone Spring Boot backend runs on:

```text
http://localhost:8080
```

Test the backend directly:

```bash
curl http://localhost:8080/api/healthcheck
```

Current response:

```text
Yess i'm working
```

This is a direct connection to Spring Boot.

Nginx and HTTPS are not involved in this standalone development mode.

---

# Run PostgreSQL With Docker Only

If you want to run Spring Boot locally while PostgreSQL runs inside Docker:

```bash
docker run -d \
  --name postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=facturx \
  -p 5432:5432 \
  postgres:17
```

Spring Boot can then connect to:

```text
localhost:5432
```

because PostgreSQL port `5432` is explicitly published to the host.

Architecture:

```text
Spring Boot on host
        ↓
localhost:5432
        ↓
PostgreSQL container
```

---

# Running With Docker Compose

From the project root:

```bash
docker compose up --build
```

The current stack contains:

```text
nginx
frontend
backend
postgres
```

Only Nginx is exposed to the host.

The frontend, backend, and PostgreSQL remain on the internal Docker network.

---

# Docker Network Architecture

The deployed development stack follows this structure:

```text
Browser
   ↓
HTTP :8080
   ↓
Nginx
   ↓
redirect
   ↓
HTTPS :8443
   ↓
Nginx
   ├── /      → frontend:5173
   │
   └── /api/* → backend:8080
                      ↓
                 postgres:5432
```

The host ports are:

```text
Host :8080 → Nginx container :80
Host :8443 → Nginx container :443
```

Inside Docker, Nginx still listens on the standard HTTP and HTTPS ports:

```text
80
443
```

The higher host ports are used so the stack can run in environments where Docker cannot expose privileged host ports below `1024`.

---

# HTTPS Entry Point

When using the full Docker Compose stack, the application is available at:

```text
https://localhost:8443
```

The HTTP endpoint:

```text
http://localhost:8080
```

redirects to:

```text
https://localhost:8443
```

The backend API is therefore accessed through Nginx using:

```text
https://localhost:8443/api/*
```

For example:

```bash
curl -k https://localhost:8443/api/healthcheck
```

Current response:

```text
Yess i'm working
```

The current `/api/healthcheck` endpoint is a simple backend availability check.

It does not currently return a structured backend/database status such as:

```json
{
  "backend": "ok",
  "database": "ok"
}
```

Database connectivity can be added to the application healthcheck later if required.

---

# Backend + PostgreSQL In Docker

When both Spring Boot and PostgreSQL run inside Docker:

```text
backend container
      ↓
postgres:5432
      ↓
postgres container
```

Inside the backend container:

```text
localhost
```

would refer to the backend container itself.

Therefore the backend uses the Docker Compose service name:

```text
postgres
```

to reach PostgreSQL.

Architecture:

```text
backend
   ↓
postgres:5432
   ↓
PostgreSQL
```

---

# Local vs Docker

The same Spring application can run in both environments.

## Local

```text
Spring Boot
     ↓
localhost:5432
     ↓
PostgreSQL
```

## Docker

```text
Spring Boot container
     ↓
postgres:5432
     ↓
PostgreSQL container
```

The difference is the PostgreSQL hostname.

---

# Current API

## Health Check

```http
GET /api/healthcheck
```

Public endpoint.

### Through the full Docker stack

```bash
curl -k https://localhost:8443/api/healthcheck
```

### Directly against Spring Boot

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

Expected request body:

```json
{
  "email": "john@test.com",
  "password": "strongpassword",
  "firstName": "John",
  "lastName": "Doe"
}
```

The password must satisfy the backend validation rules.

The application uses CSRF protection, so state-changing requests made outside the frontend may require the appropriate CSRF cookie and header.

---

## Login

```http
POST /api/auth/login
```

Public endpoint.

A successful login creates an authenticated server-side session.

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

The endpoint currently returns a JSON array directly:

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

> **Temporary endpoint**
>
> `/api/users` currently returns users globally.
>
> When organizations and tenant isolation are fully introduced, user access must be scoped by organization membership instead of exposing a global user list.

---

# PostgreSQL Healthcheck

Docker Compose checks PostgreSQL availability using the configured database user and database name:

```text
pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}
```

This keeps the healthcheck aligned with the PostgreSQL environment configuration instead of hardcoding a specific role or database.

The PostgreSQL container must become healthy before dependent services start.

---

# Development Cycle For A New Feature

When adding a new backend feature, follow this reasoning process:

```text
1. Define the use case
        ↓
2. Define the HTTP endpoint
        ↓
3. Define request and response data
        ↓
4. Create or update the Controller
        ↓
5. Implement Service logic
        ↓
6. Add Repository access if required
        ↓
7. Add or update Entity models if required
        ↓
8. Add tests
        ↓
9. Test the API
        ↓
10. Verify database state
```

Example:

```text
"I want to retrieve an invoice."
```

becomes:

```text
GET /api/invoices/{id}
        ↓
InvoiceController
        ↓
InvoiceService
        ↓
InvoiceRepository
        ↓
PostgreSQL
```

The package could then be:

```text
invoice/
├── Invoice.java
├── InvoiceController.java
├── InvoiceService.java
└── InvoiceRepository.java
```

---

# Backend Rule Of Thumb

Keep responsibilities separated:

```text
Controller
→ HTTP layer

Service
→ application / business logic

Repository
→ database access

Entity
→ persisted data

JPA / Hibernate
→ object ↔ relational mapping

PostgreSQL
→ persistent storage
```

When unsure where new code belongs, ask:

```text
Is this HTTP handling?
→ Controller

Is this application or business logic?
→ Service

Is this database access?
→ Repository

Is this persisted data?
→ Entity
```

---

# Testing

Run backend tests from:

```bash
cd backend
```

Then:

```bash
./mvnw -B test
```

The tests should pass before opening or merging a Pull Request.

---

# Stop The Docker Environment

From the project root:

```bash
docker compose down
```

To rebuild after changing Docker configuration or dependencies:

```bash
docker compose up --build
```
