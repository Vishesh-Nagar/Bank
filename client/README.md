# Bank Application - Frontend

This is the React frontend for the Bank Application. It provides a user-friendly interface for customers to manage their bank accounts. It communicates with the Spring Boot backend via REST API calls to handle user authentication, account management, and transactions.

## Features

*   User registration and login with authentication.
*   View all bank accounts associated with the logged-in user.
*   Create new bank accounts.
*   View details of a specific account.
*   Perform deposits and withdrawals on an account.
*   Delete an account.

## Technologies Used

*   **React:** A JavaScript library for building user interfaces.
*   **TypeScript:** A typed superset of JavaScript that compiles to plain JavaScript.
*   **Vite:** A fast build tool and development server for modern web projects.
*   **REST API:** Communication with the backend is done via RESTful API calls.

## Installation and Setup

### Prerequisites

*   Node.js 18.x or later
*   npm 9.x or later

### Running the Frontend

1.  **Navigate to the client directory:**
    ```bash
    cd client
    ```

2.  **Install dependencies:**
    Run the following command to install the necessary packages:
    ```bash
    npm install
    ```

3.  **Run the development server:**
    To start the frontend development server, run:
    ```bash
    npm run dev
    ```
    The frontend will be available at `http://localhost:5173`.

## Communicating with the Backend

The frontend expects the backend server to be running on `http://localhost:8080`. The backend server provides the necessary REST APIs for the application to function. Please refer to the `server/README.md` for instructions on how to set up and run the backend.
