# Bank Application — Backend

Multi-module Spring Boot microservice backend for the Bank Application.

## Architecture

The backend is organized as a **Maven multi-module project** with a shared parent POM. Each service is independently deployable with its own Dockerfile.

| Module                | Type      | Port | Database        | Key Dependencies                          |
|-----------------------|-----------|------|-----------------|-------------------------------------------|
| `common-lib`          | Library   | —    | —               | Shared DTOs, Kafka events, exceptions      |
| `user-service`        | Service   | 8081 | `bank_users`    | Spring Security, JPA, Kafka, Redis, Mail   |
| `account-service`     | Service   | 8082 | `bank_accounts` | JPA, Kafka, OpenFeign (→ user-service)     |
| `payment-service`     | Service   | 8083 | `bank_payments` | JPA, Kafka, OpenFeign (→ account-service), WebSocket |
| `notification-service`| Service   | 8084 | —               | Kafka consumer, WebSocket (STOMP/SockJS)   |
| `api-gateway`         | Gateway   | 8080 | —               | Spring Cloud Gateway, Redis rate limiter   |

## Inter-Service Communication

- **Synchronous:** OpenFeign clients (account → user, payment → account) authenticated via `INTERNAL_SERVICE_SECRET`
- **Asynchronous:** Kafka events (payment → notification, account → notification)
- **Client-facing:** API Gateway routes all external traffic, validates JWTs, enforces rate limits

## Prerequisites

- Java 21+
- Maven 3.x (or use the included `mvnw` wrapper)
- Running infrastructure: MySQL 8, Redis 7, Kafka (see root `docker-compose.yml`)

## Running Locally

### 1. Start infrastructure via Docker

```bash
# From the repo root:
docker compose up -d    # Starts MySQL, Redis, Kafka, Zookeeper
```

### 2. Configure environment

Create `server/.env`:
```properties
DB_USERNAME=root
DB_PASSWORD=password
JWT_SECRET=my_super_secret_jwt_key_1234567890
INTERNAL_SERVICE_SECRET=my_internal_service_secret_key
```

### 3. Run services

Each service must be started separately (order matters for Feign dependencies):

```bash
cd server

# 1. User Service (no dependencies on other services)
./mvnw spring-boot:run -pl user-service

# 2. Account Service (depends on user-service)
./mvnw spring-boot:run -pl account-service

# 3. Payment Service (depends on account-service)
./mvnw spring-boot:run -pl payment-service

# 4. Notification Service (depends on Kafka only)
./mvnw spring-boot:run -pl notification-service

# 5. API Gateway (depends on all services)
./mvnw spring-boot:run -pl api-gateway
```

## Building

```bash
# Build all modules:
cd server
./mvnw -B clean package -DskipTests

# Build a single service:
./mvnw -B -pl user-service -am package -DskipTests
```

## API Routes (via Gateway on :8080)

| Method | Path                    | Service          | Auth Required |
|--------|-------------------------|------------------|---------------|
| POST   | `/api/v1/users/login`   | user-service     | No            |
| POST   | `/api/v1/users/**`      | user-service     | Varies        |
| GET    | `/api/v1/accounts/**`   | account-service  | Yes           |
| POST   | `/api/v1/payments/**`   | payment-service  | Yes           |
| GET    | `/api/v1/admin/**`      | user-service     | Admin only    |
| WS     | `/ws/**`                | notification-svc | Token         |
