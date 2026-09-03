# GeoVerity - REST API Documentation

Base URL: `https://api.geoverity.org` (or `http://localhost:8080`)

All API endpoints produce and consume `application/json` (except `/api/v1/verify` which consumes `multipart/form-data`).

---

## 1. Time Authority Endpoints

### Obtain Trusted Server Time Token
- **Endpoint**: `POST /api/v1/time/token`
- **Headers**: `X-API-Key: <client_api_key>`
- **Request Body**:
  ```json
  {
    "deviceId": "dev_pixel8_gv_984128",
    "clientTimestamp": 1788440712000
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "serverTime": 1788440712000,
    "token": "MTc4ODQ0MDcxMjAwMDoxNzg4NDQwNzcyMDAwOmE5Zj...==",
    "expiresAt": 1788440772000,
    "toleranceMs": 5000
  }
  ```

---

## 2. Capture & Authentication Endpoints

### Authenticate Online Capture
- **Endpoint**: `POST /api/v1/capture`
- **Headers**: `X-API-Key: <client_api_key>`
- **Request Body**:
  ```json
  {
    "verificationId": "SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E",
    "timeToken": "MTc4ODQ0MDcxMjAwMDoxNzg4NDQwNzcyMDAwOmE5Zj...==",
    "canonicalMetadata": {
      "appVersion": "1.0.0",
      "deviceId": "dev_pixel8_gv_984128",
      "latitude": 10.785234,
      "locationName": "Karur, Tamil Nadu, India",
      "longitude": 78.125432,
      "trustedTimestamp": 1788440712000,
      "verificationId": "SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E"
    },
    "sha256Hash": "a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "verificationId": "SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E",
    "status": "AUTHENTICATED",
    "trustedTimestamp": "2026-09-03T14:35:12Z",
    "sha256": "a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8",
    "signatureStatus": "VALID",
    "ecdsaSignature": "MEYCIQCz8b1vWqfV4..."
  }
  ```

---

## 3. Third-Party Verification (Zero Login)

### Verify Original Image
- **Endpoint**: `POST /api/v1/verify`
- **Headers**: None required (Public access, no login)
- **Form Data**: `file` (Multipart image file)
- **Response (200 OK - Authentic)**:
  ```json
  {
    "verificationId": "SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E",
    "status": "AUTHENTIC",
    "signatureValid": true,
    "hashMatched": true,
    "location": "Karur, Tamil Nadu, India",
    "gps": "10.785234, 78.125432",
    "trustedTimestamp": "2026-09-03T14:35:12Z",
    "deviceId": "dev_pixel8_gv_984128",
    "sha256Hash": "a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8"
  }
  ```
- **Response (200 OK - Tampered / Modified)**:
  ```json
  {
    "verificationId": "SGA-82F4D2A7-C34E-4621-91AB-5369A18DF50E",
    "status": "NOT_AUTHENTIC",
    "signatureValid": true,
    "hashMatched": false,
    "failureReason": "Hash mismatch: The photograph content, metadata footer, or pixel bytes have been modified, cropped, compressed, or re-encoded."
  }
  ```

---

## 4. Admin Management Endpoints

- `POST /api/v1/admin/api-clients` (Create new client, returns raw key once)
- `GET /api/v1/admin/api-clients` (List clients with masked prefixes)
- `POST /api/v1/admin/api-clients/{id}/rotate` (Rotate key)
- `POST /api/v1/admin/api-clients/{id}/revoke` (Revoke key)
- `GET /api/v1/admin/verification-records` (Paginated evidence records)
- `GET /api/v1/admin/audit-logs` (Paginated security audit logs)
- `GET /api/v1/admin/stats` (System metrics summary)
