# FacturX Backend

Spring Boot backend for the FacturX application.

This README explains:

- how to run the backend locally
- how to run it with Docker
- how the backend is structured
- how to approach new backend features

[Back to main project README](../README.md)

---

# Tech Stack

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- PostgreSQL
- Maven
- Docker

---

# Backend Mental Model

The backend follows this flow:

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

Each part has one main responsibility.

```text
Controller
= receives HTTP requests

Service
= contains business logic

Repository
= reads/writes data

JPA / Hibernate
= maps Java objects to database tables

PostgreSQL
= stores the actual data
```

---

# Project Structure

The backend is organized by feature/domain.

```text
src/main/java/com/facturx/app/
│
├── BackendApplication.java
│
├── auth/
│   ├── AuthController.java
│   └── AuthService.java
│
└── user/
    ├── User.java
    ├── UserController.java
    ├── UserService.java
    └── UserRepository.java
```

The main application stays at the root:

```text
com.facturx.app.BackendApplication
```

Feature-specific code is placed inside subpackages:

```text
auth/
user/
invoice/
...
```

---

# How To Think Before Writing Code

Start from the use case, not from Spring classes.

For example:

```text
"I want to get all users."
```

Ask:

```text
1. What request comes from the client?
   → GET /api/users

2. Who receives the request?
   → UserController

3. What logic should happen?
   → UserService

4. Do we need database access?
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

This same reasoning should be reused for new features.

---

# Controller

A controller is the entry point from the external world into the backend.

Example:

```text
GET /api/users
      ↓
UserController
```

Its job is mainly to:

```text
receive request
      ↓
extract request data
      ↓
call the service
      ↓
return response
```

Controllers should not contain large amounts of business logic.

---

# Service

A service contains the application logic.

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
get all users
```

Services can use repositories when they need to access stored data.

---

# Repository

A repository provides access to the database.

Example:

```text
UserService
     ↓
UserRepository
     ↓
PostgreSQL
```

`UserRepository` uses Spring Data JPA.

Common operations include:

```text
save(...)
findAll()
findById(...)
delete(...)
```

The repository should focus on data access, not business logic.

---

# Entity

An entity represents data stored in the database.

Current `User` concept:

```text
User
├── userId
├── name
├── lastName
├── email
├── homeAdress
└── sex
```

JPA maps it to the PostgreSQL table:

```text
users
├── user_id
├── name
├── last_name
├── email
├── home_adress
└── sex
```

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

Authentication-related operations belong under the `auth` feature.

Examples:

```text
register
login
logout
token management
```

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

User-related operations belong under the `user` feature.

Examples:

```text
get users
get user by id
update user
delete user
```

Both `AuthService` and `UserService` can use the same `UserRepository`.

---

# Running Locally

## Requirements

You need:

- Java 17
- PostgreSQL

Check Java:

```bash
java -version
```

---

# PostgreSQL Configuration

The backend currently expects:

```text
Database: facturx
User: postgres
Password: postgres
Port: 5432
```

The Spring configuration uses environment variables with local defaults:

```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:5432/facturx
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
```

When running locally:

```text
DB_HOST is not defined
        ↓
localhost is used
        ↓
jdbc:postgresql://localhost:5432/facturx
```

---

# Start Backend Locally

From the `backend/` directory:

```bash
./mvnw spring-boot:run
```

The backend runs on:

```text
http://localhost:8080
```

Test it:

```bash
curl http://localhost:8080/api/healthcheck
```

---

# Run PostgreSQL With Docker Only

If you want to run Spring Boot locally but PostgreSQL inside Docker:

```bash
docker run -d \
  --name postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=facturx \
  -p 5432:5432 \
  postgres
```

Spring Boot can still use:

```text
localhost:5432
```

because PostgreSQL port `5432` is exposed to the host.

The architecture is:

```text
Spring Boot on host
        ↓
localhost:5432
        ↓
PostgreSQL container
```

---

# Running Backend With Docker

Build the backend image:

```bash
docker build -t facturx-backend .
```

The backend Dockerfile uses a multi-stage build:

```text
Build stage
Java + Maven
     ↓
creates JAR

Runtime stage
Java runtime
     ↓
runs JAR
```

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

Therefore Docker development uses:

```text
DB_HOST=postgres
```

where `postgres` is the Docker Compose service name.

---

# Docker Compose

From the project root:

```bash
docker compose up --build
```

The backend receives:

```text
DB_HOST=postgres
DB_USER=postgres
DB_PASSWORD=postgres
```

Spring therefore resolves:

```properties
jdbc:postgresql://${DB_HOST:localhost}:5432/facturx
```

as:

```text
jdbc:postgresql://postgres:5432/facturx
```

---

# Local vs Docker

The same Spring configuration works in both environments.

```text
LOCAL

DB_HOST not set
      ↓
localhost
      ↓
PostgreSQL
```

```text
DOCKER

DB_HOST=postgres
      ↓
postgres
      ↓
PostgreSQL container
```

This avoids changing `application.properties` manually when switching environments.

---

# Current API

## Health Check

```http
GET /api/healthcheck
```

Test:

```bash
curl http://localhost:8080/api/healthcheck
```

---

## Register User

```http
POST /api/auth/register
```

Example:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John",
    "lastName": "Doe",
    "email": "john@test.com",
    "homeAdress": "Paris",
    "sex": "F"
  }'
```

---

## Get All Users

```http
GET /api/users
```

Example:

```bash
curl http://localhost:8080/api/users
```

---

# Development Cycle For A New Feature

When adding a new feature, use this cycle.

```text
1. Define the use case
        ↓
2. Define the HTTP endpoint
        ↓
3. Define the request/response data
        ↓
4. Create or update the Controller
        ↓
5. Create the Service logic
        ↓
6. Add Repository access if required
        ↓
7. Add/update the Entity if required
        ↓
8. Test the API
        ↓
9. Verify database state
```

Example:

```text
"I want to retrieve an invoice"
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

The package could then become:

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
→ business logic

Repository
→ database access

Entity
→ persisted data

JPA / Hibernate
→ object ↔ relational mapping

PostgreSQL
→ persistent storage
```

When unsure where new code belongs, start by asking:

```text
Is this HTTP handling?
→ Controller

Is this application/business logic?
→ Service

Is this database access?
→ Repository

Is this persisted data?
→ Entity
```