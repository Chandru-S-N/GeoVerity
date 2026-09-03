# GeoVerity - Production Deployment Guide

## 1. Quick Start with Docker Compose

Deploy the entire production stack (PostgreSQL, Redis, Spring Boot Backend, and Web Portal) with one command:

```bash
cd docker
docker compose up -d --build
```

### Access URLs:
- **Verification Web Portal**: `http://localhost`
- **Backend API & Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **PostgreSQL Database**: `localhost:5432`

---

## 2. Cloud VPS / Ubuntu Deployment (Let's Encrypt TLS)

1. Clone repository to VPS:
   ```bash
   git clone https://github.com/Chandru-S-N/GeoVerity.git
   cd GeoVerity
   ```

2. Configure environment variables in `docker/.env`:
   ```bash
   cp docker/.env.example docker/.env
   # Edit passwords and production domain in docker/.env
   ```

3. Launch with Docker Compose:
   ```bash
   docker compose -f docker/docker-compose.yml up -d
   ```

4. Install Certbot for Let's Encrypt SSL/TLS on Nginx:
   ```bash
   sudo apt update && sudo apt install -y certbot python3-certbot-nginx
   sudo certbot --nginx -d verify.yourdomain.com
   ```

---

## 3. Production Security Checklist

- [x] ECDSA P-256 private key securely held in `/app/keys` with restricted read permissions (never committed to git).
- [x] End-user login disabled on mobile client.
- [x] Client authorization strictly via hashed `X-API-Key`.
- [x] Monotonic time reconciliation enabled (120,000ms threshold).
- [x] Zero image blobs or URLs stored in PostgreSQL.
- [x] Public verification endpoint requires only the image file.
- [x] TLS 1.3 encryption enforced across all API communications.
- [x] Android Keystore AES-256-GCM utilized for temporary offline storage.
