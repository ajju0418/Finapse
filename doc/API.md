# Finapse — API Reference

Base URL: `http://localhost:8080/api`

**This is the complete list of endpoints. If a route is not here, it does not
exist — add it deliberately rather than assuming it is available.**

## Conventions

- All endpoints except those marked **public** require
  `Authorization: Bearer <accessToken>`.
- All IDs are UUID strings.
- All money values are decimal numbers serialised from `BigDecimal`.
- All dates are ISO `yyyy-MM-dd`; timestamps are ISO-8601.
- Errors share one shape:

```json
{ "timestamp": "...", "status": 404, "code": "NOT_FOUND", "message": "Budget not found: <id>" }
```

| Status | `code` | Meaning |
|---|---|---|
| 400 | `VALIDATION_ERROR`, `BAD_REQUEST`, `INVALID_CSV` | Bad input |
| 401 | `UNAUTHORIZED` | Missing, expired or invalid access token |
| 403 | `FORBIDDEN` | Authenticated but not permitted |
| 404 | `NOT_FOUND` | Absent **or** owned by another user |
| 409 | `CONFLICT`, `DUPLICATE_STATEMENT` | Conflicts with existing state |
| 413 | `FILE_TOO_LARGE` | Upload over 10 MB |
| 422 | `STATEMENT_PROCESSING_ERROR` | File readable but not processable |
| 429 | `TOO_MANY_ATTEMPTS` | Login lockout; honour `Retry-After` |

> A resource owned by another user returns **404, not 403** — deliberately, so
> the API cannot be used to discover which IDs exist.

---

## Auth — `/api/auth`

| Method | Path | Notes |
|---|---|---|
| POST | `/register` | **public** · 201 · sets refresh cookie |
| POST | `/login` | **public** · rate-limited per email **and** IP |
| POST | `/refresh` | **public** · reads the httpOnly cookie, rotates it |
| POST | `/logout` | **public** · always succeeds so a stale client can clear state |
| GET | `/me` | Current user |
| POST | `/change-password` | Revokes **all** other sessions, re-issues this one |

Access tokens are HS256 JWTs, 15 min, held in browser memory only. Refresh
tokens are opaque, SHA-256 hashed at rest, in an httpOnly cookie
(`finapse_refresh`), rotated on every use. Replaying a rotated token revokes the
whole family.

---

## Accounts — `/api/accounts`

| Method | Path | Notes |
|---|---|---|
| GET | `/` | List |
| GET | `/{id}` | Single |
| GET | `/{id}/analytics` | Inflow, outflow, net change |
| POST | `/` | Create |
| PUT | `/{id}` | Update |
| PATCH | `/{id}/deactivate` | Soft-deactivate |
| DELETE | `/{id}` | 409 if statements are linked |

## Cards — `/api/cards`

Same shape as accounts. `GET /{id}/analytics` additionally returns
`utilizationPercent`, `utilizationBand` (`LOW` < 30, `MODERATE` ≤ 70, `HIGH`),
the current billing cycle window, `currentCycleSpend`, `nextDueDate` and
`daysUntilDue` — derived from `billingCycleDay` and `paymentDueDay`.

---

## Statements — `/api/statements`

| Method | Path | Notes |
|---|---|---|
| GET | `/` | List |
| GET | `/{id}` | **Poll this** to track an import |
| POST | `/preview` | Dry run — detected columns + sample rows, nothing saved |
| POST | `/upload` | **202** · returns `PROCESSING`, work continues in background |
| POST | `/{id}/reclassify` | Re-run classification; idempotent |
| DELETE | `/{id}` | Deletes the statement and its transactions |

Both `/preview` and `/upload` are `multipart/form-data`:

| Part | Required | Notes |
|---|---|---|
| `file` | yes | `.csv` `.xls` `.xlsx` `.pdf`, max 10 MB |
| `statementType` | upload only | `BANK` or `CREDIT_CARD` |
| `accountId` | if `BANK` | Mutually exclusive with `cardId` |
| `cardId` | if `CREDIT_CARD` | Mutually exclusive with `accountId` |
| `columnMapping` | no | JSON part overriding auto-detected columns |

### Import flow

