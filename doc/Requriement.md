# Finapse — Software Requirements Specification

**Version:** 1.0
**Status:** MVP Requirements Baseline
**Product:** Finapse
**Platform:** Local Web Application

---

# 1. Purpose

This document defines the functional and non-functional requirements for the Finapse MVP.

It is the primary reference for determining what the system must do.

Any feature not defined here should not be considered part of the MVP unless explicitly approved and added to this document.

---

# 2. Product Constraints

The MVP has the following fixed constraints:

* Single user
* Single laptop
* Local application
* Local MySQL database
* CSV statements only
* Bank and credit-card statements supported
* User explicitly selects statement type during upload
* No authentication required
* No cloud synchronization
* No direct bank integrations
* No destructive automatic reconciliation

---

# 3. User Model

## FR-001 — Single User

The application shall support one logical user in the MVP.

There is no requirement for:

* User registration
* Login
* Password management
* User roles
* Multi-user collaboration

The architecture may remain extensible for future multi-user support.

---

# 4. Landing Page Requirements

## FR-010 — Product Introduction

The landing page shall explain what Finapse is and the problem it solves.

## FR-011 — Value Proposition

The landing page shall communicate that Finapse can:

* Track income
* Track actual spending
* Combine bank and credit-card activity
* Detect potential duplicate transactions
* Reconcile credit-card payments
* Track cashback
* Track refunds
* Provide financial insights

## FR-012 — Double-Counting Demonstration

The landing page should demonstrate the credit-card double-counting problem.

Example:

```text
Credit Card Purchase       ₹5,000
Bank Credit Card Payment   ₹5,000

Actual Spending            ₹5,000
```

The product should make it clear that:

```text
Credit Card Payment ≠ New Expense
```

## FR-013 — Primary Call to Action

The landing page shall provide a clear path into the application.

---

# 5. Statement Upload Requirements

## FR-020 — Statement Type Selection

Before uploading a statement, the user shall select:

```text
BANK
CREDIT_CARD
```

The system shall not attempt to automatically determine the statement type during MVP.

## FR-021 — Account/Card Selection

For a bank statement, the user shall select the associated bank account.

For a credit-card statement, the user shall select the associated credit card.

## FR-022 — CSV Upload

The user shall be able to upload a CSV file.

## FR-023 — File Validation

The system shall validate:

* File type
* File readability
* File size
* CSV structure
* Required transaction information

## FR-024 — Duplicate File Detection

The system shall detect if the same statement file has already been imported.

The system shall warn the user rather than importing the same file again.

## FR-025 — Processing Status

The user shall receive feedback during statement processing.

Possible states:

```text
UPLOADED
PROCESSING
REVIEW_REQUIRED
COMPLETED
FAILED
CANCELLED
```

---

# 6. CSV Processing Requirements

## FR-030 — CSV Parsing

The backend shall parse uploaded CSV files.

## FR-031 — Header Recognition

The system shall identify relevant transaction columns.

Possible source columns include:

```text
Date
Transaction Date
Posting Date
Description
Narration
Debit
Credit
Withdrawal
Deposit
Amount
Balance
```

## FR-032 — Column Normalization

Different source column names shall map to Finapse's standardized transaction fields.

Example:

```text
Withdrawal
Debit
Amount Debited
```

may map to:

```text
DEBIT
```

## FR-033 — Date Normalization

Supported source date formats shall be converted into a consistent internal representation.

## FR-034 — Amount Normalization

Financial amounts shall be normalized into:

```text
DECIMAL(15,2)
```

## FR-035 — Direction Detection

Transactions shall be classified as:

```text
DEBIT
CREDIT
```

## FR-036 — Invalid Row Detection

Invalid transaction rows shall be identified and reported.

They shall not be silently discarded.

## FR-037 — Source Traceability

Every imported transaction shall retain:

* Statement ID
* Original source row number

This allows the user and developers to trace a normalized transaction back to the original CSV.

---

# 7. Transaction Requirements

## FR-040 — Transaction Storage

Normalized transactions shall be persisted in MySQL.

## FR-041 — Unique Transaction ID

Every transaction shall have a unique internal identifier.

## FR-042 — Transaction Data

A transaction may contain:

```text
Date
Posted Date
Description
Amount
Direction
Transaction Type
Account
Card
Merchant
Category
Cashback
Source Statement
Source Row
Transaction Hash
Reconciliation Status
```

## FR-043 — Transaction Types

The system shall support:

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

## FR-044 — Financial Meaning

Transaction direction shall not automatically determine transaction type.

For example:

```text
DEBIT
```

may represent:

```text
EXPENSE
TRANSFER
CREDIT_CARD_PAYMENT
FEE
```

