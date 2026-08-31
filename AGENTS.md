# Finapse — Agent Working Guide

**Read this file completely before writing any code.** It is the contract for
working in this repository. `CLAUDE.md` points here; this file is canonical.

Finapse is a **privacy-first personal finance reconciliation platform**. It
imports bank and credit-card statements, classifies transactions, detects
duplicates, links related transactions, and reports what actually happened to
the user's money.

---

## 0. The one idea that governs everything

> **A transaction appearing in a statement is not necessarily a new expense.**

You buy something for ₹2,000 on a credit card. Later you pay the card bill from
your bank account. Two debits appear across two statements. Naive trackers say
you spent ₹4,000. **You spent ₹2,000.**

Every feature you write must respect this. If a change causes money to be
counted twice, it is wrong no matter how clean the code is.

---

## 1. Non-negotiable domain rules

These are invariants. Violating one is a bug even if tests pass.

| # | Rule |
|---|------|
| D1 | **Spending = `EXPENSE` − `REFUND`, floored at zero.** Never sum raw debits. |
| D2 | **`CREDIT_CARD_PAYMENT` is never spending.** It settles spending already counted. |
| D3 | **`TRANSFER` is never spending or income.** It moves money between the user's own accounts. |
| D4 | **`CASHBACK` is reported separately.** It is not income and does not reduce spending. |
| D5 | **Never delete or mutate an imported transaction to "fix" reconciliation.** Create a `TransactionLink` instead. Original records stay intact. |
| D6 | **When the engine is unsure, it must not guess.** Set status `REVIEW_REQUIRED` and let the user decide. |
| D7 | **Every classification must carry a reason.** Populate `classificationSource`, `classificationConfidence` and `classificationReason`. A blank explanation is a defect. |
| D8 | **Money is `BigDecimal`.** Never `double`, never `float`. Compare with `compareTo`, never `==` or `equals`. |
| D9 | **A transaction belongs to exactly one source** — an account *or* a card, never both, never neither. Enforced by a DB `CHECK`. |
| D10 | **Financial data never leaves the user's machine.** No telemetry, no third-party APIs, no CDN calls with user data. |

When D1–D4 apply, reference the rule number in your code comment so the next
reader knows the arithmetic is deliberate:

```java
// D1: refunds reduce spending; a negative result would misreport a windfall.
BigDecimal actualSpending = grossExpenses.subtract(refunds).max(BigDecimal.ZERO);
```

---

## 2. Security rules — apply mechanically, every time

This codebase has had real IDOR vulnerabilities. Follow these exactly.

### S1. Every user-owned lookup is scoped by user id

The app is multi-user. `findById` on a user-owned entity is **always a bug** —
any signed-in user could read another user's data by guessing a UUID.

```java
// WRONG — any user can read any card
cardRepository.findById(id);

// RIGHT
cardRepository.findByIdAndUserId(id, userService.getCurrentUserId());
```

User-owned entities: `Account`, `Card`, `Statement`, `Transaction`, `Budget`,
`UserClassificationRule`, `RefreshToken`.
Shared reference data (`Category`, `Merchant`) is exempt.

### S2. Get the current user from `UserService`

`userService.getCurrentUserId()` / `getCurrentUser()`. Never accept a user id
from a request body, query parameter, or path variable — that is impersonation.

### S3. Never log or commit secrets

No passwords, tokens, or full account numbers in logs. Configuration reads from
the environment: `${DB_PASSWORD:}`. A real value must never appear in
`application.properties`.

### S4. Validate at the boundary

DTOs carry `jakarta.validation` annotations; controllers use `@Valid`. Do not
re-validate deep in services.

### S5. Errors go through `GlobalExceptionHandler`

Throw a typed exception (`ResourceNotFoundException`, `ConflictException`,
`BadRequestException`, `UnauthorizedException`). Never build an error response
by hand and never leak a stack trace to the client.

