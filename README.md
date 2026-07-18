# Bank Application

A full-stack banking application built with a **microservice architecture**. Users can register, log in, manage bank accounts, and perform peer-to-peer payments with real-time notifications.

## Architecture

```
┌─────────────┐     ┌──────────────────────────────────────────────────────┐
│   Client    │────▶│              API Gateway (:8080)                     │
│  React/Vite │     │         Rate Limiting · JWT Validation               │
│   (:3000)   │◀────│              Route Forwarding                        │
└─────────────┘     └────┬──────────┬──────────────┬───────────────────────┘
                         │          │              │
                    ┌────▼───┐ ┌────▼─────┐  ┌─────▼──────┐  ┌──────────────┐
                    │ User   │ │ Account  │  │ Payment    │  │ Notification │
                    │Service │ │ Service  │  │ Service    │  │ Service      │
                    │ :8081  │ │  :8082   │  │  :8083     │  │  :8084       │
                    └───┬────┘ └────┬─────┘  └─────┬──────┘  └──────┬───────┘
                        │          │               │                │
                    ┌───▼──────────▼───────────────▼────┐     ┌─────▼─────┐
                    │          MySQL 8.0                 │     │   Kafka   │
                    │  bank_users │ bank_accounts │      │     │  Events   │
                    │             │ bank_payments │      │     └───────────┘
                    └───────────────────────────────────┘
```

## Tech Stack

| Layer         | Technology                                    |
|---------------|-----------------------------------------------|
| Frontend      | React 19, TypeScript, Vite, MUI, TailwindCSS  |
| API Gateway   | Spring Cloud Gateway, Redis (rate limiting)    |
| Backend       | Java 21, Spring Boot 3.5, Spring Security      |
| Messaging     | Apache Kafka (event-driven notifications)      |
| Database      | MySQL 8.0 (3 schemas), Redis (JWT blocklist)   |
| Containerization | Docker, Docker Compose                      |

## Quick Start (Docker)

```bash
# 1. Clone and configure
git clone <repo-url>
cd Bank
cp .env.example .env        # Edit with your secrets

# 2. Start infrastructure only (MySQL, Redis, Kafka)
docker compose up -d

# 3. Start everything (infra + backend + UI)
docker compose --profile full up -d

# 4. Access the app
#    UI:      http://localhost:3000
#    API:     http://localhost:8080
```

### Viewing Logs (Isolated Terminals)
By running the cluster in detached mode (`-d`), you can open multiple terminal windows to monitor specific services individually without the logs overlapping:
```bash
# Terminal 1: Watch API Gateway logs
docker compose logs -f bank-api-gateway

# Terminal 2: Watch User Service logs
docker compose logs -f bank-user-service
```
*(Press `Ctrl+C` to stop watching the logs. To stop the entire cluster, run `docker compose down`)*

### Docker Compose Profiles

| Command                                      | What it starts                        |
|----------------------------------------------|---------------------------------------|
| `docker compose up -d`                       | Infrastructure only (MySQL/Redis/Kafka)|
| `docker compose --profile backend up -d`     | Infra + all 5 backend microservices    |
| `docker compose --profile full up -d`        | Everything including the React client  |
| `docker compose up -d --build user-service`  | Rebuild & restart a single service     |

## Local Development (Without Docker for services)

See [local_setup_guide.md](local_setup_guide.md) for running services outside Docker against Dockerized infrastructure.

## Project Structure

```
Bank/
├── docker-compose.yml          # Full-stack orchestration
├── .env.example                # Environment template
├── init-db.sql                 # Database schema initialization
├── client/                     # React/Vite frontend
│   ├── Dockerfile
│   ├── nginx.conf              # Production reverse proxy config
│   └── src/
├── server/                     # Spring Boot microservices
│   ├── pom.xml                 # Parent POM (multi-module)
│   ├── common-lib/             # Shared DTOs, events, exceptions
│   ├── user-service/           # Auth, registration, user management
│   ├── account-service/        # Bank account CRUD, balance operations
│   ├── payment-service/        # P2P transfers, daily limits
│   ├── notification-service/   # Kafka consumer, WebSocket push
│   └── api-gateway/            # Route forwarding, rate limiting, JWT
```

## Services

| Service               | Port | Database        | Description                              |
|-----------------------|------|-----------------|------------------------------------------|
| `api-gateway`         | 8080 | —               | Routes, rate limits, JWT validation       |
| `user-service`        | 8081 | `bank_users`    | Registration, login, email verification   |
| `account-service`     | 8082 | `bank_accounts` | Account CRUD, deposits, withdrawals       |
| `payment-service`     | 8083 | `bank_payments` | P2P transfers, daily limits, scheduling   |
| `notification-service`| 8084 | —               | Kafka consumer → WebSocket push           |
| `client`              | 3000 | —               | React SPA served via Nginx                |

## Environment Variables

Copy `.env.example` to `.env` and fill in:

| Variable                 | Required | Default                           | Description                    |
|--------------------------|----------|-----------------------------------|--------------------------------|
| `DB_USERNAME`            | ✅        | —                                 | MySQL username                 |
| `DB_PASSWORD`            | ✅        | —                                 | MySQL root password            |
| `JWT_SECRET`             | ✅        | —                                 | JWT signing key (64+ chars)    |
| `INTERNAL_SERVICE_SECRET`| ✅        | —                                 | Inter-service auth token       |
| `KAFKA_BOOTSTRAP`        | ❌        | `bank-kafka:29092`                | Kafka broker address           |
| `REDIS_HOST`             | ❌        | `bank-redis`                      | Redis hostname                 |
| `CORS_ALLOWED_ORIGINS`   | ❌        | `http://localhost,...:5173,...:3000`| Allowed CORS origins           |
| `LOG_PATH`               | ❌        | `./logs`                          | Log output directory           |