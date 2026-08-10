# Finapse — Database Schema & Data Model

**Version:** 1.0
**Status:** MVP Baseline
**Database:** MySQL 8.0+
**ORM:** Spring Data JPA / Hibernate

---

# 1. Database Philosophy

Finapse stores **financial events**, not pre-calculated expenses.

A transaction is a raw financial event imported from a statement.

Its financial meaning is determined through:

```text
Transaction
    ↓
Classification
    ↓
Reconciliation
    ↓
Financial Analytics
```

The database must preserve the original transaction even when it is:

* A duplicate
* A credit-card payment
* A transfer
* A refund
* Cashback
* A reconciled transaction

**Never destructively delete a transaction during reconciliation.**

---

# 2. Core Entities

The MVP contains nine core entities:

```text
users
accounts
cards
statements
transactions
merchants
categories
transaction_links
reconciliation_reviews
```

---

# 3. Entity Relationship Diagram

```text
                         ┌──────────────┐
                         │    USERS     │
                         └──────┬───────┘
                                │
                   ┌────────────┼────────────┐
                   │            │            │
                   ▼            ▼            ▼
             ┌──────────┐ ┌──────────┐ ┌────────────┐
             │ ACCOUNTS │ │  CARDS   │ │ CATEGORIES │
             └────┬─────┘ └────┬─────┘ └──────┬─────┘
                  │            │              │
                  │            │              │
                  └──────┬─────┘              │
                         │                    │
                         ▼                    │
                  ┌────────────┐              │
                  │ STATEMENTS │              │
                  └─────┬──────┘              │
                        │                     │
                        │ 1:N                 │
                        ▼                     │
                 ┌──────────────┐             │
                 │ TRANSACTIONS │◄────────────┘
                 └──────┬───────┘
                        │
                        │ N:1
                        ▼
                  ┌───────────┐
                  │ MERCHANTS │
                  └───────────┘

                 TRANSACTION
                      │
                      │
                      ▼
             ┌──────────────────┐
             │ TRANSACTION_LINK │
             └────────┬─────────┘
                      │
                      ▼
           ┌────────────────────────┐
           │ RECONCILIATION_REVIEW  │
           └────────────────────────┘
```

---

# 4. USERS

## Table

```text
users
```

### Columns

| Column     | Type         | Constraints |
| ---------- | ------------ | ----------- |
| id         | CHAR(36)     | PK          |
| name       | VARCHAR(100) | NOT NULL    |
| created_at | DATETIME     | NOT NULL    |
| updated_at | DATETIME     | NOT NULL    |

### Purpose

Represents the single local Finapse user.

Authentication is not part of MVP.

---

# 5. ACCOUNTS

## Table

```text
accounts
```

Represents bank accounts.

### Columns

| Column           | Type         | Constraints   |
| ---------------- | ------------ | ------------- |
| id               | CHAR(36)     | PK            |
| user_id          | CHAR(36)     | FK            |
| name             | VARCHAR(150) | NOT NULL      |
| institution_name | VARCHAR(150) | NULL          |
| account_type     | ENUM         | NOT NULL      |
| last_four_digits | VARCHAR(4)   | NULL          |
| currency         | CHAR(3)      | DEFAULT `INR` |
| is_active        | BOOLEAN      | DEFAULT TRUE  |
| created_at       | DATETIME     | NOT NULL      |
| updated_at       | DATETIME     | NOT NULL      |

### Account Types

```text
BANK
```

The MVP only supports bank accounts.

---

# 6. CARDS

## Table

```text
cards
```

Represents credit cards.

### Columns

| Column            | Type          | Constraints  |
| ----------------- | ------------- | ------------ |
| id                | CHAR(36)      | PK           |
| user_id           | CHAR(36)      | FK           |
| name              | VARCHAR(150)  | NOT NULL     |
| issuer            | VARCHAR(100)  | NULL         |
| last_four_digits  | VARCHAR(4)    | NULL         |
| credit_limit      | DECIMAL(15,2) | NULL         |
| billing_cycle_day | TINYINT       | NULL         |
| payment_due_day   | TINYINT       | NULL         |
| is_active         | BOOLEAN       | DEFAULT TRUE |
| created_at        | DATETIME      | NOT NULL     |
| updated_at        | DATETIME      | NOT NULL     |

---

# 7. STATEMENTS

## Table

```text
statements
```

Represents every imported CSV file.

### Columns

