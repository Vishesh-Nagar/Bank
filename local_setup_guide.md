# Bank Application Local Setup Guide

This guide provides step-by-step instructions for getting the Bank Application running locally on your machine. The architecture consists of a React frontend and a Spring Boot microservice backend, backed by MySQL databases, Redis for rate limiting/caching, and Kafka for event-driven asynchronous messaging.

## Prerequisites

Ensure you have the following installed on your machine:
- **Docker** (for running the infrastructure: MySQL, Redis, Kafka)
- **Java 21 or 25** (the backend uses Java 25 based on the `pom.xml`, but Java 21+ should work)
- **Maven 3.x**
- **Node.js 18+ and npm** (for the frontend)

---

## Step 1: Infrastructure Setup using Docker

The backend operates as a set of microservices, requiring distinct database schemas, Redis, and Kafka.

The `docker-compose.yml` in the root of your project spins up:
1. **MySQL** (Port 3306) with initialized schemas: `bank_users`, `bank_accounts`, `bank_payments`.
2. **Redis** (Port 6379) for API Gateway rate-limiting and JWT blocklisting.
3. **Kafka & Zookeeper** (Port 9092 & 2181) for event-driven messaging.

**To start the infrastructure:**
1. Open a terminal in the root directory (`d:\Github\Bank`).
2. Run the following command:
   ```bash
   docker compose up -d
   ```
This will download the required images, spin them up, and create the databases automatically.

---

## Step 2: Environment Variables & Credentials

For enhanced security, the microservices do not contain hardcoded default credentials in their code. You **must** provide the following environment variables before running the backend.

**Required Environment Variables:**
- **DB_USERNAME**: `root` (If using the docker-compose setup)
- **DB_PASSWORD**: `password` (If using the docker-compose setup)
- **JWT_SECRET**: A secure random string for JWT generation/validation (e.g., `my_super_secret_jwt_key_1234567890`)
- **INTERNAL_SERVICE_SECRET**: A secure random string for internal inter-service communication authentication (e.g., `my_internal_service_secret_key`)

**Optional Environment Variables:**
- **CORS_ALLOWED_ORIGINS**: Defines allowed frontend origins (Defaults to `http://localhost:3000,http://localhost:5173`)
- **KAFKA_BOOTSTRAP**: Kafka address (Defaults to `localhost:9092`)

**How to set them:**

Create a `.env` file in the `server` directory (e.g. `d:\Github\Bank\server\.env`) and add your values there. Spring Boot has been configured to automatically pick this up.

```properties
# server/.env
DB_USERNAME=root
DB_PASSWORD=password
JWT_SECRET=my_super_secret_jwt_key_1234567890
INTERNAL_SERVICE_SECRET=my_internal_service_secret_key
```

*Note: You do not need to export these manually in the terminal if you use the `.env` file approach!*

---

## Step 3: Running the Backend Microservices

Because the backend is split into multiple modules, you'll need to run them. The main services you need to start are:
- `user-service` (Port 8081)
- `account-service` (Port 8082)
- `payment-service` (Port 8083)
- `notification-service` (Port 8084)
- `api-gateway` (Port 8080 - Routes all traffic)

**Using your IDE (Recommended):**
The easiest way to run the backend is to open the `server` folder in IntelliJ IDEA, Eclipse, or VS Code. Ensure you configure the Environment Variables in your Run Configurations, and then run the main application classes for each microservice.

**Using Terminal (Maven):**
If you prefer the command line, simply create your `.env` file in the `server` folder, open separate terminal tabs/windows for each service, navigate to the `server` directory, and run:

```bash
# Terminal 1: User Service
cd server
./mvnw spring-boot:run -pl user-service

# Terminal 2: Account Service
cd server
./mvnw spring-boot:run -pl account-service

# Terminal 3: Payment Service
cd server
./mvnw spring-boot:run -pl payment-service

# Terminal 4: Notification Service
cd server
./mvnw spring-boot:run -pl notification-service

# Terminal 5: API Gateway
cd server
./mvnw spring-boot:run -pl api-gateway
```

---

## Step 4: Running the Frontend

Once your backend services are up and running, you can start the React frontend.

1. Open a new terminal and navigate to the `client` directory:
   ```bash
   cd client
   ```
2. Install the dependencies:
   ```bash
   npm install
   ```
3. Start the development server:
   ```bash
   npm run dev
   ```

The frontend will start and be accessible at [http://localhost:5173](http://localhost:5173). It will communicate with the backend via the API Gateway (running on `http://localhost:8080`).

> [!TIP]
> If you encounter issues where the frontend fails to communicate with the backend, ensure your `api-gateway` is actively running, Kafka/Redis are reachable, and all services share the exact same `JWT_SECRET` and `INTERNAL_SERVICE_SECRET` environment variables.
