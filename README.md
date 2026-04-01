# PharmaCX DMS

A 21 CFR Part 11 compliant pharmaceutical Document Management System with integrated on-premises AI assistance.

---

## Prerequisites

- Docker Desktop 24+ with Docker Compose
- 12 GB RAM minimum (16 GB recommended)
- 20 GB free disk space
- macOS, Linux, or Windows (WSL2)

---

## Developer Setup

**1. Clone and configure**

```bash
git clone <repo-url>
cd pharma-cx-dms
```

**2. Start all services**

```bash
cd infra
docker compose up -d
```

This starts MongoDB, Spring Boot backend, React frontend, OnlyOffice Document Server, and Ollama AI (auto-downloads `qwen2.5:1.5b` and builds the `pharma-ai` model on first run — allow 3-5 minutes).

**3. Access the app**

| Service | URL |
|---|---|
| Frontend | http://localhost:3001 |
| Backend API | http://localhost:8081 |
| OnlyOffice | http://localhost:8093 |
| Ollama API | http://localhost:11434 |

**4. Local development (without Docker)**

```bash
# Backend (Java 17+, Maven)
cd backend
./mvnw spring-boot:run

# Frontend (Node 18+)
cd frontend
npm install
npm run dev
```

Set these environment variables for local backend:
```
SPRING_DATA_MONGODB_URI=mongodb://localhost:27018/pharma-cx-db
ONLYOFFICE_URL=http://localhost:8093
ONLYOFFICE_EXTERNAL_URL=http://localhost:8093
ONLYOFFICE_JWT_SECRET=pharma-cx-onlyoffice-jwt-secret-key-2026
AUTH_JWT_SECRET=pharma-cx-256bit-secret-key-for-dev-only!!
```

---

## Deployment

**1. Update secrets before deploying**

Edit `infra/docker-compose.yml` and replace the default dev secrets:

```yaml
AUTH_JWT_SECRET: <strong-256bit-secret>
ONLYOFFICE_JWT_SECRET: <strong-secret>
JWT_SECRET: <same-as-onlyoffice-jwt-secret>
```

**2. Set the correct external URL**

In `docker-compose.yml`, update `ONLYOFFICE_EXTERNAL_URL` to your server's public hostname:
```yaml
ONLYOFFICE_EXTERNAL_URL: https://your-domain.com:8093
```

**3. Deploy**

```bash
cd infra
docker compose up -d --build
```

**4. Verify all services are healthy**

```bash
docker compose ps
```

All services should show `healthy` or `running` within 5 minutes.

---

---

## AI Assistant (Helix AI)

The built-in AI Assistant runs fully on-premises via **Helix AI** (powered by Ollama) — no data leaves your infrastructure.

- **Direct Assistance**: Available in **Author Draft** and **Author Edit** modes inside the document editor.
- **Helix AI Integration**: Seamlessly synchronized with the **Helix AI** (AnythingLLM + LiteLLM) stack for advanced RAG and source attribution.
- **Model**: Now uses the unified **`helix-ai`** model for consistency across all solutions.
- **Infrastructure Managed**: AI paths and models are managed via `docker-compose.yml` environment variables, ensuring a single source of truth.

To update the AI model or system prompt, edit `infra/ollama/helix-ai` then recreate the model:
```bash
docker exec pharma-ollama ollama create helix-ai -f /modelfiles/helix-ai
```

---

## Helix AI — Zero-Code RAG Integration

PharmaCX is now integrated with **Helix AI** for advanced pharmaceutical search and document grounding.

### 🧬 How it works
Any document that transitions to the **PUBLISHED** status in PharmaCX is automatically exported to a shared hot-folder. Helix AI monitors this folder and vectorizes the content in real-time.

### 🛠️ Configuration
The integration is pre-configured in `infra/docker-compose.yml` via shared volume mapping:
- **System Grounding Path**: `/Users/venkateshwarlu/Documents/knowledge-base` (Container: `/app/knowledge-base`)
- **System Export Path**: `/Users/venkateshwarlu/Documents/published-docs` (Container: `/app/published-docs`)

### 🚀 Starting the Grounding Heartbeat
To enable real-time synchronization, you must start the heartbeat script in your Helix AI directory:
```bash
cd /Users/venkateshwarlu/Documents/helix-ai
./scripts/helix_grounding_heartbeat.sh ./published-docs qa-confidential
```

---

---

## Useful Commands

```bash
# View logs
docker compose logs -f backend
docker compose logs -f ollama

# Restart a service
docker compose restart backend

# Rebuild after code changes
docker compose up -d --build backend
docker compose up -d --build frontend

# Stop everything
docker compose down

# Full reset (removes all data)
docker compose down -v
```

---

## Compliance

This system is designed to support 21 CFR Part 11 requirements including electronic records, audit trails, and role-based access control. It is the responsibility of the deploying organization to validate the system per their site validation master plan before use in a regulated environment.