| Column             | Type         | Constraints  |
| ------------------ | ------------ | ------------ |
| id                 | CHAR(36)     | PK           |
| user_id            | CHAR(36)     | FK           |
| account_id         | CHAR(36)     | FK, nullable |
| card_id            | CHAR(36)     | FK, nullable |
| statement_type     | ENUM         | NOT NULL     |
| original_file_name | VARCHAR(255) | NOT NULL     |
| file_hash          | CHAR(64)     | NOT NULL     |
| transaction_count  | INT          | NOT NULL     |
| import_status      | ENUM         | NOT NULL     |
| period_start       | DATE         | NULL         |
| period_end         | DATE         | NULL         |
| uploaded_at        | DATETIME     | NOT NULL     |
| processed_at       | DATETIME     | NULL         |

### Statement Types

```text
BANK
CREDIT_CARD
```

### Source Rule

For a bank statement:

```text
account_id = required
card_id = NULL
```

For a credit-card statement:

```text
account_id = NULL
card_id = required
```

A statement must never reference both.

---

# 8. STATEMENT IMPORT STATUS

```text
UPLOADED
PROCESSING
REVIEW_REQUIRED
COMPLETED
FAILED
CANCELLED
```

Flow:

```text
UPLOADED
    ↓
PROCESSING
    ↓
COMPLETED
```

If issues require user attention:

```text
PROCESSING
    ↓
REVIEW_REQUIRED
```

---

# 9. TRANSACTIONS

## Table

```text
transactions
```

This is the **central entity in Finapse**.

### Columns

| Column                | Type          | Purpose                 |
| --------------------- | ------------- | ----------------------- |
| id                    | CHAR(36)      | Primary key             |
| statement_id          | CHAR(36)      | Source statement        |
| account_id            | CHAR(36)      | Bank source             |
| card_id               | CHAR(36)      | Credit-card source      |
| merchant_id           | CHAR(36)      | Normalized merchant     |
| category_id           | CHAR(36)      | Spending category       |
| transaction_date      | DATE          | Transaction date        |
| posted_date           | DATE          | Posted date             |
| description           | VARCHAR(500)  | Original description    |
| amount                | DECIMAL(15,2) | Transaction amount      |
| direction             | ENUM          | Debit/Credit            |
| transaction_type      | ENUM          | Financial meaning       |
| cashback_amount       | DECIMAL(15,2) | Cashback                |
| transaction_hash      | CHAR(64)      | Duplicate detection aid |
| reconciliation_status | ENUM          | Reconciliation state    |
| source_row_number     | INT           | Original CSV row        |
| created_at            | DATETIME      | Created time            |
| updated_at            | DATETIME      | Updated time            |

---

# 10. TRANSACTION SOURCE

Every transaction belongs to exactly one source.

### Bank transaction

```text
account_id = BANK_ACCOUNT
card_id = NULL
```

### Credit-card transaction

```text
account_id = NULL
card_id = CREDIT_CARD
```

Therefore:

```text
(account_id IS NOT NULL AND card_id IS NULL)
OR
(account_id IS NULL AND card_id IS NOT NULL)
```

---

# 11. TRANSACTION DIRECTION

Direction represents the movement shown by the statement.

```text
DEBIT
CREDIT
```

Important:

> Direction does not determine financial meaning.

Example:

```text
DEBIT ₹5,000
```

could be:

```text
EXPENSE
TRANSFER
CREDIT_CARD_PAYMENT
FEE
```

---

# 12. TRANSACTION TYPE

Supported values:

```text
EXPENSE
INCOME
TRANSFER
CREDIT_CARD_PAYMENT
CASHBACK
REFUND
FEE
INTEREST
UNKNOWN
```

Examples:

```text
Salary
→ CREDIT
→ INCOME

Amazon purchase
→ DEBIT
→ EXPENSE

Bank → Credit Card payment
→ DEBIT
→ CREDIT_CARD_PAYMENT

Cashback
→ CREDIT
→ CASHBACK

Refund
→ CREDIT
→ REFUND
```

---

# 13. RECONCILIATION STATUS

Supported values:

```text
UNMATCHED
MATCHED
REVIEW_REQUIRED
CONFIRMED_DUPLICATE
CONFIRMED_TRANSFER
CONFIRMED_CARD_PAYMENT
```

New transactions should normally begin as:

```text
UNMATCHED
```

---

# 14. MERCHANTS

## Table

```text
merchants
```

