# Cypress Project ReadMe

This document provides an overview of the Cypress project components and instructions for running and deploying the application.

## Project Structure

-   `backend/`: Contains the Java Spring Boot backend application.
    -   `src/main/java/app/`: Main application source code (controllers, services, models, repositories).
    -   `src/main/resources/`: Application configuration file.
    -   `pom.xml`: Maven project configuration.
    -   `target/`: Contains the built application artifact (e.g., `cypress-backend-0.0.1-SNAPSHOT.jar`).
    -   `cypress.db`: SQLite database file used by the backend.
-   `frontend/`: Contains the static web frontend application.
    -   `*.html`: HTML pages for different parts of the application (index, report submission, tracking, admin, feedback).
    -   `css/`: Stylesheets for the frontend.
    -   `js/`: JavaScript files for frontend logic (form handling, map integration, admin functions, report tracking, feedback).
-   `microservice/duplicate-detection/`: Contains the Go microservice for detecting duplicate reports.
    -   `main.go`: The source code for the microservice.
    -   `go.mod`, `go.sum`: Go module files.

## ✨ Features

### 📍 Interactive Map-Based Report Creation
Click anywhere on the map to select a location and create detailed community issue reports with categorization.

![Map Report Creation](./Map_Report_Creation.gif)

### 🔍 Report Tracking & Management  
Track your submitted reports by entering your email address. Browse through all community reports with scrollable interface.

![Report Tracking](./Report_Tracking.gif)

### 🤖 Duplicate Detection
Advanced microservice automatically detects potential duplicate reports using geographical proximity and text similarity algorithms.

![Duplicate Detection](./Duplicate_Detection.gif)

### 📊 Comprehensive Admin Dashboard
Real-time analytics, interactive charts, and powerful administrative tools for managing community reports.

![Admin Dashboard Overview](./Admin_Dashboard_Overview.gif)

## Components

### 1. Backend (Spring Boot)

-   **Description:** Handles core application logic, including report submission, retrieval, feedback, administration, and user subscriptions. It exposes a REST API for the frontend and potentially other clients. It uses a SQLite database (`cypress.db`) for persistence.
-   **Technology:** Java 11, Spring Boot 2.7.4, Spring Data JPA, Spring Web, SQLite.
-   **API Endpoints:** Provided by controllers in `backend/src/main/java/app/controller/`:
    -   `ReportController`: Manages report creation, retrieval, and updates.
    -   `FeedbackController`: Handles user feedback submissions.
    -   `AdminController`: Provides administrative functionalities (e.g., managing reports, viewing stats).
    -   `SubscriptionController`: Manages user subscriptions (for report updates).
    -   `HomeController`: Serves the main index page at the root path (`/`).
-   **Static File Serving:** The backend is configured to serve frontend static files from the `../frontend/` directory relative to the JAR location. The `HomeController` ensures that accessing the root URL (`/`) properly serves the `index.html` page.
-   **Build:** Navigate to the `backend/` directory and run:
    ```bash
    mvn clean package
    ```
    This generates the executable JAR file in `backend/target/`.
-   **Run:** After building, run the application using:
    ```bash
    java -jar target/cypress-backend-0.0.1-SNAPSHOT.jar
    ```
    The backend runs on port 8080 by default and serves the frontend files directly from the `../frontend/` directory.

### 2. Frontend (HTML/CSS/JS)

-   **Description:** Provides the user interface for interacting with the application. Users can submit reports, track existing reports, provide feedback, and administrators can manage the system. It uses JavaScript for dynamic behavior and API interactions.
-   **Technology:** HTML5, CSS3, JavaScript.
-   **Key Files:**
    -   `index.html`: Main landing page.
    -   `report.html`: Form for submitting new reports.
    -   `track.html`: Page for tracking the status of submitted reports.
    -   `admin.html`: Interface for administrators.
    -   `feedback.html`: Form for submitting feedback.
    -   `js/map.js`: Integrates with the mapping library Leaflet.js for displaying report locations.
-   **Deployment:** The frontend is served directly by the Spring Boot backend. Simply run the backend, and the frontend will be accessible (via `http://localhost:8080`).

### 3. Duplicate Detection Microservice (Go)

-   **Description:** A standalone service written in Go that checks incoming reports for potential duplicates based on geographical proximity (using Haversine distance) and description similarity (using Jaccard similarity) against reports stored in the main application's SQLite database (`backend/cypress.db`).
-   **Technology:** Go, net/http, go-sqlite3 driver.
-   **API Endpoint:** Exposes a `/detect` endpoint via HTTP.
-   **Run:**
    1.  Navigate to the `microservice/duplicate-detection/` directory.
    2.  Ensure Go is installed.
    3.  Run the service:
        ```bash
        go run main.go
        ```
    The microservice runs on port 8081 by default.
-   **Note:** This service directly accesses the `backend/cypress.db` file. Ensure the path in `main.go` (`../../backend/cypress.db`) is correct relative to where the microservice is run, or modify the path accordingly.

## Delivery Package

A typical delivery package would include:

1.  **Backend JAR:** `backend/target/cypress-backend-0.0.1-SNAPSHOT.jar`
2.  **Frontend Files:** The entire `frontend/` directory.
3.  **Duplicate Detection Microservice:** The compiled executable for `microservice/duplicate-detection/main.go` (or the source code and instructions to build/run it).
4.  **Database:** The `backend/cypress.db` file (if deploying with existing data, otherwise the backend will likely create it).

## Deployment Steps

1.  **Build Backend:** Run `mvn clean package` in the `backend` directory.
2.  **Place Frontend:** Ensure the `frontend` directory is located relative to the backend JAR as expected by the backend's resource handler configuration (i.e., `../frontend/` relative to the JAR's location, so typically place the JAR in `backend/target/` and the `frontend` directory in the project root).
3.  **Run Microservice:** Start the Go microservice (`go run main.go` in `microservice/duplicate-detection/`). Ensure it can access `backend/cypress.db`.
4.  **Run Backend:** Start the backend JAR (`java -jar backend/target/cypress-backend-0.0.1-SNAPSHOT.jar`).
5.  **Access Application:** Open a web browser to `http://localhost:8080`.

**Important:** The backend and microservice ports (8080 and 8081) and the database path used by the microservice are hardcoded. Modify the source code or use configuration files/environment variables for more flexible deployment scenarios.
