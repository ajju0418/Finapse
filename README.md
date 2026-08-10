# Finapse

A privacy-first personal finance intelligence and transaction reconciliation platform.

> **Don't count transactions. Understand them.**

---

## What is Finapse?

Finapse imports bank and credit-card CSV statements, normalises transactions, detects duplicates, reconciles related transactions (e.g. credit-card payments), tracks cashback and refunds, and provides an accurate view of income, spending, and cash flow — all locally on your laptop.

---

## Stack

| Layer    | Technology                              |
|----------|-----------------------------------------|
| Frontend | Next.js 14, TypeScript, Tailwind, shadcn/ui |
| Backend  | Java 21, Spring Boot 3, Spring Data JPA |
| Database | MySQL 8+                                |
| CSV      | Apache Commons CSV                      |

---

## Project Structure

```
FINAPSE/
├── doc/          — Product and architecture documentation
├── database/     — schema.sql, seed.sql
├── backend/      — Spring Boot application
├── frontend/     — Next.js application
└── README.md
```

---

## Getting Started

### 1. Database

```sql
mysql -u root -p < database/schema.sql
mysql -u root -p finapse < database/seed.sql
```

### 2. Backend

```bash
cd backend
# Configure DB credentials (or set environment variables)
# DB_URL, DB_USERNAME, DB_PASSWORD
mvn spring-boot:run
```

Backend runs on: http://localhost:8080

Health check: http://localhost:8080/api/health

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on: http://localhost:3000

---

## Environment Variables

### Backend

| Variable      | Default                                      |
|---------------|----------------------------------------------|
| `DB_URL`      | `jdbc:mysql://localhost:3306/finapse?...`    |
| `DB_USERNAME` | `root`                                       |
| `DB_PASSWORD` | *(empty)*                                    |

### Frontend

| Variable               | Default                        |
|------------------------|--------------------------------|
| `NEXT_PUBLIC_API_URL`  | `http://localhost:8080/api`    |

---

## Implementation Phases

- [x] Phase 1 — Project Foundation
- [x] Phase 2 — Database + JPA Entities
- [x] Phase 3 — Account + Card Management
- [x] Phase 4 — CSV Import Pipeline
- [x] Phase 5 — Transaction Intelligence
- [x] Phase 6 — Reconciliation Engine
- [x] Phase 7 — Money Dashboard
- [x] Phase 8 — Frontend Polish
