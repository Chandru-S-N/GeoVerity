-- GeoVerity Core Relational Schema
-- PostgreSQL 15+ compatible

CREATE TABLE IF NOT EXISTS api_clients (
    id UUID PRIMARY KEY,
    client_name VARCHAR(100) NOT NULL,
    api_key_hash VARCHAR(64) NOT NULL,
    api_key_prefix VARCHAR(16) NOT NULL,
    permissions VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ac_api_key_hash ON api_clients(api_key_hash);
CREATE INDEX IF NOT EXISTS idx_ac_status ON api_clients(status);

CREATE TABLE IF NOT EXISTS devices (
    id UUID PRIMARY KEY,
    device_id VARCHAR(128) NOT NULL UNIQUE,
    device_model VARCHAR(128),
    os_version VARCHAR(64),
    app_version VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    first_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dev_device_id ON devices(device_id);

CREATE TABLE IF NOT EXISTS verification_records (
    id UUID PRIMARY KEY,
    verification_id VARCHAR(64) NOT NULL UNIQUE,
    sha256_hash VARCHAR(64) NOT NULL,
    canonical_metadata TEXT NOT NULL,
    trusted_server_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    ecdsa_signature TEXT NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    api_client_id UUID NOT NULL REFERENCES api_clients(id),
    status VARCHAR(32) NOT NULL DEFAULT 'AUTHENTICATED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vr_verification_id ON verification_records(verification_id);
CREATE INDEX IF NOT EXISTS idx_vr_api_client_id ON verification_records(api_client_id);
CREATE INDEX IF NOT EXISTS idx_vr_created_at ON verification_records(created_at);
CREATE INDEX IF NOT EXISTS idx_vr_status ON verification_records(status);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    client_ip VARCHAR(45),
    api_client_id UUID,
    verification_id VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_verification_id ON audit_logs(verification_id);
CREATE INDEX IF NOT EXISTS idx_audit_event_type ON audit_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at);
