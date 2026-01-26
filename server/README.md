# Bank Application - Backend

This is the Spring Boot backend for the Bank Application. It provides a RESTful API for the frontend to interact with. It handles business logic, data persistence, and authentication.

## Features

*   **User Management:** Handles user registration and authentication using Spring Security.
*   **Account Management:** Provides endpoints for creating, viewing, and deleting bank accounts.
*   **Transactions:** Includes endpoints for depositing and withdrawing funds from an account.
*   **Data Persistence:** Uses Spring Data JPA with Hibernate to interact with a MySQL database.

## Technologies Used

*   **Java:** The primary programming language for the backend.
*   **Spring Boot:** A framework for creating stand-alone, production-grade Spring-based applications.
*   **Spring Security:** Provides authentication and authorization for the application.
*   **Spring Data JPA (Hibernate):** Used for data persistence and ORM.
*   **MySQL:** The relational database used to store application data.
*   **Maven:** A build automation tool used for managing the project's build, reporting, and documentation.

## Installation and Setup

### Prerequisites

*   Java 21 or later
*   Maven 3.x
*   MySQL 8.x

### Database Setup

1.  **Create a database:**
    Make sure you have a MySQL server running and create a database named `bank`.

2.  **Configure database credentials:**
    Open the `src/main/resources/application.yaml` file and update the `spring.datasource.username` and `spring.datasource.password` properties with your MySQL credentials.

### Running the Backend

1.  **Navigate to the server directory:**
    ```bash
    cd server
    ```

2.  **Run the backend server:**
    Use the Maven wrapper to run the application:
    ```bash
    ./mvnw spring-boot:run
    ```
    The backend server will start on port `8080`.

## API Endpoints

The backend provides the following REST API endpoints.

### User Endpoints

*   `POST /api/users`: Create a new user.
*   `POST /api/users/login`: Authenticate a user and get a token.

### Account Endpoints

All account endpoints require authentication.

*   `POST /api/accounts`: Create a new bank account for the authenticated user.
*   `GET /api/accounts`: Get all accounts for the authenticated user.
*   `GET /api/accounts/{id}`: Get an account by its ID.
*   `PUT /api/accounts/{id}/deposit`: Deposit an amount into an account.
*   `PUT /api/accounts/{id}/withdraw`: Withdraw an amount from an account.
*   `DELETE /api/accounts/{id}`: Delete an account by its ID.

For detailed request and response formats, please refer to the code or use a tool like Postman to interact with the API.
