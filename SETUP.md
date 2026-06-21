# HRSS - Comprehensive Setup & Deployment Guide

This document contains step-by-step instructions on how to set up, run, test, and deploy the HRSS project for both Development and Production environments.

---

## 1. Local Development Setup

### Prerequisites
- **Java 21** installed (`java -version`)
- **Maven** (or use the included `./mvnw` wrapper)
- **Docker Desktop** or Docker Engine installed (`docker --version`)

### Step 1.1: Database Environment (Docker)
Docker uses `.env` files to configure your local databases.
1. Copy the `.env.dev` template to `.env`:
   ```bash
   cp .env.dev .env
   ```

### Step 1.2: Application Properties (Spring Boot)
Spring Boot reads from `src/main/resources/application-dev.yml` for your local application configuration.
1. Open `application-dev.yml` in your IDE.
2. Fill in the placeholder values for your database passwords (`dev_root_password`) and OAuth credentials (`client-id`, `client-secret`).
*Note: If you add real production secrets here, make sure to add `application-dev.yml` to your `.gitignore` file!*

### Step 1.3: Start Services (MySQL & Redis)
Instead of installing databases locally, we use `docker-compose.yml` to spin them up instantly.
```bash
docker compose -f docker-compose.yml up -d mysql redis
```
*(Wait a few seconds for the databases to initialize)*

### Step 1.3: Run the Application
You can run the application directly via Maven. The `dev` profile is active by default.
```bash
./mvnw spring-boot:run
```
The application will be available at `http://localhost:8080`.

### Step 1.4: Run Automated Tests
We use **Testcontainers** for our integration tests, which automatically spins up temporary Docker containers for MySQL and Redis during the test phase.
Make sure Docker is running, then execute:
```bash
./mvnw clean test
```

---

## 2. Production Deployment Guide

The production environment is fully containerized, secure, and observable. It includes Nginx (Reverse Proxy), Promtail/Loki/Grafana (Logging), and the Spring Boot application.

### Prerequisites
- A Linux Server (Ubuntu 22.04+ recommended)
- **Docker Engine** & **Docker Compose V2** installed.

### Step 2.1: Clone & Configure
1. Clone the repository onto your server.
2. Copy the production environment template:
   ```bash
   cp .env.prod .env
   ```
3. **CRITICAL:** Edit the `.env` file!
   - Change `MYSQL_ROOT_PASSWORD` and `MYSQL_PASSWORD` to secure strings.
   - Set a strong `JWT_SECRET`.
   - Set `GRAFANA_PASSWORD`.
   - Set `CORS_ORIGINS` to your exact frontend domain (e.g., `https://app.yourdomain.com`).

### Step 2.2: Compile the Application
Since we don't want to install Java on the server itself, our Dockerfile builds the `.jar` using a multi-stage process. No local Java installation is required!

### Step 2.3: Start the Production Stack
Run the following command to build the image and spin up the entire cluster:
```bash
docker compose -f docker-compose.prod.yml up -d --build
```

### Step 2.4: Verify the Deployment
To ensure everything started correctly, check the status of your containers:
```bash
docker compose -f docker-compose.prod.yml ps
```
You should see `Up (healthy)` for all services.

### Step 2.5: Accessing the Services
- **Backend API:** `http://your-server-ip/` (Nginx proxies port 80 directly to the app on 8080).
- **Grafana Dashboards:** `http://your-server-ip:3000` (Login with `admin` and the password you set in `.env`).

### Step 2.6: Security Hardening (SSL/TLS)
For a true 10/10 production deployment, you must enable HTTPS. 
1. Map a domain name to your server's IP address.
2. Install `certbot`.
3. Modify `nginx/conf.d/default.conf` to configure your SSL certificates.

---

## 3. Useful Commands

- **Stop all services:** `docker compose -f docker-compose.prod.yml down`
- **View logs directly (if not using Grafana):** `docker compose -f docker-compose.prod.yml logs -f app`
- **Rebuild after making code changes:** `docker compose -f docker-compose.prod.yml up -d --build app`
