# 🔗 UrlShortener: Real-Time URL Shortening and Analytics Platform

**UrlShortener** is a powerful, modern URL shortening service that allows users to instantly generate short links, securely manage them, and track comprehensive real-time analytics. Built with a robust Spring Boot backend and a responsive React frontend, UrlShortener is designed to be fast, scalable, and user-friendly.

## 📚 Table of Contents

* [Overview](#overview)
* [Key Features](#key-features)
* [Tech Stack](#tech-stack)
* [Getting Started](#getting-started)
* [Screenshots](#screenshots)
* [Contributions](#contribution)
* [License](#license)
* [Author](#author)

## Overview

UrlShortener provides a complete solution for link management. Beyond just shortening long URLs, it offers detailed, real-time insights into link usage, including the total number of clicks and individual user interactions. The platform ensures a personalized and secure experience through full user authentication and a modern, fast user interface.

## Key Features

  * **✅ Shorten Links:** Instantly generate concise, shareable short URLs for any long link.
  * **📈 Analytics Dashboard:** Access a powerful dashboard to track the performance of every link, showing the total number of clicks.
  * **👤 User Tracking:** Monitor granular link usage, tracking individual clicks associated with authenticated users.
  * **🔒 User Authentication:** Secure login and signup functionality powered by JWT for a personalized and private link management experience.
  * **✨ Modern UI:** A smooth, reactive frontend built with ReactJS for an excellent user experience.
  * **🚀 Fast & Scalable:** A robust and efficient backend powered by Spring Boot ensures high performance and scalability.

## Tech Stack

UrlShortener is a full-stack application leveraging modern, industry-standard technologies:

### Backend (API)

| Technology | Purpose |
| :--- | :--- |
| **Spring Boot** | Core framework for the RESTful API, providing speed and stability. |
| **Spring Security** | Handling authorization, user authentication, and securing endpoints. |
| **JWT Authentication** | Secure, stateless authentication for API communication. |
| **MySQL 8.0** | Relational database for persistence of short links, user data, workflow state and click analytics. |

### Frontend (UI)

| Technology | Purpose |
| :--- | :--- |
| **ReactJS** | Library for building the responsive and dynamic Single Page Application (SPA). |
| **React Router** | Managing client-side routing and navigation within the application. |
| **Axios** | Efficient, promise-based HTTP client for communicating with the Spring Boot API. |

### DevOps

| Technology                      | Purpose                                                                                                                                     |
| :------------------------------ | :------------------------------------------------------------------------------------------------------------------------------------------ |
| **Docker**                      | Containerization platform to package applications with all dependencies, ensuring consistency across environments.                          |
| **Docker Compose**              | Tool for defining and managing multi-container Docker applications using a single configuration file.                                       |
| **Kubernetes (K8s)**            | Container orchestration platform for automated deployment, scaling, and management of containerized applications.                           |
| **Jenkins**                     | Continuous Integration and Continuous Deployment (CI/CD) automation server for building, testing, and deploying applications.               |
| **Terraform**                   | Infrastructure as Code (IaC) tool for provisioning and managing cloud resources in a consistent and automated way.                          |
| **Ansible**                     | Configuration management and automation tool for provisioning servers and deploying applications efficiently.                               |
| **Prometheus**                  | Monitoring and alerting toolkit designed for collecting and querying time-series metrics from applications and infrastructure.              |
| **Grafana**                     | Visualization and analytics platform used to create dashboards and monitor metrics collected by Prometheus and other sources.               |
| **Google Cloud Platform (GCP)** | Cloud service provider used for hosting, scaling, and managing applications and infrastructure.                                             |
| **Render**                      | Cloud platform for hosting and deploying web applications and services with automated builds and scalability.                               |
| **Vercel**                      | Frontend deployment platform optimized for React and other JavaScript frameworks, enabling fast global delivery and easy CI/CD integration. |


## Getting Started

Follow these steps to set up and run UrlShortener locally.

### Prerequisites

  * Java Development Kit (JDK 21)
  * Node.js 20+ and npm
  * A running MySQL 8.0 instance (Docker Compose is provided).
  * Maven (for Spring Boot build)

### 1\. Database Setup

1.  Update the database connection properties in the backend's `application.properties` file with your credentials:
    ```yaml
    spring.datasource.url: jdbc:postgresql://localhost:5432/urlshortener_db
    spring.datasource.username: your_db_user
    spring.datasource.password: your_db_password
    ```

### 2\. Backend Setup

1.  Navigate to `Url-Shortner-sb`.
2.  Build the project using Maven:
    ```bash
    ./mvnw clean test
    ```
3.  Run the application:
    ```bash
    java -jar target/Url-Shortner-sb-0.0.1-SNAPSHOT.jar
    # OR if using an IDE like IntelliJ, run the main application class.
    ```
    The API should start running on `http://localhost:9090`.

### 3\. Frontend Setup

1.  Navigate to `Url-Shortner-Frontend`.
2.  Install the dependencies:
    ```bash
    npm ci
    ```
3.  Start the development server:
    ```bash
    npm run dev
    ```
    The React application should open in your browser at `http://localhost:5173` (or the configured port)

## Contribution

We welcome contributions\! If you have suggestions for new features, bug fixes, or improvements, please:

1.  Fork the repository.
2.  Create a new feature branch (`git checkout -b feature/amazing-feature`).
3.  Commit your changes (`git commit -m 'feat: Add amazing feature'`).
4.  Push to the branch (`git push origin feature/amazing-feature`).
5.  Open a Pull Request.

## License

This project is licensed under the **MIT License** – see the [LICENSE](./LICENSE) file for details.

## Author

Abhishek Rajput




---

# Agentic Engineering Assignment Extension

This branch extends the existing URL Shortener with a controlled agentic SDLC orchestrator. See:
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — architecture, DAG, governance, decisions and production backlog.
- [`docs/SCENARIOS.md`](docs/SCENARIOS.md) — greenfield, brownfield and ambiguous walkthroughs with API examples.
- [`docs/TESTING_AND_RISKS.md`](docs/TESTING_AND_RISKS.md) — validation plan, risks, mitigations and limitations.
- [`docs/FINAL_ENGINEERING_SUMMARY.md`](docs/FINAL_ENGINEERING_SUMMARY.md) — assignment coverage, assumptions, validation status and remaining gaps.

## Run locally

Prerequisites: Java 21, Docker Desktop, and Node.js (for the React UI).

```bash
cd Url-Shortner-sb
docker compose up --build
```

Backend: `http://localhost:9090`; health: `http://localhost:9090/actuator/health`.
The compose defaults are for local development only. Override database credentials and `JWT_SECRET` before any shared or deployed environment. Generate a secret with:

```bash
openssl rand -base64 48
```

For the React UI, copy `Url-Shortner-Frontend/.env.example` to `.env`, then:

```bash
cd Url-Shortner-Frontend
npm ci
npm run dev
```

## Agentic API

All `/api/agentic/**` and `/api/v2/workflows/**` endpoints require JWT authentication.

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/agentic/runs` | Start a GREENFIELD, BROWNFIELD, or AMBIGUOUS run |
| GET | `/api/agentic/runs/{runId}` | Inspect task DAG, outputs, decisions and audit trail |
| POST | `/api/agentic/runs/{runId}/clarify` | Resolve an ambiguity gate |
| POST | `/api/agentic/runs/{runId}/approve` | Human approval to proceed to implementation |
| POST | `/api/agentic/runs/{runId}/reject` | Reject and stop downstream execution |
| POST | `/api/agentic/runs/{runId}/replan` | Invalidate prior task outputs and re-plan |
| GET | `/api/agentic/metrics` | Workflow reliability counters |

The orchestrator is a runnable deterministic prototype, not a connected LLM agent. Workflow state is in memory and is lost on restart. The release-readiness stage never deploys. These boundaries are intentional and documented.


### Durable v2 workflow API

The v2 API persists runs, task outputs and audit events in MySQL. All endpoints require JWT authentication and are scoped to the authenticated owner.

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v2/workflows` | Create GREENFIELD, BROWNFIELD or AMBIGUOUS workflow |
| GET | `/api/v2/workflows/{id}` | Read workflow DAG, outputs and audit history |
| GET | `/api/v2/workflows/metrics` | Read owner-scoped run counters |
| POST | `/api/v2/workflows/{id}/clarifications` | Resolve an ambiguity and regenerate upstream artifacts |
| POST | `/api/v2/workflows/{id}/approve` | Approve the requirement and current upstream artifact scope |
| POST | `/api/v2/workflows/{id}/reject` | Reject at the approval gate |
| POST | `/api/v2/workflows/{id}/replans` | Invalidate stale artifacts and re-plan |
