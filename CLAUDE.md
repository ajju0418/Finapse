# Finapse

@AGENTS.md

---

The full working guide is in [AGENTS.md](AGENTS.md) and is imported above. It is
the single source of truth — read it before writing code, and put any new
project rules there rather than here, so every tool sees them.

## Quick reference

```bash
cd backend  && mvn spring-boot:run   # API on :8080
cd backend  && mvn test              # must pass before you finish
cd frontend && npm run dev           # UI on :3000
cd frontend && npm run build         # must succeed before you finish
```

## The five rules that matter most

1. **Spending = `EXPENSE` − `REFUND`, floored at zero.** Card payments and
   transfers are never spending. Never sum raw debits.
2. **Scope every user-owned lookup by user id.** `findByIdAndUserId(...)`, never
   `findById(...)`. This repo has had real IDOR bugs.
3. **Money is `BigDecimal`**, compared with `compareTo`. Never `double`.
4. **Never mutate or delete an imported transaction** to resolve a duplicate.
   Create a `TransactionLink` and let the user decide.
5. **Run the build and tests, read the output, then report.** Never claim a
   command passed without running it.

## Docs

| File | Contents |
|---|---|
| [AGENTS.md](AGENTS.md) | Working guide, domain rules, patterns, repo traps |
| [doc/API.md](doc/API.md) | Every REST endpoint |
| [doc/PRODUCT.md](doc/PRODUCT.md) | Product definition and principles |
| [doc/Requriement.md](doc/Requriement.md) | Numbered functional requirements |
| [doc/backend_Architecture.md](doc/backend_Architecture.md) | Backend layering |
| [doc/database_Schema.md](doc/database_Schema.md) | Data model |
| [doc/frontend_architecture.md](doc/frontend_architecture.md) | Frontend structure |