### S6. Escape untrusted text on the way out

CSV export prefixes a leading `= + - @` with `'` to stop spreadsheet formula
injection. Preserve that behaviour — a merchant name is attacker-influenced text.

---

## 3. Commands

Run from the repository root unless noted. On a fresh machine, do the one-time
setup in [README.md](README.md) first — the database password and JWT secret
come from the environment and have no defaults.

### Backend
```bash
cd backend
mvn spring-boot:run          # start API on :8080
mvn test                     # full test suite — MUST pass before you finish
mvn -q -DskipTests compile   # fast compile check while iterating
```

### Frontend
```bash
cd frontend
npm install
npm run dev                  # :3000
npm run build                # full production build + type check
npm run lint
npx tsc --noEmit             # types only, faster than a build
```

### Everything at once
```bash
docker compose up --build    # needs a .env — copy from .env.example
```

### Health check
`GET http://localhost:8080/api/health`

---

## 4. Definition of done

Do not report a task complete until **all** of these hold.

- [ ] `cd backend && mvn test` passes. Not "compiles" — **passes**.
- [ ] `cd frontend && npm run build` succeeds.
- [ ] New backend behaviour has a unit test covering the happy path **and** at
      least one edge case (empty input, zero amount, missing optional field).
- [ ] No `findById` added on a user-owned entity (see S1).
- [ ] Any new money arithmetic obeys D1–D4.
- [ ] `database/schema.sql` updated if you touched an entity's columns.
- [ ] No secret, password, or personal data added to a tracked file.

If a command fails, **fix it**. Do not report success with a known-failing test
and do not describe a failure as pre-existing without checking `git stash`.

---

## 5. Where things live

```
backend/src/main/java/com/finapse/
  controller/     REST endpoints — thin, no business logic
  service/        business logic, transaction boundaries
  classification/ the intelligence engine
    strategy/     ordered classifier chain (@Order 1..6)
    detection/    normalization, merchant matching, recurrence
    orchestrator/ ClassificationOrchestrator — the entry point
  repository/     Spring Data JPA interfaces
  entity/         JPA entities
  dto/            request/response records
  enums/          domain enums
  config/         security, CORS, async
  security/       JWT filter and token service
  exception/      typed exceptions + GlobalExceptionHandler

frontend/
  app/app/<page>/page.tsx   routed pages (App Router)
  components/<domain>/      React components grouped by domain
  lib/api/                  typed API clients — one file per domain
  types/                    shared TypeScript types

database/schema.sql         canonical schema — keep in sync with entities
doc/                        product and architecture docs
```

---

## 6. Code patterns to copy

Match the surrounding code. When in doubt, open a neighbouring file and mirror it.

### Backend service

```java
@Service
@RequiredArgsConstructor          // constructor injection — never @Autowired fields
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserService userService;

    @Transactional(readOnly = true)          // readOnly for queries
    public List<BudgetResponse> getAll() {
        UUID userId = userService.getCurrentUserId();   // S2
        return budgetRepository.findActiveByUser(userId)
                .stream().map(this::toResponse).toList();
    }

    private Budget findOrThrow(UUID id, UUID userId) {
        return budgetRepository.findByIdAndUserId(id, userId)     // S1
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + id));
    }
}
```

### DTOs are records

```java
public record BudgetRequest(
        UUID categoryId,
        @NotNull(message = "A budget limit is required.")
        @DecimalMin(value = "0.01", message = "The budget limit must be greater than zero.")
        BigDecimal limitAmount
) {}
```

Validation messages are shown directly to the user. Write them as full sentences
in plain language: *"The budget limit must be greater than zero."* — not
*"limitAmount invalid"*.

### Frontend API client

```ts
// lib/api/budgets.ts — one file per domain, always typed
export const budgetsApi = {
  getAll: () => apiClient.get<Budget[]>('/budgets'),
  create: (input: BudgetInput) => apiClient.post<Budget>('/budgets', input),
}
```

