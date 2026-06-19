<div align="center">

# 🛡️ Aegis Vault

### A Production-Grade, Full-Stack Secure Password Manager

[![Java](https://img.shields.io/badge/Java-21_LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Multi--Stage-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Render](https://img.shields.io/badge/Deployed_on-Render-46E3B7?style=for-the-badge&logo=render&logoColor=white)](https://render.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](LICENSE)

<br/>

> Zero-knowledge architecture · AES-256 encryption · Real-time breach detection · JWT authentication

<br/>

**[🔗 Live Demo](https://aegis-vault-frontend.onrender.com)** &nbsp;|&nbsp; **[📡 API Docs](#-api-reference)** &nbsp;|&nbsp; **[🚀 Quick Start](#-quick-start)**

<br/>

---

</div>

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Tech Stack](#️-tech-stack)
- [Architecture](#-architecture)
- [Security Design](#-security-design)
- [Quick Start](#-quick-start)
- [API Reference](#-api-reference)
- [Deployment](#-deployment-render)
- [Project Structure](#-project-structure)
- [Environment Variables](#-environment-variables)

---

## 🔍 Overview

**Aegis Vault** is a production-ready, full-stack password management application built to enterprise security standards. It encrypts every stored credential with **AES-256** before it touches the database, authenticates users with cryptographically signed **JWT tokens**, and monitors all stored passwords against the **HaveIBeenPwned** threat intelligence database in real time.

The frontend is built with **zero external JavaScript dependencies** — pure HTML5, CSS3, and ES6 — while the backend is a hardened **Spring Boot 3** REST API containerized with a **multi-stage Docker** build and deployed on **Render**.

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🔐 **AES-256 Encryption** | Raw passwords never reach the database — every credential is symmetrically encrypted before storage |
| 🎫 **JWT Authentication** | Stateless, cryptographically signed session tokens with configurable expiry |
| ⚠️ **Live Breach Detection** | HaveIBeenPwned API integration scans all stored passwords against billions of known data breaches |
| 🛡️ **Rate Limiting** | Request throttling on all authentication endpoints to block brute-force and credential stuffing attacks |
| 🎲 **Password Generator** | On-demand entropy-focused password generator with customizable length and character complexity |
| 🔍 **Vault Search** | Instant client-side keyword search across all stored platform credentials |
| 📤 **CSV Export** | One-click, secure data portability export of the full vault to CSV |
| 🌗 **Light / Dark Mode** | Theme preference persisted across sessions via localStorage |
| 📱 **Responsive UI** | Fully responsive layout across desktop, tablet, and mobile viewports |

---

## 🛠️ Tech Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 LTS | Core runtime |
| Spring Boot | 3.2.5 | Application framework |
| Spring Security | 6.x | JWT filter chain, CORS, auth |
| Spring Data JPA | 3.x | ORM and data access layer |
| PostgreSQL | 16 | Production database |
| H2 Database | — | In-memory engine for local dev |
| Lombok | Latest | Boilerplate reduction |
| Maven | 3.9+ | Build and dependency management |

### Frontend
| Technology | Purpose |
|------------|---------|
| HTML5 / CSS3 | Structure and styling |
| Vanilla JavaScript (ES6+) | API communication, DOM management |
| CSS Custom Properties | Theming (dark / light mode) |
| Fetch API | Async HTTP requests |
| Web Crypto API | Client-side password generation entropy |

### Infrastructure
| Service | Role |
|---------|------|
| Docker (Multi-stage) | Build optimisation and containerisation |
| Render Web Services | Backend hosting |
| Render Static Sites | Frontend CDN delivery |
| Render Managed PostgreSQL | Production database |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT BROWSER                           │
│          HTML5 · CSS3 · Vanilla JS · Fetch API                  │
└───────────────────────────┬─────────────────────────────────────┘
                            │  HTTPS (JWT Bearer Token)
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SPRING BOOT 3.2.5 API                        │
│                                                                 │
│   ┌──────────────────┐    ┌─────────────────┐                   │
│   │ RateLimitFilter  │───►│   JwtFilter     │                   │
│   └──────────────────┘    └────────┬────────┘                   │
│                                    │ Verified Principal         │
│                           ┌────────▼────────┐                   │
│                           │   Controllers   │                   │
│                           │ Auth │  Vault   │                   │
│                           └────────┬────────┘                   │
│                                    │                            │
│                    ┌───────────────▼──────────────┐             │
│                    │       Service Layer           │            │
│                    │  AES-256 Encrypt / Decrypt    │            │
│                    │  HIBP Breach Detection        │            │
│                    └───────────────┬──────────────┘             │
│                                    │                            │
└────────────────────────────────────┼────────────────────────────┘
                                     │ JPA / JDBC
                                     ▼
                    ┌────────────────────────────────┐
                    │      PostgreSQL Database       │
                    │  (Cipher text only — no plain- │
                    │   text passwords ever stored)  │
                    └────────────────────────────────┘
```

### Request Flow
```
Request ──► CORS Check ──► Rate Limit ──► JWT Validation ──► Controller
                                                                 │
                                          AES-256 Decrypt ◄───── │
                                                │                │
                                         Response ──────────────►│
```

---

## 🔒 Security Design

### Encryption at Rest
All passwords are encrypted with **AES-256** (Advanced Encryption Standard, 256-bit key) before being written to the database. The encryption key is injected at runtime from an environment variable — it is never committed to source control or hardcoded.

```
User Password  ──►  AES-256 Encrypt (system key)  ──►  Cipher Text  ──►  Database
Database       ──►  AES-256 Decrypt (system key)  ──►  Plain Text   ──►  Response
```

### Authentication
- Passwords are hashed using **BCrypt** (adaptive cost factor) before storage
- Login issues a signed **JWT** (HS256) valid for 24 hours
- Every protected endpoint validates the JWT signature before processing

### Defence in Depth

```
Layer 1  →  CORS hardening        (only verified frontend origin allowed)
Layer 2  →  Rate limiting         (blocks brute-force on /auth/login)
Layer 3  →  JWT validation        (stateless, signature-verified sessions)
Layer 4  →  IDOR protection       (service layer checks resource ownership)
Layer 5  →  AES-256 encryption    (credentials encrypted before DB write)
Layer 6  →  Profile isolation     (dev / prod configs separated)
Layer 7  →  Secret injection      (all keys via environment variables)
```

---

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Git

### Run Locally

```bash
# 1. Clone the repository
git clone https://github.com/your-username/aegis-vault.git
cd aegis-vault

# 2. Run with the dev profile (uses H2 in-memory database — no setup needed)
mvn spring-boot:run

# 3. Open the frontend
# Navigate to: http://localhost:8080
```

> **Note:** The dev profile auto-creates an in-memory H2 database on startup and drops it on shutdown. No PostgreSQL installation is needed for local development.

### Run with Docker

```bash
# Build the image
docker build -t aegis-vault .

# Run the container (dev mode)
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  aegis-vault
```

---

## 📡 API Reference

All protected endpoints require the `Authorization` header:
```
Authorization: Bearer <your_jwt_token>
```

### Authentication

#### Register
```http
POST /api/auth/register
Content-Type: application/json
```
```json
{
  "username": "vedant",
  "email": "vedant@example.com",
  "password": "SecurePass@123"
}
```
**Response** `201 Created`
```json
{
  "message": "User registered successfully!",
  "data": null
}
```

---

#### Login
```http
POST /api/auth/login
Content-Type: application/json
```
```json
{
  "username": "vedant",
  "password": "SecurePass@123"
}
```
**Response** `200 OK`
```json
{
  "message": "Login successful",
  "data": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### Vault Operations

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|:---:|
| `GET` | `/api/passwords` | Retrieve all credentials (decrypted) | ✅ |
| `POST` | `/api/passwords` | Save and encrypt a new credential | ✅ |
| `PUT` | `/api/passwords/{id}` | Update an existing credential | ✅ |
| `DELETE` | `/api/passwords/{id}` | Permanently delete a credential | ✅ |
| `GET` | `/api/passwords/search?keyword=` | Search credentials by platform | ✅ |
| `GET` | `/api/passwords/generate?length=16&useSpecial=true` | Generate a secure password | ✅ |
| `POST` | `/api/passwords/check-strength` | Analyse password strength | ✅ |
| `GET` | `/api/passwords/scan-breaches` | Run HIBP breach scan on all passwords | ✅ |
| `GET` | `/api/passwords/breached-passwords` | List only breached credentials | ✅ |

#### Save a Password
```http
POST /api/passwords
Authorization: Bearer <token>
Content-Type: application/json
```
```json
{
  "platform": "Netflix",
  "loginUsername": "vedant@example.com",
  "password": "MyNetflixPass@99"
}
```
**Response** `201 Created`
```json
{
  "message": "Password saved successfully!",
  "data": null
}
```

#### Generate a Password
```http
GET /api/passwords/generate?length=20&useSpecial=true
Authorization: Bearer <token>
```
**Response** `200 OK`
```json
{
  "message": "Success",
  "data": {
    "password": "xK!9mP#2rL@vQ8nT$wYz",
    "length": 20,
    "useSpecial": true
  }
}
```

---

## ☁️ Deployment (Render)

This project deploys to **Render** using a multi-stage Docker build.

### Step 1 — Build Configuration

The `Dockerfile` in the project root handles both build and runtime:

```dockerfile
# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx400m", "-jar", "app.jar"]
```

### Step 2 — Render Web Service Settings

| Field | Value |
|-------|-------|
| Language | Docker |
| Build Command | *(leave empty — Dockerfile handles it)* |
| Start Command | *(leave empty — Dockerfile ENTRYPOINT handles it)* |
| Instance Type | Free |

### Step 3 — Environment Variables

Set the following in the Render Web Service dashboard:

| Variable | Description |
|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | `jdbc:postgresql://<render-host>:5432/<db-name>` |
| `DB_USER` | Your Render PostgreSQL username |
| `DB_PASSWORD` | Your Render PostgreSQL password |
| `JWT_SECRET` | 64-character random hex string (`openssl rand -hex 32`) |
| `JWT_EXPIRATION` | `86400000` (24 hours in ms) |

---

## 📁 Project Structure

```
aegis-vault/
│
├── src/
│   └── main/
│       ├── java/com/vault/app/
│       │   ├── controller/
│       │   │   ├── AuthController.java        # Login / Register endpoints
│       │   │   ├── VaultController.java       # Password CRUD endpoints
│       │   │   └── GlobalExceptionHandler.java
│       │   ├── service/
│       │   │   ├── UserService.java
│       │   │   ├── VaultService.java          # AES-256 encrypt/decrypt logic
│       │   │   └── BreachDetectionService.java # HIBP integration
│       │   ├── entity/
│       │   │   ├── User.java
│       │   │   └── SavedPassword.java
│       │   ├── dto/
│       │   │   ├── ApiResponse.java
│       │   │   ├── UserLoginDTO.java
│       │   │   ├── PasswordRequestDTO.java
│       │   │   └── BreachScanResultDTO.java
│       │   ├── security/
│       │   │   ├── JwtFilter.java
│       │   │   └── SecurityConfig.java
│       │   └── util/
│       │       ├── PasswordGenerator.java
│       │       └── PasswordStrengthChecker.java
│       │
│       └── resources/
│           ├── application.properties          # Profile selector
│           ├── application-dev.properties      # H2 local config
│           └── application-prod.properties     # PostgreSQL Render config
│
├── frontend/
│   ├── index.html                              # Landing page
│   ├── login.html
│   ├── register.html
│   ├── vault.html                              # Main dashboard
│   ├── css/
│   │   └── style.css
│   └── js/
│       ├── api.js                              # Backend communication layer
│       ├── auth.js                             # Login / Register logic
│       └── vault.js                            # Vault management logic
│
├── Dockerfile                                  # Multi-stage Docker build
├── .dockerignore
└── pom.xml
```

---

## 🔧 Environment Variables

| Variable | Profile | Description | Example |
|----------|---------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | All | Active Spring profile | `prod` / `dev` |
| `DB_URL` | prod | JDBC PostgreSQL connection URL | `jdbc:postgresql://host:5432/db` |
| `DB_USER` | prod | Database username | `vaultuser` |
| `DB_PASSWORD` | prod | Database password | `••••••••` |
| `JWT_SECRET` | prod | 64-char hex signing key | `openssl rand -hex 32` |
| `JWT_EXPIRATION` | prod | Token validity in milliseconds | `86400000` |
| `PORT` | prod | Auto-injected by Render | `8080` |

---

## 📜 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

Built by **Vedant** · B.Tech Computer Science · Uttaranchal University

[![GitHub](https://img.shields.io/badge/GitHub-your--username-181717?style=flat-square&logo=github)](https://github.com/your-username)

</div>
