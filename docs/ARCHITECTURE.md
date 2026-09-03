# GeoVerity - Architecture Specification

## 1. System Overview

**GeoVerity** is a secure, enterprise-grade mobile and server digital evidence authentication platform. It allows authorized Android mobile clients to capture controlled photographs and cryptographically bind:
- Image pixel content
- GPS location coordinates & locality name
- Authoritative trusted server timestamp
- Device and application identification
- Unique Verification ID (`SGA-<UUID>`)

---

## 2. Core Architecture Principles

```
+-------------------------------------------------------------------------+
|                           ANDROID CLIENT                                |
|   (Controlled CameraX Capture -> Footer Composition -> Embedded COM)    |
+-------------------------------------------------------------------------+
                                    |
                       X-API-Key    | (TLS / HTTPS)
                                    v
+-------------------------------------------------------------------------+
|                       SPRING BOOT BACKEND AUTHORITY                     |
|  - Validates API Key & Time Token                                       |
|  - Computes Server-Side ECDSA P-256 Signature                           |
|  - Relational Database Storage (Proofs Only, No Image Files)           |
+-------------------------------------------------------------------------+
                                    ^
                                    | (Multipart Upload Image ONLY)
                                    | (Zero Login Required)
+-------------------------------------------------------------------------+
|                  THIRD-PARTY VERIFICATION WEB PORTAL                    |
|  - Extracts Verification ID from JPEG COM Marker / QR Code              |
|  - Direct Indexed DB Lookup                                             |
|  - Verifies Server ECDSA Signature                                      |
|  - Recalculates Composite SHA-256 (100% Bit Exact Match)                |
+-------------------------------------------------------------------------+
```

### Key Rules:
1. **Zero User Login**: End users do not register or log in. Mobile clients authenticate strictly via `X-API-Key` securely held in Android Keystore.
2. **Server Authority**: The mobile client is a controlled capture environment, not an authoritative signer. Signatures are generated strictly on the backend using server-side **ECDSA NIST P-256 (SHA256withECDSA)**.
3. **Zero Image Retention on Server**: The backend database stores only cryptographic proofs (`sha256_hash`, `canonical_metadata`, `verification_id`, `ecdsa_signature`, `audit_logs`). No image blobs or URLs are permanently stored.
4. **Third-Party Verification Requires Only the Image**: Third parties upload only the original image file. The Verification ID is auto-extracted from embedded JPEG COM segments (`0xFF, 0xFE`) or QR codes.

---

## 3. Dedicated Image Layout Design

The original photograph area remains clean and unobstructed. A dedicated metadata footer is rendered at the bottom:

```
+----------------------------------------------------+
|                                                    |
|             ORIGINAL PHOTOGRAPH CONTENT            |
|               (Clean & Unobstructed)               |
|                                                    |
+----------------------------------------------------+
| Location: Karur, Tamil Nadu, India                 |
| GPS: 10.785234, 78.125432           +------------+ |
| Date: 03 Sep 2026                   | DEDICATED  | |
| Time: 02:35:12 PM IST               |  QR CODE   | |
| Verification ID: SGA-82F4D2A7...    |            | |
| Device: dev_pixel8_gv_984128        +------------+ |
+----------------------------------------------------+
```

---

## 4. Cryptographic Hashing Specification

The authenticated hash is calculated over:
```
SHA-256( finalImageBytes + canonicalMetadataBytes )
```

### Canonical Metadata JSON Specification:
Deterministic JSON with strictly alphabetical key order and normalized 6-decimal float coordinates:
```json
{
  "appVersion": "1.0.0",
  "deviceId": "dev_pixel8_gv_984128",
  "latitude": 10.785234,
  "locationName": "Karur, Tamil Nadu, India",
  "longitude": 78.125432,
  "trustedTimestamp": 1788440712000,
  "verificationId": "SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E"
}
```