---

# 8. Account Requirements

## FR-050 — Bank Account Creation

The user shall be able to maintain bank accounts.

## FR-051 — Multiple Accounts

The user shall be able to maintain multiple bank accounts.

## FR-052 — Account Information

An account may contain:

* Account name
* Institution
* Last four digits
* Currency
* Active/inactive status

## FR-053 — Statement Association

Each bank statement shall be associated with one bank account.

---

# 9. Credit Card Requirements

## FR-060 — Credit Card Creation

The user shall be able to maintain credit cards.

## FR-061 — Multiple Cards

The user shall be able to maintain multiple credit cards.

## FR-062 — Card Information

A card may contain:

* Card name
* Issuer
* Last four digits
* Credit limit
* Billing cycle
* Payment due date
* Active/inactive status

## FR-063 — Card Statement Association

Each credit-card statement shall be associated with one credit card.

## FR-064 — Card Spending

Finapse shall calculate spending associated with a credit card.

## FR-065 — Card Cashback

Finapse shall calculate cashback associated with a credit card.

## FR-066 — Card Payment Tracking

Finapse shall identify bank transactions that represent payments toward credit cards.

---

# 10. Transaction Classification Requirements

## FR-070 — Classification

The system shall classify imported transactions into supported transaction types.

## FR-071 — Classification Evidence

Where practical, classification should use available evidence such as:

* Description
* Amount
* Direction
* Account/card source
* Merchant
* Date
* Related transactions

## FR-072 — Uncertain Classification

If Finapse cannot confidently determine the financial meaning of a transaction, it shall use:

```text
UNKNOWN
```

or create a reconciliation review where a relationship between transactions is suspected.

The system shall not make an irreversible financial classification based solely on uncertain evidence.

---

# 11. Reconciliation Requirements

Reconciliation is a core Finapse capability.

## FR-080 — Related Transaction Detection

The system shall identify potentially related transactions.

Supported relationship types:

```text
CREDIT_CARD_PAYMENT
TRANSFER
REFUND
DUPLICATE
CASHBACK
```

## FR-081 — Credit Card Payment Detection

The system shall identify potential bank transactions representing credit-card payments.

## FR-082 — Payment Matching

The reconciliation engine may use:

* Amount
* Date proximity
* Description
* Card/statement information
* Transaction type
* Other available transaction metadata

to identify possible relationships.

## FR-083 — Review Required

If a relationship is uncertain, the system shall create a:

```text
REVIEW_REQUIRED
```

state.

## FR-084 — No Automatic Ambiguous Decisions

The system shall never silently convert an uncertain relationship into a confirmed relationship.

## FR-085 — User Confirmation

The user shall be able to confirm or reject a proposed relationship.

## FR-086 — Relationship Persistence

Confirmed relationships shall be stored independently of the underlying transactions.

---

# 12. Credit Card Double-Counting Requirements

## FR-090 — Expense Exclusion

A confirmed credit-card bill payment shall not be counted as a new expense.

Example:

```text
Card Purchase       ₹5,000
Bank Payment        ₹5,000

Actual Spending     ₹5,000
```

## FR-091 — Transaction Preservation

Both transactions shall remain in the database.

The system shall not delete either transaction.

## FR-092 — Settlement Representation

The bank transaction shall be represented as:

```text
CREDIT_CARD_PAYMENT
```

once the relationship is confirmed.

## FR-093 — Settlement vs Spending

Finapse shall distinguish between:

```text
Card Spending
```

and:

```text
Card Bill Payment
```

throughout analytics and UI.

---

# 13. Duplicate Detection Requirements

## FR-100 — Duplicate Detection

The system shall identify potential duplicate transactions.

## FR-101 — Duplicate Evidence

Potential duplicates may be identified using:

* Date
* Amount
* Direction
* Description
* Account/card
* Transaction hash

## FR-102 — Duplicate Classification

A potential duplicate shall not automatically be deleted.

## FR-103 — Duplicate Review

Potential duplicates should be available for user review.

## FR-104 — Confirmed Duplicate

After user confirmation, the relationship shall be stored as:

```text
DUPLICATE
```

The original transactions shall remain available.

---

# 14. Cashback Requirements

## FR-110 — Cashback Detection

Finapse shall identify cashback transactions where supported by the statement data.

## FR-111 — Cashback Classification

Cashback shall use:

```text
CASHBACK
```

as its transaction type.

## FR-112 — Cashback Tracking

Finapse shall calculate cashback earned within a selected period.

## FR-113 — Card Cashback

Finapse shall calculate cashback per credit card.

## FR-114 — Cashback Association

Where sufficient information exists, cashback may be linked to the transaction that generated it.

