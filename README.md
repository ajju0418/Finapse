# Finapse

A privacy-first personal finance intelligence and transaction reconciliation
platform. Everything runs on your own machine.

> **Don't count transactions. Understand them.**

---

## What it does

Finapse imports bank and credit-card statements, normalises and classifies each
transaction, detects duplicates, and links related transactions so money is
never counted twice.

The problem it solves: you spend ₹2,000 on a credit card, then pay the card bill
from your bank account. Two debits appear across two statements. A naive tracker
reports ₹4,000 of spending. Finapse recognises the settlement and reports
₹2,000.

**Features:** statement import (CSV, Excel, PDF) with column mapping ·
classification with a learning feedback loop · duplicate detection ·
reconciliation with user review · dashboard with trends and custom date ranges ·
budgets · subscription detection · credit-card utilisation and billing cycles ·
manual cash entry · CSV export.

---

## Stack

| Layer | Technology |
|---|---|
| Frontend | Next.js 16, React 19, TypeScript, Tailwind, shadcn/ui |
| Backend | Java 21, Spring Boot 3.3, Spring Data JPA, Spring Security |
| Database | MySQL 8+ |
| Parsing | Apache Commons CSV, Apache POI, PDFBox |
| Auth | JWT access tokens + rotating opaque refresh tokens |

---

## Project structure

```
Finapse/
├── AGENTS.md      — working guide for AI coding agents (start here)
├── CLAUDE.md      — imports AGENTS.md
├── doc/           — product, API and architecture documentation
├── database/      — schema.sql, seed.sql
├── backend/       — Spring Boot application
├── frontend/      — Next.js application
└── docker-compose.yml
```

---

## Quick start with Docker

The fastest path. Requires Docker Desktop.

```bash
cp .env.example .env
# Fill in DB_PASSWORD and FINAPSE_JWT_SECRET, then:
docker compose up --build
```

Generate a JWT secret with:

```bash
openssl rand -base64 48
```

- UI: http://localhost:3000
- API: http://localhost:8080/api
- Health: http://localhost:8080/api/health

---

## Local development setup

### Prerequisites

- Java 21 JDK (the build targets 21; JDK 24 also works for tests)
- Node.js 18+ and npm
- Maven 3.8+
- MySQL 8.0+ on port 3306

### 1. Configure credentials

The database password is read from the environment. **There is no default and no
password is stored in the repository** — set it before starting the backend.

```bash
# Windows (cmd)
set DB_USERNAME=root
set DB_PASSWORD=your_local_mysql_password

# macOS / Linux
export DB_USERNAME=root
export DB_PASSWORD=your_local_mysql_password
```

The backend creates the `finapse` database on first run and seeds the default
categories, so no manual SQL is required.

To set it up by hand instead:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS finapse;"
mysql -u root -p finapse < database/schema.sql
mysql -u root -p finapse < database/seed.sql
```

### 2. Start the backend

```bash
cd backend
mvn spring-boot:run
```

### 3. Start the frontend

In a second terminal:

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

Open http://localhost:3000 and register an account. The first registration
claims any pre-existing local data.

---

## Environment variables

### Backend

| Variable | Default | Notes |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/finapse?...` | |
| `DB_USERNAME` | `root` | |
| `DB_PASSWORD` | *(none)* | **Required** |
| `FINAPSE_JWT_SECRET` | *(ephemeral)* | Base64, ≥32 bytes. **Set this outside local dev** — otherwise a new key is generated per process and every session dies on restart. |
| `FINAPSE_ALLOWED_ORIGINS` | `http://localhost:3000,3001,3002` | CORS allowlist. Must match the frontend's actual origin. |
| `FINAPSE_COOKIE_SECURE` | `false` | Set `true` when serving over HTTPS. |

### Frontend

| Variable | Default | Notes |
|---|---|---|
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080/api` | Baked in at build time |
| `NEXT_PUBLIC_REFRESH_COOKIE_NAME` | `finapse_refresh` | Must match the backend |

---

## Testing and building

```bash
cd backend  && mvn test        # backend unit tests
cd frontend && npm run build   # production build + type check
cd frontend && npm run lint
```

CI runs both on every push — see `.github/workflows/ci.yml`.

---

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `Port 8080 already in use` | `netstat -ano \| findstr :8080`, then stop that PID |
| API calls fail with `403 Invalid CORS request` | Frontend origin missing from `FINAPSE_ALLOWED_ORIGINS`. Next falls back to :3001 when :3000 is taken. |
| `429` on every login | Lockout is keyed on email **and** IP; on localhost these collapse. Wait 15 minutes. |
| New page returns 404 | Stale dev server — restart `npm run dev` |
| Sessions drop on every backend restart | `FINAPSE_JWT_SECRET` unset, so the key is regenerated per process |
| Subscriptions list empty | Needs ≥2 occurrences 25–35 days apart. One month of data returns none — expected. |

---

## Contributing (human or AI)

Read **[AGENTS.md](AGENTS.md)** first. It contains the domain invariants that
keep the financial arithmetic correct, the security rules this codebase enforces,
and the repo-specific traps worth knowing before you start.

---

## Privacy

Finapse is designed to run entirely on your machine. There is no telemetry, no
analytics, and no third-party service receives your financial data. The database
is local and the statement files never leave your disk.
