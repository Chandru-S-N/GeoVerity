-- ==============================================================================
-- GeoVerity Production Relational Database Schema for MySQL (8.0+)
-- Character Set: utf8mb4, Engine: InnoDB
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS geoverity
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE geoverity;

-- ------------------------------------------------------------------------------
-- 1. Table: api_clients
-- Stores registered client credentials with one-way SHA-256 key hashing
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS api_clients (
    id VARCHAR(36) NOT NULL,
    client_name VARCHAR(100) NOT NULL,
    api_key_hash VARCHAR(64) NOT NULL,
    api_key_prefix VARCHAR(16) NOT NULL,
    permissions VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ac_api_key_hash (api_key_hash),
    INDEX idx_ac_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 2. Table: devices
-- Hardware-bound client device registry and app version tracking
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS devices (
    id VARCHAR(36) NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    device_model VARCHAR(128) NULL,
    os_version VARCHAR(64) NULL,
    app_version VARCHAR(32) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dev_device_id (device_id),
    INDEX idx_dev_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 3. Table: verification_records
-- Core cryptographic evidence registry (SHA-256 hash, ECDSA signatures, metadata)
-- Note: Raw images are never stored; only irreversible cryptographic proofs.
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS verification_records (
    id VARCHAR(36) NOT NULL,
    verification_id VARCHAR(64) NOT NULL,
    sha256_hash VARCHAR(64) NOT NULL,
    canonical_metadata TEXT NOT NULL,
    trusted_server_timestamp TIMESTAMP NOT NULL,
    ecdsa_signature TEXT NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    api_client_id VARCHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'AUTHENTICATED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vr_verification_id (verification_id),
    INDEX idx_vr_sha256 (sha256_hash),
    INDEX idx_vr_api_client_id (api_client_id),
    INDEX idx_vr_created_at (created_at),
    INDEX idx_vr_status (status),
    CONSTRAINT fk_vr_api_client FOREIGN KEY (api_client_id)
        REFERENCES api_clients (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 4. Table: audit_logs
-- Immutable tamper-evident audit log of all security and verification events
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(36) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    client_ip VARCHAR(45) NULL,
    api_client_id VARCHAR(36) NULL,
    verification_id VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL,
    details TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_audit_verification_id (verification_id),
    INDEX idx_audit_event_type (event_type),
    INDEX idx_audit_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
