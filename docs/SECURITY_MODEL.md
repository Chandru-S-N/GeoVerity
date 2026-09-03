# GeoVerity - Security Model & Threat Analysis

## 1. Threat Vectors & Defense Mechanisms

| Threat / Attack Vector | Attack Description | GeoVerity Countermeasure |
| :--- | :--- | :--- |
| **EXIF Tampering** | Attacker edits EXIF header to forge GPS/time | Server stores canonical metadata; uploaded EXIF is ignored during verification. |
| **Clock Rollback Attack** | User turns off network, rewinds clock by 2 hours, captures fake evidence | Monotonic elapsed time reconciliation detects deviation > 120s and triggers `TIME_ANOMALY` rejection. |
| **Pixel Alteration / Photoshop** | Modifying 1 pixel or cropping the footer | Composite SHA-256 calculation produces 100% hash mismatch against authoritative record. |
| **Social Media Re-compression** | Sending photo via standard WhatsApp/Telegram | WhatsApp re-encodes image bytes, causing hash mismatch. System enforces Document/File transfer. |
| **Client-Side Key Extraction** | Reverse-engineering mobile APK to steal private key | Authority private keys are strictly server-side. Mobile client has no private signing keys. |
| **API Key Theft / Replay** | Intercepting mobile client API key | API keys stored in Android Keystore AES-256-SIV; admin can revoke or rotate keys in real-time. |
| **Database Compromise Privacy Risk** | Attacker dumps server database | Server stores only cryptographic hashes and metadata proofs. Zero original image files stored. |

---

## 2. Why EXIF is Not Trusted as Proof

EXIF metadata is stored as standard editable tag blocks within JPEG/TIFF headers. Free software tools allow arbitrary modification of EXIF coordinates, camera models, and timestamps within seconds. 

In GeoVerity:
- The server stores the **authoritative canonical metadata** in PostgreSQL upon capture approval.
- Third-party verification uses the **server's stored metadata** to recalculate the composite hash.
- Uploaded EXIF tags are discarded and never used for authentication.

---

## 3. Why QR Code is an Identifier, Not Cryptographic Proof

A QR code is simply a visual encoding of the Verification ID (`SGA-...`) to facilitate rapid camera scanning.
- A counterfeit image can easily copy and paste a genuine QR code.
- Verification requires **3 independent cryptographic tests**:
  1. Valid Verification ID in database registry
  2. Valid Server ECDSA P-256 digital signature
  3. 100% exact bit match on composite SHA-256 hash over uploaded image bytes + stored metadata

---

## 4. Realistic Security Stance

GeoVerity uses formal, realistic security engineering terminology and **never claims 100% security**. It provides mathematical proof of digital integrity against modification, re-compression, EXIF tampering, and device clock rollbacks.