Always go through `apiClient` — it handles auth headers, the 401 refresh cycle,
and error normalisation. Never call `fetch` directly for JSON endpoints.

### Frontend page

```tsx
'use client'
// Every page handles all four states. Do not skip any.
if (loading) return <Skeleton />
if (error)   return <ErrorBanner />
if (data.length === 0) return <EmptyState />
return <TheActualContent />
```

---

## 7. Repo-specific traps

These have each cost real debugging time. Read them.

### Environment
- **Tests run on JDK 24 while the project targets 21.** Byte Buddy is pinned to
  `1.17.5` in `pom.xml` for this reason. Do not remove that pin.
- **On Windows, the VS Code terminal may alternate between `cmd` and
  PowerShell** and mangle pipes and redirects. If a command fails with
  "not recognized", wrap it: `cmd /c "..."`. Heredocs (`<<EOF`) never work in
  `cmd` — write the text to a file instead.
- **Never run `taskkill /IM node.exe` or delete `.next`** while `npm run dev` is
  running. It leaves a stale route manifest — new routes 404 while old ones work.
- **If Next falls back to port 3001**, the backend CORS allowlist must include it
  or every request fails preflight with `403 Invalid CORS request`.

### Symptom → cause
| Symptom | Cause |
|---|---|
| New route returns 404, old routes fine | Stale dev server — restart `npm run dev` |
| API calls fail from a loaded page | CORS origin/port mismatch |
| `429` on every login attempt | Lockout is keyed on email **and** IP; on localhost both collapse to one host. Wait 15 minutes. |
| Subscriptions list is empty | Detection needs ≥2 occurrences 25–35 days apart **and** a non-null merchant. One month of data legitimately yields zero. |
| Blank classification reason | Row imported before the column existed — `POST /api/statements/{id}/reclassify` |

### Architecture
- **`@Async` only works across bean boundaries.** `StatementImportProcessor`
  is a separate bean for exactly this reason. Calling it from inside
  `StatementService` via `this` would silently run inline.
- **The async import thread has no `SecurityContext`.** The user id is passed
  explicitly. Do not call `userService.getCurrentUserId()` from a background job.
- **Async work is dispatched in `afterCommit`**, otherwise the worker can start
  before the row it needs is visible.
- **Learned rules key on the *normalized* narration**, not the raw description.
  Use `NormalizationService.normalize` on both sides or rules will never match.
- **`/api/rules` is its own controller**, not `/api/transactions/rules` — the
  latter collides with the `/{id}` path variable.

---

## 8. Working style

### Scope
Change what was asked and what is genuinely required to make it work. Do not
refactor adjacent code, rename things, add abstractions for single call sites,
or add comments to code you did not touch.

### Comments
Explain **why**, never **what**. The code already says what it does.

```java
// BAD:  // loop over transactions and add the amount
// GOOD: // D2: card payments settle earlier spending, so they are excluded here.
```

Do not leave `TODO`, commented-out code, or scaffolding in a finished change.

### Verify before claiming
Do not say "this should work" or "tests should pass". Run the command and read
the output. If you did not run it, say so plainly.

### Reading before writing
Read the file you are about to change, plus one neighbouring file of the same
kind, before editing. Most mistakes in this repo come from inventing a pattern
that already exists three files away.

### Don't create documentation
Do not create summary or changelog markdown files unless explicitly asked. Report
what changed in your reply instead.

---

## 9. Domain vocabulary

| Term | Meaning |
|---|---|
| **Statement** | One imported file, tied to exactly one account or card |
| **Transaction** | One row from a statement, or a manual cash entry |
| **Normalization** | Stripping UPI/NEFT prefixes, gateways, refs, city names from a narration |
| **Merchant** | Canonical payee derived from a normalized narration |
| **Classification** | Deciding a transaction's `TransactionType` |
| **Reconciliation** | Linking related transactions so money is not double-counted |
| **Transaction link** | A non-destructive relationship: duplicate, card payment, refund, cashback |
| **Review** | A suggested link awaiting the user's decision |
| **Learned rule** | A rule created from a user correction, applied to future imports |

