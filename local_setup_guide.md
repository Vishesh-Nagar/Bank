# Bank Application — Local Setup Guide

Step-by-step guide for running the Bank Application locally. The architecture consists of a React frontend, 5 Spring Boot microservices, and Dockerized infrastructure (MySQL, Redis, Kafka).

## Prerequisites

- **Docker Desktop** (for MySQL, Redis, Kafka)
- **Java 21+**
- **Maven 3.x** (or use the included `mvnw` wrapper)
- **Node.js 18+** and **npm**

---

## Step 1: Infrastructure (Docker)

From the **repository root** (`Bank/`):

```bash
docker compose up -d
```

This starts:
| Service     | Port | Purpose                                    |
|-------------|------|--------------------------------------------|
| MySQL 8.0   | 3306 | Databases: `bank_users`, `bank_accounts`, `bank_payments` |
| Redis 7     | 6379 | API Gateway rate limiting, JWT blocklisting |
| Kafka       | 9092 | Event-driven messaging (external port)      |
| Zookeeper   | 2181 | Kafka coordination                          |

Verify everything is healthy:
```bash
docker compose ps
```

---

## Step 2: Environment Variables

Create a `.env` file in the `server/` directory:

```properties
# server/.env
DB_USERNAME=root
DB_PASSWORD=password
JWT_SECRET=my_super_secret_jwt_key_1234567890
INTERNAL_SERVICE_SECRET=my_internal_service_secret_key
```

> [!NOTE]
> Spring Boot automatically reads this `.env` file via `spring.config.import` — no manual exporting needed.

> [!IMPORTANT]
> All services **must** share the same `JWT_SECRET` and `INTERNAL_SERVICE_SECRET` values, otherwise JWT validation and inter-service auth will fail.

---

## Step 3: Backend Microservices

Start each service in a separate terminal. **Order matters** — downstream services depend on upstream ones being healthy.

```bash
cd server

# Terminal 1: User Service (:8081) — no service dependencies
./mvnw spring-boot:run -pl user-service

# Terminal 2: Account Service (:8082) — depends on user-service
./mvnw spring-boot:run -pl account-service

# Terminal 3: Payment Service (:8083) — depends on account-service
./mvnw spring-boot:run -pl payment-service

# Terminal 4: Notification Service (:8084) — depends on Kafka only
./mvnw spring-boot:run -pl notification-service

# Terminal 5: API Gateway (:8080) — depends on all services
./mvnw spring-boot:run -pl api-gateway
```

> [!TIP]
> When running locally (not in Docker), Kafka is accessible on `localhost:9092`. The default config already handles this via the `KAFKA_BOOTSTRAP` env var defaulting to `bank-kafka:29092` for Docker. For local runs, Spring auto-configures from the `.env` file. If you hit Kafka connectivity issues, set `KAFKA_BOOTSTRAP=localhost:9092` in your `server/.env`.

---

## Step 4: Frontend

```bash
cd client
npm install
npm run dev
```

The frontend starts at **http://localhost:5173** and proxies API calls to the gateway at `localhost:8080` via Vite's dev server proxy.

---

## Alternative: Full Docker Stack

Instead of running services individually, start **everything** in Docker:

```bash
# From the repo root:
docker compose --profile full up -d --build
```

This builds and starts all 5 backend services, the client (on port 3000), and all infrastructure. Use this when you don't need hot-reload on the backend.

| Profile     | Command                                     | What it starts                    |
|-------------|---------------------------------------------|-----------------------------------|
| *(default)* | `docker compose up -d`                      | Infrastructure only               |
| `backend`   | `docker compose --profile backend up -d`    | Infra + all 5 backend services    |
| `full`      | `docker compose --profile full up -d`       | Everything including React client |

---

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| Frontend can't reach backend | API Gateway not running | Ensure `api-gateway` is started last and healthy |
| `Connection refused` on Kafka | Wrong bootstrap address | Set `KAFKA_BOOTSTRAP=localhost:9092` in `server/.env` for local runs |
| JWT validation fails across services | Mismatched secrets | Ensure all services share the same `JWT_SECRET` |
| MySQL connection refused | Docker not running | Run `docker compose up -d` from repo root |