Used to normalize inconsistent merchant descriptions.

### Columns

| Column          | Type         |
| --------------- | ------------ |
| id              | CHAR(36) PK  |
| name            | VARCHAR(150) |
| normalized_name | VARCHAR(150) |
| category_id     | CHAR(36) FK  |
| created_at      | DATETIME     |
| updated_at      | DATETIME     |

Example:

```text
SWIGGY
SWIGGY INDIA
SWIGGY PVT LTD
SWIGGY*ORDER
```

can map to:

```text
Swiggy
```

---

# 15. CATEGORIES

## Table

```text
categories
```

Initial categories:

```text
Food & Dining
Groceries
Shopping
Transportation
Bills & Utilities
Entertainment
Healthcare
Travel
Education
Subscriptions
Other
```

Categories can later become hierarchical.

For MVP, a flat category structure is sufficient.

---

# 16. TRANSACTION LINKS

## Table

```text
transaction_links
```

This table represents relationships between financial transactions.

### Columns

| Column                | Type         |
| --------------------- | ------------ |
| id                    | CHAR(36) PK  |
| source_transaction_id | CHAR(36) FK  |
| target_transaction_id | CHAR(36) FK  |
| link_type             | ENUM         |
| confidence_score      | DECIMAL(5,2) |
| status                | ENUM         |
| reason                | VARCHAR(500) |
| created_at            | DATETIME     |
| reviewed_at           | DATETIME     |

---

# 17. TRANSACTION LINK TYPES

```text
CREDIT_CARD_PAYMENT
TRANSFER
REFUND
DUPLICATE
CASHBACK
```

Examples:

```text
Bank Payment
      │
      └── CREDIT_CARD_PAYMENT
              │
              ▼
       Card Transaction
```

```text
Purchase
      │
      └── REFUND
              │
              ▼
          Refund
```

```text
Purchase
      │
      └── CASHBACK
              │
              ▼
          Cashback
```

---

# 18. LINK STATUS

```text
SUGGESTED
REVIEW_REQUIRED
CONFIRMED
REJECTED
```

Finapse must never silently confirm an uncertain relationship.

---

# 19. CONFIDENCE SCORE

`confidence_score` represents how strongly the reconciliation engine believes two transactions are related.

Range:

```text
0.00 - 100.00
```

Example:

```text
91.50
```

Important:

> Confidence score is evidence, not authority.

Even a high-confidence ambiguous relationship may require user review.

---

# 20. RECONCILIATION REVIEWS

## Table

```text
reconciliation_reviews
```

Stores user-reviewable reconciliation decisions.

### Columns

| Column              | Type          |
| ------------------- | ------------- |
| id                  | CHAR(36) PK   |
| transaction_link_id | CHAR(36) FK   |
| review_type         | ENUM          |
| status              | ENUM          |
| system_reason       | VARCHAR(1000) |
| user_decision       | VARCHAR(100)  |
| created_at          | DATETIME      |
| reviewed_at         | DATETIME      |

---

# 21. REVIEW TYPES

```text
POSSIBLE_DUPLICATE
POSSIBLE_CARD_PAYMENT
POSSIBLE_TRANSFER
POSSIBLE_REFUND
POSSIBLE_CASHBACK
```

---

# 22. REVIEW STATUS

```text
PENDING
APPROVED
REJECTED
```

Workflow:

```text
Potential Relationship
        ↓
Transaction Link
        ↓
REVIEW_REQUIRED
        ↓
Reconciliation Review
        ↓
       PENDING
       /     \
   APPROVE   REJECT
      ↓        ↓
 CONFIRMED   REJECTED
```

---

# 23. CREDIT CARD PAYMENT MODEL

Example:

### Credit-card statement

```text
Amazon       ₹1,500
Swiggy         ₹500
```

### Bank statement

```text
SBI Card Payment       ₹2,000
```

Finapse stores:

```text
T1 → Amazon ₹1,500 → EXPENSE
T2 → Swiggy ₹500   → EXPENSE
T3 → Card Payment ₹2,000 → CREDIT_CARD_PAYMENT
```

A reconciliation relationship is created around the settlement.

For MVP:

> Finapse does not need to allocate the ₹2,000 payment individually across T1 and T2.

The important fact is:

```text
T3 = Credit Card Settlement
```

Therefore:

```text
Actual Spending = ₹2,000
```

not:

```text
₹2,000 + ₹2,000 = ₹4,000
```

---