```
POST /preview   → confirm or remap columns
POST /upload    → 202, status PROCESSING
GET  /{id}      → poll until status leaves PROCESSING
                  COMPLETED | REVIEW_REQUIRED | FAILED (see importError)
```

Uploading a byte-identical file twice returns 409 — the file hash is stored.

---

## Transactions — `/api/transactions`

| Method | Path | Notes |
|---|---|---|
| GET | `/statement/{statementId}` | All rows in a statement |
| GET | `/card/{cardId}` | |
| GET | `/account/{accountId}` | |
| GET | `/{id}` | Single |
| POST | `/` | Manual cash entry · 201 |
| PATCH | `/{id}` | Correct classification **and teach the engine** |
| PATCH | `/{id}/type?type=` | Type only, no rule learned |
| PATCH | `/{id}/category?categoryId=` | Category only |
| DELETE | `/{id}` | **Manual entries only** — 400 for imported rows |

`PATCH /{id}` with `applyToSimilar: true` creates a `UserClassificationRule`
keyed on the *normalized* narration, so future imports classify the same way.

> These list endpoints are unpaginated and unfiltered. See gap #2 in
> [AGENTS.md](../AGENTS.md).

---

## Dashboard — `/api/dashboard`

| Method | Path | Notes |
|---|---|---|
| GET | `/?period=` | Summary |
| GET | `/trends?months=N` | Monthly series, max 36 |

`period`: `THIS_MONTH` `LAST_MONTH` `7_DAYS` `30_DAYS` `3_MONTHS` `6_MONTHS`
`1_YEAR` `YTD` `CUSTOM`.
`CUSTOM` requires both `from` and `to`; the span must be ≤ 5 years.

The summary returns `income`, `grossExpenses`, `refunds`, `actualSpending`,
`cashback`, `netCashFlow`, a category breakdown, top merchants, recent
transactions, the pending review count, and per-source summaries.

`actualSpending = max(grossExpenses − refunds, 0)` and
`netCashFlow = income − actualSpending`. Cashback is reported separately and
card payments are excluded — see rules D1–D4.

---

## Budgets — `/api/budgets`

| Method | Path | Notes |
|---|---|---|
| GET | `/` | Each budget with spend, remaining, status, projection |
| POST | `/` | 201 · 409 if one already exists for that category + period |
| PUT | `/{id}` | Update |
| DELETE | `/{id}` | Does not affect transactions |

`categoryId: null` means the overall spending budget. `period` is `WEEKLY`,
`MONTHLY` or `YEARLY`. `status` is `ON_TRACK`, `AT_RISK` (past
`alertThreshold`, default 80%) or `EXCEEDED`. `projectedSpend` is a
straight-line run rate over elapsed days.

---

## Reconciliation — `/api/reconciliation`

| Method | Path | Notes |
|---|---|---|
| GET | `/reviews` | Pending suggested links |
| GET | `/reviews/count` | Badge count |
| POST | `/reviews/{id}/decide` | `APPROVED` or `REJECTED` |

Approving records the relationship. It never deletes a transaction — see D5.

---

## Other

| Method | Path | Notes |
|---|---|---|
| GET | `/api/categories` | All spend categories |
| GET | `/api/subscriptions` | Detected recurring charges |
| GET | `/api/rules` | Learned classification rules |
| DELETE | `/api/rules/{ruleId}` | Forget a rule |
| GET | `/api/export/transactions.csv` | CSV download, optional `from`/`to` |
| GET | `/api/health` | **public** · liveness + DB status |

Subscription detection requires ≥2 occurrences 25–35 days apart **and** a
non-null merchant. A single month of data legitimately returns an empty list.

The CSV export prefixes a leading `= + - @` with `'` to prevent spreadsheet
formula injection. Preserve that when editing the exporter.

---

## Adding an endpoint

1. Controller method — thin, delegates immediately to a service.
2. Request DTO as a `record` with `jakarta.validation` annotations; `@Valid` in
   the controller.
3. Service holds the logic and the `@Transactional` boundary.
4. Scope every user-owned lookup with `findByIdAndUserId` (rule S1).
5. Add the frontend client in `frontend/lib/api/` and types in `frontend/types/`.
6. Add a service test.
7. **Update this file.**
