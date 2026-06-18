# Bank Application Local Setup Guide

This guide provides step-by-step instructions for getting the Bank Application running locally on your machine. The architecture consists of a React frontend and a Spring Boot microservice backend, backed by MySQL databases.

## Prerequisites

Ensure you have the following installed on your machine:
- **Docker** (for running the database easily)
- **Java 21 or 25** (the backend uses Java 25 based on the `pom.xml`, but Java 21+ should work)
- **Maven 3.x**
- **Node.js 18+ and npm** (for the frontend)

---

## Step 1: Database Setup using Docker

The backend operates as a set of microservices, and each core service requires its own distinct database schema. The necessary schemas are:
- `bank_users` (for user-service)
- `bank_accounts` (for account-service)
- `bank_payments` (for payment-service)

To make things easy, I've created two files in the root of your project:
1. `docker-compose.yml`
2. `init-db.sql` (Initializes the three required databases automatically)

**To start the database:**
1. Open a terminal in the root directory (`d:\Github\Bank`).
2. Run the following command:
   ```bash
   docker compose up -d
   ```
This will download the `mysql:8.0` image, spin it up on port `3306`, and create the databases. The default `root` password is set to `password`.

---

## Step 2: Environment Variables & Credentials

The microservices are configured to read database credentials from environment variables. If they aren't provided, they fall back to default values.

**Default Credentials (used if no env vars are set):**
- **DB_USERNAME**: `root`
- **DB_PASSWORD**: `password`
- **JWT_SECRET**: `mysecretkey`

If you used the `docker-compose up` command from Step 1, these default credentials match the database perfectly, and **you do not need to configure any further database credentials manually**.

*Optional:* If you want to use custom credentials, you can export them in your terminal before running the backend:
```bash
# Windows PowerShell
$env:DB_USERNAME="your_username"
$env:DB_PASSWORD="your_password"
$env:JWT_SECRET="your_custom_secure_jwt_secret"
```

---

## Step 3: Running the Backend Microservices

Because the backend is split into multiple modules, you'll need to run them. The main services you need to start are:
- `user-service` (Port 8081)
- `account-service`
- `payment-service`
- `api-gateway` (Usually routes requests to the others, typically Port 8080)

**Using your IDE (Recommended):**
The easiest way to run the backend is to open the `server` folder in IntelliJ IDEA, Eclipse, or VS Code, and run the main application classes for each microservice.

**Using Terminal (Maven):**
If you prefer the command line, open separate terminal tabs/windows, navigate to the `server` directory, and run each service:

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

# Terminal 4: API Gateway
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

The frontend will start and be accessible at [http://localhost:5173](http://localhost:5173). It will communicate with the backend via the API Gateway (usually running on `http://localhost:8080`).

> [!TIP]
> If you encounter issues where the frontend fails to communicate with the backend, ensure your `api-gateway` is actively running and correctly routing to the microservices.
