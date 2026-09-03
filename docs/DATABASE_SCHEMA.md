# GeoVerity - Database Relational Schema

GeoVerity uses a strict relational table structure (PostgreSQL 15+ & MySQL compatible) managed with Flyway migrations.

```
+-------------------------------------------------------------------------------+
|                                 DATABASE ERD                                  |
+-------------------------------------------------------------------------------+

  +-----------------------+              +-----------------------------+
  |      api_clients      |              |    verification_records     |
  +-----------------------+              +-----------------------------+
  | id (PK, UUID)         |<----+        | id (PK, UUID)               |
  | client_name           |     |        | verification_id (UK, INDEX) |
  | api_key_hash (UK)     |     +------->| api_client_id (FK)          |
  | api_key_prefix        |              | sha256_hash                 |
  | permissions           |              | canonical_metadata (TEXT)   |
  | status                |              | trusted_server_timestamp    |
  | created_at            |              | ecdsa_signature (TEXT)      |
  | last_used_at          |              | device_id                   |
  +-----------------------+              | status (INDEX)              |
                                         | created_at (INDEX)          |
  +-----------------------+              | updated_at                  |
  |        devices        |              +-----------------------------+
  +-----------------------+                             |
  | id (PK, UUID)         |                             |
  | device_id (UK, INDEX) |                             v
  | device_model          |              +-----------------------------+
  | os_version            |              |         audit_logs          |
  | app_version           |              +-----------------------------+
  | status                |              | id (PK, UUID)               |
  | first_seen_at         |              | event_type (INDEX)          |
  | last_seen_at          |              | client_ip                   |
  +-----------------------+              | api_client_id (FK, opt)     |
                                         | verification_id (INDEX)     |
                                         | status                      |
                                         | details (TEXT)              |
                                         | created_at (INDEX)          |
                                         +-----------------------------+
```

## Table Specifications

### 1. `api_clients`
Stores registered client applications (not human users).
- `id`: UUID Primary Key
- `client_name`: Client name (e.g. "GeoVerity Android Client")
- `api_key_hash`: SHA-256 hash of API key (UNIQUE)
- `api_key_prefix`: Masked prefix (e.g. `gv_live_82F4`)
- `permissions`: Allowed scopes (`CAPTURE,VERIFY,TIME_TOKEN`)
- `status`: `ACTIVE`, `REVOKED`, `DISABLED`
- `created_at`: Timestamp
- `last_used_at`: Timestamp of last API call

### 2. `verification_records`
Stores authoritative digital evidence proof records. **No image blobs or permanent file paths!**
- `id`: UUID Primary Key
- `verification_id`: Unique Identifier `SGA-...` (UNIQUE, INDEXED)
- `sha256_hash`: Composite SHA-256 hash string (64 chars)
- `canonical_metadata`: Normalized JSON containing GPS, location, device ID, and timestamp
- `trusted_server_timestamp`: Authoritative server time
- `ecdsa_signature`: Base64 ECDSA P-256 signature
- `device_id`: Device identifier
- `api_client_id`: Foreign Key to `api_clients(id)`
- `status`: `AUTHENTICATED`, `REVOKED`

### 3. `audit_logs`
Immutable stream of security events.
- `id`: UUID Primary Key
- `event_type`: Event code (`CAPTURE_REQUEST`, `AUTHENTICATION_SUCCESS`, `TIME_ANOMALY`, `VERIFY_SUCCESS`, `API_KEY_ROTATED`)
- `client_ip`: Remote IP
- `verification_id`: Verification ID reference
- `status`: `SUCCESS`, `FAILURE`, `ANOMALY`
- `details`: Detailed audit description