### Enums — use these exact values

- `TransactionType`: `EXPENSE` `INCOME` `TRANSFER` `CREDIT_CARD_PAYMENT`
  `CASHBACK` `REFUND` `FEE` `INTEREST` `EMI` `SUBSCRIPTION`
  `VERIFICATION_CHARGE` `UNKNOWN`
- `ClassificationSource`: `USER_OVERRIDE` `MERCHANT_DATABASE` `EXACT_RULE`
  `FUZZY_RULE` `HISTORICAL` `PATTERN` `LLM` `UNKNOWN`
- `ReconciliationStatus`: `UNMATCHED` `MATCHED` `REVIEW_REQUIRED`
  `CONFIRMED_DUPLICATE` `CONFIRMED_TRANSFER` `CONFIRMED_CARD_PAYMENT`
- `ImportStatus`: `UPLOADED` `PROCESSING` `REVIEW_REQUIRED` `COMPLETED`
  `FAILED` `CANCELLED`

Do not invent new enum values. Adding one requires an entity change, a
`database/schema.sql` change, and a frontend type change together.

---

## 10. The classification engine

Strategies run in `@Order` sequence; the first confident match wins.

| Order | Strategy | Basis |
|---|---|---|
| 1 | `UserOverrideClassifier` | The user's own learned rule — always wins |
| 2 | `ExactMerchantClassifier` | Known merchant with a category |
| 3 | `HistoricalPatternClassifier` | ≥3 prior transactions, ≥60% agreement |
| 4 | `RuleBasedClassifier` | Keyword lists |
| 5 | `CategoryKeywordClassifier` | Category keyword match |
| 6 | `AmountPatternClassifier` | Amount shape (₹1–2 verification, EMI, round subscriptions) |

To add a strategy: implement the interface, give it an `@Order`, and add tests
covering both a match and a no-match. Never reorder existing strategies without
being asked — user overrides must stay first.

---

## 11. Testing

Backend uses JUnit 5 + Mockito + AssertJ. Mirror `BudgetServiceTest`.

```java
@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {
    @Mock BudgetRepository budgetRepository;
    @InjectMocks BudgetService budgetService;

    @Test
    void getAll_countsExpensesAndSubtractsRefunds() {   // name states the behaviour
        // ...
        assertThat(response.spent()).isEqualByComparingTo("3500.00");  // never isEqualTo
    }
}
```

- Compare `BigDecimal` with `isEqualByComparingTo`. `isEqualTo` fails on scale:
  `2.50 != 2.5`.
- Test names describe behaviour, not method names.
  Good: `getAll_countsExpensesAndSubtractsRefunds`.
  Bad: `testGetAll`.
- Always cover: empty collection, zero amount, and a null optional relationship.

---

## 12. Current state

Implemented: auth with refresh-token rotation, CSV/Excel/PDF import with column
mapping and async processing, the classification engine with a correction
feedback loop, duplicate detection, reconciliation with user review, dashboard
with trends and custom ranges, budgets, subscription detection, credit-card
utilisation and billing cycles, manual cash entry, and CSV export.

Known gaps, roughly in priority order:

1. **Self-transfer detection** — bank→bank moves between the user's own accounts
   are still counted as spending. This is D3 unenforced and is the most valuable
   remaining fix.
2. **No search, filter, or pagination** on transaction endpoints.
3. **No database migration tool.** `ddl-auto=update` plus hand-maintained
   `schema.sql`. Adding Flyway would de-risk everything else.
4. **No password reset flow.**
5. **Rate limiting covers login only.**
6. **No frontend tests.**