---

# 15. Refund Requirements

## FR-120 — Refund Detection

Finapse shall identify potential refund transactions.

## FR-121 — Refund Classification

Refund transactions shall use:

```text
REFUND
```

as their transaction type.

## FR-122 — Refund Relationship

Where possible, refunds should be linked to their original transactions.

## FR-123 — Net Spending

Refunds shall reduce effective spending calculations.

Example:

```text
Purchase       ₹2,000
Refund         ₹2,000

Net Spending       ₹0
```

---

# 16. Merchant Requirements

## FR-130 — Merchant Identification

The system should identify merchants from transaction descriptions.

## FR-131 — Merchant Normalization

Similar merchant descriptions should be normalized where practical.

Example:

```text
SWIGGY
SWIGGY INDIA
SWIGGY PVT LTD
```

may map to:

```text
Swiggy
```

## FR-132 — Merchant Association

Transactions may be associated with normalized merchants.

## FR-133 — Merchant Analytics

Merchant information shall support spending analysis.

---

# 17. Category Requirements

## FR-140 — Transaction Categories

The system shall support spending categories.

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

## FR-141 — Category Assignment

Transactions may be associated with a category.

## FR-142 — Category Editing

The user should be able to change a transaction's category.

## FR-143 — Category Analytics

Finapse shall provide spending grouped by category.

---

# 18. Statement Review Requirements

## FR-150 — Import Preview

Before final import, the system should display:

* Statement type
* Account/card
* File name
* Transaction count
* Date range
* Transaction preview
* Potential duplicates
* Potential reconciliation items

## FR-151 — Import Confirmation

The user shall be able to confirm the import.

## FR-152 — Import Cancellation

The user shall be able to cancel the import before final persistence.

---

# 19. Money Dashboard Requirements

## FR-160 — Income

Display income for the selected period.

## FR-161 — Actual Spending

Display actual spending for the selected period.

Credit-card settlements and internal transfers shall not be counted as new expenses.

## FR-162 — Net Cash Flow

Display:

```text
Net Cash Flow
=
Income - Actual Spending
```

## FR-163 — Cashback

Display cashback earned for the selected period.

## FR-164 — Category Breakdown

Display spending by category.

## FR-165 — Merchant Breakdown

Display top merchants by spending.

## FR-166 — Spending Trends

Display spending over time.

## FR-167 — Reconciliation Alerts

Surface important items requiring user attention.

Examples:

```text
3 potential duplicates
2 possible card payments
1 possible refund
```

## FR-168 — Time Filters

Support:

```text
7 Days
30 Days
This Month
3 Months
6 Months
1 Year
Custom
```

---

# 20. Cards Dashboard Requirements

## FR-170 — Card List

Display all active credit cards.

## FR-171 — Card Summary

Display, where available:

* Credit limit
* Outstanding
* Available credit
* Spending
* Cashback
* Payment due date

## FR-172 — Card Details

Users shall be able to view activity associated with a specific card.

## FR-173 — Card Spending

Display spending associated with the selected card.

## FR-174 — Card Cashback

Display cashback associated with the selected card.

## FR-175 — Card Payments

Display payments made toward the selected card.

---

# 21. Error Handling Requirements

## FR-180 — Invalid File

Display a clear error when an uploaded CSV cannot be processed.

## FR-181 — Missing Required Data

Identify missing required transaction fields.

## FR-182 — Invalid Rows

Report invalid rows rather than silently ignoring them.

## FR-183 — Processing Failure

A failed statement import shall have a visible failure state.

## FR-184 — User-Friendly Errors

Internal implementation details and stack traces shall not be exposed to the user.

---

# 22. Data Integrity Requirements

## NFR-001 — Financial Precision

All monetary values shall use fixed-precision decimal storage.

Recommended:

```text
DECIMAL(15,2)
```

Floating-point types shall not be used for financial calculations.

## NFR-002 — Transaction Preservation

Imported financial transactions shall not be silently deleted.

## NFR-003 — Source Traceability

Every transaction must be traceable to its source statement and source CSV row where applicable.

## NFR-004 — Referential Integrity

Foreign-key relationships shall be enforced by the database.

---

# 23. Privacy Requirements

## NFR-010 — Local Storage

Financial data shall be stored locally in the user's MySQL database.

## NFR-011 — No Unnecessary External Transfer

Raw financial statements shall not be sent to external services in the MVP.

## NFR-012 — Sensitive Logging

The application shall avoid logging:

* Full account numbers
* Full card numbers
* Raw financial statements
* Sensitive transaction information unnecessarily

---

# 24. Performance Requirements

## NFR-020 — CSV Processing

