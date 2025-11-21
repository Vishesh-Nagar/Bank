# Bank Application

This is a simple banking application with a React frontend and a Spring Boot backend. It allows users to register, log in, and manage their bank accounts.

## Overall Flow

The application consists of two main parts:

*   **Frontend:** A single-page application built with React and TypeScript. It provides the user interface for interacting with the application.
*   **Backend:** A RESTful API built with Spring Boot and Java. It handles the business logic, data persistence, and authentication.

The frontend communicates with the backend through REST API calls. The backend uses a MySQL database to store user and account information.

## Technologies Used

*   **Frontend:** React, TypeScript, Vite
*   **Backend:** Java, Spring Boot, Spring Security, JPA (Hibernate)
*   **Database:** MySQL

## Installation Steps

### Prerequisites

*   Java 21 or later
*   Maven 3.x
*   Node.js 18.x or later
*   npm 9.x or later
*   MySQL 8.x

### Backend Setup

1.  **Configure the database:**
    *   Open the `src/main/resources/application.yaml` file.
    *   Update the `spring.datasource.username` and `spring.datasource.password` properties with your MySQL credentials.
    *   Make sure you have a database named `bank` in your MySQL server.

2.  **Run the backend server:**
    Open a terminal in the root directory of the project and run the following command:
    ```bash
    ./mvnw spring-boot:run
    ```
    The backend server will start on port `8080`.

### Frontend Setup

1.  **Install dependencies:**
    Open a terminal in the `client` directory and run the following command:
    ```bash
    npm install
    ```

2.  **Run the frontend server:**
    In the same terminal, run the following command:
    ```bash
    npm run dev
    ```
    The frontend development server will start on port `5173`.

You can now access the application at `http://localhost:5173`.

## API Endpoints

### User Endpoints

#### `POST /api/users`

Create a new user.

**Request Body:**

```json
{
  "username": "testuser",
  "password": "password",
  "email": "test@example.com"
}
```

**Response:**

```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com"
}
```

#### `POST /api/users/login`

Authenticate a user and get a token.

**Request Body:**

```json
{
  "username": "testuser",
  "password": "password"
}
```

**Response:**

```json
{
    "user": {
        "id": 1,
        "username": "testuser",
        "email": "test@example.com"
    }
}
```

### Account Endpoints

All account endpoints require authentication.

#### `POST /api/accounts`

Create a new bank account for the authenticated user.

**Request Body:**

```json
{
  "accountHolderName": "Test User",
  "balance": 1000.0,
  "accountType": "SAVINGS"
}
```

**Response:**

```json
{
  "id": 1,
  "accountHolderName": "Test User",
  "balance": 1000.0,
  "accountType": "SAVINGS"
}
```

#### `GET /api/accounts`

Get all accounts for the authenticated user.

**Response:**

```json
[
  {
    "id": 1,
    "accountHolderName": "Test User",
    "balance": 1000.0,
    "accountType": "SAVINGS"
  }
]
```

#### `GET /api/accounts/{id}`

Get an account by its ID. The user must be the owner of the account.

**Response:**

```json
{
  "id": 1,
  "accountHolderName": "Test User",
  "balance": 1000.0,
  "accountType": "SAVINGS"
}
```

#### `PUT /api/accounts/{id}/deposit`

Deposit an amount into an account.

**Request Body:**

```json
{
  "amount": 500.0
}
```

**Response:**

```json
{
  "id": 1,
  "accountHolderName": "Test User",
  "balance": 1500.0,
  "accountType": "SAVINGS"
}
```

#### `PUT /api/accounts/{id}/withdraw`

Withdraw an amount from an account.

**Request Body:**

```json
{
  "amount": 200.0
}
```

**Response:**

```json
{
  "id": 1,
  "accountHolderName": "Test User",
  "balance": 1300.0,
  "accountType": "SAVINGS"
}
```

#### `DELETE /api/accounts/{id}`

Delete an account by its ID.

**Response:**

```
Account deleted successfully
```