# 24. Duplicate Detection

Duplicate detection operates at two levels.

## Statement Level

Each uploaded file receives a SHA-256 hash:

```text
file_hash
```

If the same user uploads the same file again:

```text
existing hash == new hash
```

the application should warn the user.

## Transaction Level

Transactions receive a fingerprint:

```text
account/card
+
date
+
amount
+
direction
+
normalized description
```

stored as:

```text
transaction_hash
```

The hash is a **duplicate detection aid**.

It is not proof that two transactions are duplicates.

---

# 25. Data Integrity Rules

The database must enforce:

### Rule 1

Every transaction belongs to exactly one statement.

### Rule 2

Every transaction has exactly one financial source:

```text
Bank Account
OR
Credit Card
```

### Rule 3

Every bank statement belongs to one bank account.

### Rule 4

Every credit-card statement belongs to one credit card.

### Rule 5

Money uses:

```text
DECIMAL(15,2)
```

Never floating-point types.

### Rule 6

Transactions are never deleted during reconciliation.

### Rule 7

A transaction cannot link to itself.

### Rule 8

Transaction relationships must reference valid transactions.

---

# 26. Delete Strategy

Financial records should be treated as historical records.

Therefore:

```text
Transaction
Statement
Transaction Link
```

should use restrictive deletion behavior.

The system should prefer:

```text
is_active = false
```

or status changes over destructive deletion.

For example:

```text
Card
→ is_active = false
```

rather than deleting the card and potentially breaking historical relationships.

---

# 27. Indexing Strategy

Important indexes:

```text
transactions.statement_id
transactions.account_id
transactions.card_id
transactions.transaction_date
transactions.transaction_type
transactions.category_id
transactions.merchant_id
transactions.transaction_hash
transactions.reconciliation_status

statements.user_id
statements.account_id
statements.card_id
statements.file_hash
statements.import_status

transaction_links.source_transaction_id
transaction_links.target_transaction_id
transaction_links.status
transaction_links.link_type

reconciliation_reviews.status
reconciliation_reviews.review_type
```

---

# 28. Derived Financial Metrics

The database stores transactions.

Financial metrics are calculated by the application/service layer.

### Income

```text
SUM(INCOME transactions)
```

### Gross Expenses

```text
SUM(EXPENSE transactions)
```

### Refunds

```text
SUM(REFUND transactions)
```

### Actual Spending

```text
Gross Expenses - Refunds
```

### Cashback

```text
SUM(CASHBACK transactions)
```

### Net Cash Flow

```text
Income - Actual Spending
```

Confirmed:

```text
CREDIT_CARD_PAYMENT
TRANSFER
```

must not be treated as new spending.

---

# 29. Database Layering

The database should be treated as:

```text
Raw Financial Events
        ↓
Normalized Transactions
        ↓
Relationships
        ↓
Classification
        ↓
Analytics
```

Do not store dashboard-specific totals directly in the transaction tables.

For example, do not add:

```text
monthly_spending
monthly_income
monthly_cashflow
```

to `transactions`.

These are derived values.

---

# 30. Source Traceability

Every transaction must be traceable:

```text
Transaction
     ↓
Statement
     ↓
Original CSV
     ↓
Source Row
```

Example:

```text
Transaction:
Amazon ₹1,500

Statement:
sbi_cashback_august_2026.csv

Source Row:
42
```

This is required for debugging and user verification.

---

# 31. Final Schema

```text
users
│
├── accounts
│      │
│      └── statements
│             │
│             └── transactions
│
├── cards
│      │
│      └── statements
│             │
│             └── transactions
│
└── statements

categories
     │
     └── transactions

merchants
     │
     └── transactions

transactions
     │
     └── transaction_links
                │
                └── reconciliation_reviews
```

---

# 32. Schema Source of Truth

The actual executable database definition is:

```text
database/schema.sql
```

This document explains the model and business intent.

Whenever the database structure changes:

1. Update `docs/05-DATA-MODEL.md`
2. Update `database/schema.sql`
3. Update corresponding JPA entities
4. Update affected API DTOs
5. Update tests

Never change the database schema silently.

---

# 33. MVP Table List

The final MVP database consists of:

```text
1. users
2. accounts
3. cards
4. categories
5. merchants
6. statements
7. transactions
8. transaction_links
9. reconciliation_reviews
```

This is intentionally small.

Do not introduce additional tables merely to accommodate future features unless the current MVP requirements genuinely require them.
