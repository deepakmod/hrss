# HRSS - Enterprise HRSS Backend System

An Enterprise-Grade, highly secure, and observable backend application built with **Java 21** and **Spring Boot 3.3.4**.

## Features
- **Security:** OAuth2 (Google & Azure), JWT Stateless Sessions, Token-Bucket Rate Limiting (Bucket4j).
- **Observability:** Centralized Logging (Loki, Promtail, Grafana), Actuator + Prometheus metrics.
- **Persistence:** MySQL 9.7 with Flyway Database Migrations and Redis Caching.
- **Testing:** Fully automated integration tests using Testcontainers.
- **Infrastructure:** Docker Compose clusters with Nginx Reverse Proxies and non-root application containers.

## Quick Start & Documentation

Please refer to our comprehensive **[SETUP.md](SETUP.md)** for detailed, step-by-step instructions on:
1. Local Development Setup (Using `.env.dev` and Maven)
2. Running Integration Tests via Testcontainers
3. Production Deployment (Using Docker Compose, Nginx, and Grafana)

## API Documentation
Once the application is running in the `dev` profile, you can access the interactive Swagger UI documentation at:
`http://localhost:8080/swagger-ui.html`
*(Note: Swagger is explicitly disabled in the `prod` profile for security reasons).*
