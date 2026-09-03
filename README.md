# GeoVerity
### A Secure Geolocation & Digital Photographic Evidence Authentication Platform

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Android](https://img.shields.io/badge/Android-Kotlin_%7C_Compose-blue.svg)](https://developer.android.com/)
[![ECDSA](https://img.shields.io/badge/ECDSA-NIST_P--256-indigo.svg)](https://csrc.nist.gov/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

---

## 1. Executive Summary

**GeoVerity** is a mission-critical digital evidence authentication platform designed for forensic, legal, insurance, regulatory, and field inspection workflows. It allows an authorized Android client to capture photographs and cryptographically bind:
- **Image Pixel Content**
- **GPS Coordinates & Location Name**
- **Trusted Authoritative Server Timestamp**
- **Device & Application Identity**
- **Unique Verification ID (`SGA-<UUID>`)**

The system proves whether a submitted digital photograph is the **exact, unmodified evidence originally captured through GeoVerity**.

---

## 2. Key Architecture Decisions

- **Zero User Login**: The Android app has **NO user login or registration screens**. Authorization is performed via `X-API-Key` securely held in Android Keystore.
- **Server Authority**: The mobile app is a controlled capture client, **not an authoritative signer**. All digital signatures are issued exclusively by the backend using server-side **ECDSA NIST P-256 (SHA256withECDSA)**.
- **Dedicated Metadata Footer**: The original photograph area remains clean and unobstructed. A dedicated metadata footer and QR identifier are rendered at the bottom.
- **Embedded Verification ID**: Stored inside JPEG `COM` segments (`0xFF, 0xFE`) before SHA-256 hashing.
- **Monotonic Offline Time Reconciliation**: Detects clock tampering attacks via `SystemClock.elapsedRealtime()`; flags and rejects any capture with `TIME_ANOMALY` if clock deviation exceeds 120 seconds.
- **Zero Server Image Storage**: Only cryptographic proofs (`sha256_hash`, `canonical_metadata`, `verification_id`, `ecdsa_signature`, `audit_logs`) are retained in relational tables. No image blobs or permanent URLs are stored.
- **Zero-Login Third-Party Verification**: Third parties upload **ONLY the image file**. Verification auto-extracts the ID, queries PostgreSQL, validates ECDSA, and recalculates SHA-256 for an exact bit match.

---

## 3. Project Structure

```
GeoVerity/
├── android/                   # Android Kotlin Jetpack Compose Mobile App
│   ├── app/src/main/java/     # Clean Architecture (presentation, crypto, image, offline, data)
│   ├── build.gradle.kts       # Gradle Build System & Version Catalogs
│   └── AndroidManifest.xml
├── backend/                   # Spring Boot 3 Java 21 Authority Backend
│   ├── src/main/java/         # Controllers, Services, Repositories, Entities, Cryptography
│   ├── src/main/resources/    # application.yml, Flyway Migrations (V1, V2)
│   └── pom.xml                # Maven Dependencies & Plugins
├── verification-portal/       # Public Verification Portal & Admin Web Console (React + Vite)
│   ├── src/components/        # VerificationPortal, AdminConsole, SimulatorLab, DocumentationView
│   └── package.json
├── docker/                    # Docker Compose & Multi-stage Dockerfiles
│   ├── docker-compose.yml     # PostgreSQL 16, Redis, Backend, Portal Nginx
│   └── nginx.conf
├── docs/                      # Comprehensive Architecture & Security Documentation
│   ├── ARCHITECTURE.md
│   ├── SECURITY_MODEL.md
│   ├── OFFLINE_ALGORITHM.md
│   ├── DATABASE_SCHEMA.md
│   ├── API_DOCUMENTATION.md
│   └── DEPLOYMENT_GUIDE.md
├── postman/                   # Ready-to-run Postman API Test Collection
└── README.md
```

---

## 4. Quick Start

### 1. Launch with Docker Compose (Recommended)
```bash
cd docker
docker compose up -d --build
```
- **Web Verification Portal & Admin Console**: `http://localhost`
- **Swagger / OpenAPI Documentation**: `http://localhost:8080/swagger-ui.html`

### 2. Run Backend Locally
```bash
cd backend
.\mvnw.cmd test-compile
.\mvnw.cmd spring-boot:run
```

### 3. Run Web Portal Locally
```bash
cd verification-portal
npm install
npm run dev
```

---

## 5. Security & Cryptographic Verifications

Run the automated backend test suite covering ECDSA P-256 signing, SHA-256 determinism, clock tampering attacks, and byte-level image tamper resistance:

```bash
cd backend
.\mvnw.cmd test
```

### Passing Tests:
- `EcdsaSignerTest`: Positive signing & verification, payload tamper rejection, signature corruption rejection.
- `TimeAnomalyTest`: Clock rewind attack detection & rejection (`TIME_ANOMALY`), legitimate offline elapsed time reconciliation.
- `Sha256HasherTest`: Deterministic SHA-256 and composite image + canonical metadata hashing.
- `JpegMarkerTest`: Embedding and extraction of Verification ID in JPEG COM segments (`0xFF, 0xFE`).
- `VerificationIntegrationTest`: Positive authentic evidence verification and 1-pixel tampering rejection (`Hash Mismatch`).

---

## 6. License
Apache License 2.0. Built for production-ready, mission-critical digital evidence authentication.