The application shall process normal personal-finance CSV statements without unnecessary delays.

## NFR-021 — Non-Blocking UI

Long-running processing should not freeze the frontend.

## NFR-022 — Database Indexing

Frequently queried transaction fields shall be indexed.

Important indexes include:

```text
statement_id
account_id
card_id
transaction_date
transaction_type
category_id
merchant_id
transaction_hash
reconciliation_status
```

---

# 25. Maintainability Requirements

## NFR-030 — Layered Backend

Backend responsibilities shall be separated into:

```text
Controller
Service
Repository
Entity
DTO
```

## NFR-031 — Business Logic Isolation

Financial classification and reconciliation logic shall not be implemented directly inside controllers.

## NFR-032 — Testability

Core financial logic shall be independently testable.

Priority test areas:

* CSV parsing
* Transaction normalization
* Duplicate detection
* Credit-card reconciliation
* Refund detection
* Cashback detection
* Financial calculations

---

# 26. API Requirements

The backend shall expose REST APIs.

Initial API areas:

```text
Statements
Transactions
Accounts
Cards
Dashboard
Reconciliation
Categories
```

The exact API contract will be defined separately in:

```text
docs/06-API.md
```

---

# 27. Technical Requirements

The MVP shall use:

```text
Frontend:
Next.js
TypeScript
Tailwind CSS
shadcn/ui

Backend:
Java
Spring Boot
Spring Data JPA
Hibernate

Database:
MySQL

CSV:
Apache Commons CSV

Testing:
JUnit
Mockito

API:
REST
```

---

# 28. Explicitly Out of Scope

The following are not MVP requirements:

### File Formats

* Excel
* XLSX
* PDF
* Image statements
* OCR

### Financial Integrations

* Bank APIs
* Credit-card APIs
* UPI APIs
* Automatic bank synchronization

### Financial Domains

* Investments
* Stocks
* Mutual funds
* Tax management
* Loans
* Insurance

### Advanced Intelligence

* AI financial advisor
* Natural-language financial assistant
* Predictive financial forecasting
* Automated financial recommendations

### Platform Features

* User authentication
* Multi-user support
* Cloud synchronization
* Mobile applications
* Organization/workspace management

### Infrastructure

* Microservices
* Kafka
* Redis
* Kubernetes
* Distributed processing

---

# 29. MVP Acceptance Criteria

The MVP is considered functionally complete when a user can:

1. Create/select a bank account.
2. Create/select a credit card.
3. Select Bank or Credit Card statement type.
4. Upload a CSV statement.
5. Validate and parse the CSV.
6. Normalize transaction data.
7. Preview transactions.
8. Import transactions into MySQL.
9. Preserve source statement and row information.
10. Detect potential duplicates.
11. Detect potential credit-card payments.
12. Create reconciliation reviews.
13. Confirm or reject reconciliation suggestions.
14. Prevent confirmed credit-card payments from being counted as new expenses.
15. Detect cashback where available.
16. Detect refunds where possible.
17. Categorize transactions.
18. View income.
19. View actual spending.
20. View net cash flow.
21. View category spending.
22. View card spending.
23. View card cashback.
24. View reconciliation items requiring attention.

---

# 30. Core Business Rule

The most important requirement in Finapse is:

> **A transaction appearing in a financial statement does not automatically represent an expense.**

Therefore:

```text
DEBIT ≠ EXPENSE
```

and:

```text
CREDIT CARD PAYMENT ≠ NEW SPENDING
```

The system must determine the financial meaning of transactions before including them in financial analytics.

---

# 31. Requirements Priority

### P0 — Mandatory MVP

```text
CSV Upload
Transaction Parsing
Transaction Normalization
Transaction Storage
Bank Accounts
Credit Cards
Transaction Classification
Duplicate Detection
Credit Card Reconciliation
Review Required Workflow
Actual Spending Calculation
Income
Cash Flow
Statement Management
```

### P1 — MVP Supporting Features

```text
Merchant Normalization
Categories
Cashback
Refund Detection
Card Analytics
Spending Analytics
```

### P2 — Post-MVP

```text
Advanced AI Classification
Budgeting
Subscriptions
Financial Forecasting
Investment Tracking
Bank Integrations
Excel/PDF Support
Cloud Sync
Mobile Applications
```

---

# 32. Requirement Governance

These requirements are the source of truth for the Finapse MVP.

If implementation decisions conflict with these requirements:

1. Identify the conflict.
2. Do not silently change the requirement.
3. Update the requirements document if the product decision changes.
4. Ensure affected architecture, database, API, and UI documentation are updated accordingly.

The application should evolve from the requirements rather than allowing implementation convenience to redefine the product.
