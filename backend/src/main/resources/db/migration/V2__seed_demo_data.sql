-- Seed Initial Demo API Client for GeoVerity Android Client
-- Raw Key: gv_live_demo_android_app_key_2026_98a72
-- SHA-256 Hash: c759a224a9a084c5689da6d4002636c0a0c9a41df08f61546ea48d88e0f3fe67

INSERT INTO api_clients (id, client_name, api_key_hash, api_key_prefix, permissions, status, created_at, last_used_at)
VALUES (
    'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d',
    'GeoVerity Official Android App',
    'c759a224a9a084c5689da6d4002636c0a0c9a41df08f61546ea48d88e0f3fe67',
    'gv_live_demo',
    'CAPTURE,VERIFY,TIME_TOKEN',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- Seed Demo Device
INSERT INTO devices (id, device_id, device_model, os_version, app_version, status, first_seen_at, last_seen_at)
VALUES (
    'b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e',
    'dev_pixel8_gv_984128',
    'Google Pixel 8 Pro',
    'Android 14 (API 34)',
    '1.0.0',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
