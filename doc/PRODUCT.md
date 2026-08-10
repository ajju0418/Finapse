# Finapse — Product Definition

**Version:** 1.0
**Status:** MVP Product Definition
**Project Type:** Personal Finance Intelligence Platform

---

# 1. Product Overview

**Finapse** is a privacy-first personal finance intelligence platform that helps a user understand their actual financial activity by combining and analyzing bank and credit-card statements.

The platform accepts financial statements in **CSV format**, extracts and normalizes transaction data, identifies transaction types, detects potential duplicates, reconciles related transactions, tracks cashback and refunds, and provides a unified view of income, spending, and cash flow.

Finapse is designed to solve a common problem with traditional expense trackers:

> **A financial transaction appearing in a statement does not necessarily represent a new expense.**

For example, when a user purchases something using a credit card and later pays the credit-card bill from their bank account, both transactions appear as debits across different statements.

Finapse must recognize that relationship and avoid counting the credit-card payment as a second expense.

---

# 2. Product Vision

> **Finapse helps users understand what actually happened to their money, not simply what transactions appeared in their statements.**

The long-term vision is to build an intelligent personal financial command center that can understand:

* Income
* Spending
* Cash flow
* Bank accounts
* Credit cards
* Transfers
* Credit-card payments
* Cashback
* Refunds
* Duplicate transactions
* Merchant behavior
* Spending patterns
* Financial trends

---

# 3. Core Value Proposition

Traditional expense trackers primarily answer:

> "What transactions occurred?"

Finapse aims to answer:

> **"What actually happened to my money?"**

The product achieves this through:

```text
Financial Statements
        ↓
Transaction Normalization
        ↓
Transaction Classification
        ↓
Reconciliation
        ↓
Duplicate Detection
        ↓
Cashback / Refund Detection
        ↓
Financial Analytics
```

---

# 4. Core Problem

A user's financial information is fragmented across multiple accounts and statements.

A typical user may have:

* Multiple bank accounts
* Multiple credit cards
* Different transaction descriptions
* Credit-card bill payments
* Cashback
* Refunds
* Transfers
* Duplicate records

Simply adding all debit transactions produces an inaccurate representation of spending.

### Example

Credit-card statement:

```text
Amazon Purchase
₹2,000
```

Bank statement:

```text
Credit Card Bill Payment
₹2,000
```

A simple expense tracker may calculate:

```text
₹2,000 + ₹2,000 = ₹4,000
```

Finapse should calculate:

```text
Actual Spending = ₹2,000
```

The bank payment is recognized as a **credit-card settlement**, not a new expense.

---

# 5. Core Product Principles

## 5.1 Financial Accuracy

Finapse must prioritize correct financial interpretation over simple transaction counting.

## 5.2 No Destructive Reconciliation

Transactions must never be deleted simply because they appear to be duplicates or related.

The original financial records must remain available.

## 5.3 Explainability

When Finapse makes an important classification or reconciliation decision, the system should be able to explain why.

Example:

```text
Possible Credit Card Payment

Reason:
Matching amount and payment description
between bank and credit-card statements.
```

## 5.4 User Control

When the system is uncertain, the transaction relationship must be sent to:

```text
REVIEW_REQUIRED
```

The user makes the final decision.

## 5.5 Privacy First

Financial data is sensitive.

The MVP is designed as a **single-user, single-laptop application**, keeping financial data within the user's local environment.

---

# 6. Target User

The initial target user is an individual who manages multiple financial accounts and wants an accurate view of their personal spending.

Typical user:

```text
Bank Account(s)
      +
Credit Card(s)
      +
Multiple Statements
      ↓
      Finapse
      ↓
Unified Financial Picture
```

---

# 7. Product Scope — MVP

The first version of Finapse will contain four primary application areas:

### 1. Landing Page

Introduces Finapse and explains its value proposition.

### 2. Money

The core financial dashboard.

Displays:

* Income
* Spending
* Cash flow
* Cashback
* Spending categories
* Top merchants
* Reconciliation information

### 3. Cards

Credit-card management and financial activity.

Displays:

* Cards
* Card limits
* Outstanding amounts
* Card spending
* Cashback
* Payment information

### 4. Statements

Statement import and processing.

Allows users to:

* Select statement type
* Upload CSV
* Preview transactions
* Review processing results
* Import the statement

---

# 8. Statement Types

The MVP supports exactly two statement types:

```text
BANK
CREDIT_CARD
```

The user explicitly selects the statement type before uploading.

The system does not need to automatically determine whether a statement is a bank or credit-card statement.

### Bank Statement

Associated with a bank account.

### Credit Card Statement

Associated with a credit card.

---

# 9. Core User Flow

```text
User
  ↓
Statements
  ↓
Select Statement Type
  ↓
Upload CSV
  ↓
CSV Validation
  ↓
Transaction Extraction
  ↓
Transaction Normalization
  ↓
Transaction Classification
  ↓
Duplicate / Reconciliation Analysis
  ↓
Review Required Items
  ↓
Confirm Import
  ↓
Financial Dashboard
```

---

# 10. Transaction Intelligence

Finapse's core intelligence is the ability to understand the meaning of transactions.

A transaction can be:

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

A transaction's debit/credit direction must not automatically determine its financial meaning.

For example:

```text
Bank Debit
₹5,000
```

could be:

```text
Expense
Transfer
Credit Card Payment
Fee
```

The system must determine the appropriate classification.

---

# 11. Reconciliation

Reconciliation is the primary differentiating feature of Finapse.

The system should identify relationships such as:

```text
Bank Payment
      ↕
Credit Card Settlement
```

```text
Purchase
      ↕
Refund
```

```text
Purchase
      ↕
Cashback
```

```text
Transaction
      ↕
Potential Duplicate
```

Relationships should be stored rather than deleting or replacing the underlying transactions.

---

# 12. Review-First Principle

If Finapse cannot confidently determine the relationship between transactions, it must not automatically make a financial decision.

Instead:

```text
Possible Match
      ↓
REVIEW_REQUIRED
      ↓
User Decision
      ↓
Confirmed / Rejected
```

Example:

```text
Possible Credit Card Payment

Bank Transaction      ₹5,000
Card Settlement       ₹5,000

Confidence             91%

[ Confirm ]
[ Reject ]
```

The user's decision becomes the authoritative classification.

---

# 13. Duplicate Detection

Finapse should identify multiple forms of duplication.

### Exact Duplicate

The same transaction appears more than once.

### Potential Duplicate

Two transactions have highly similar characteristics but cannot be confidently identified as duplicates.

### Related Transaction

Two transactions are not duplicates but represent the same financial movement.

Example:

```text
Bank → Credit Card Payment
```

This distinction is critical.

Finapse must never treat all matching amounts as duplicates.

---

# 14. Cashback Intelligence

Cashback is treated as a distinct financial event.

Example:

```text
Amazon Purchase
₹2,000

Cashback
₹100
```

Finapse should understand:

```text
Gross Purchase      ₹2,000
Cashback Earned       ₹100
Effective Cost       ₹1,900
```

When cashback appears as a separate transaction, Finapse may establish a relationship between the cashback and the original purchase.

---

# 15. Refund Intelligence

Refunds should be distinguished from ordinary income.

Example:

```text
Purchase
₹2,000

Refund
₹2,000
```

Finapse should calculate:

```text
Gross Spending       ₹2,000
Refund               ₹2,000
Net Spending             ₹0
```

Where possible, the refund should be linked to the original transaction.

---

# 16. Merchant Intelligence

Transaction descriptions from financial institutions may contain inconsistent merchant names.

Examples:

```text
SWIGGY
SWIGGY INDIA
SWIGGY PVT LTD
SWIGGY*ORDER
```

Finapse should eventually normalize these into:

```text
SWIGGY
```

Merchant normalization improves:

* Spending analytics
* Category analysis
* Duplicate detection
* Merchant-level insights

---

# 17. Financial Dashboard

The Money page is the primary application page.

It should answer:

> How much money came in?

> How much did I actually spend?

> What is my current cash flow?

> Where did my money go?

> What financial transactions need my attention?

Primary metrics:

```text
Income
Spending
Net Cash Flow
Cashback
```

Supporting analytics:

```text
Spending by Category
Top Merchants
Spending Trends
Reconciliation Alerts
```

---

# 18. Cards Management

The Cards page acts as the user's credit-card command center.

For each card, Finapse should provide where sufficient information is available:

```text
Card Name
Issuer
Last Four Digits
Credit Limit
Current Outstanding
Available Credit
Spending
Cashback
Payment Due Date
```

The page should clearly distinguish:

```text
Card Spending
```

from:

```text
Card Bill Payment
```

because the payment must not become a second expense.

---

# 19. Statement Management

The Statements page is the primary data ingestion area.

The upload flow should be:

```text
Select:

○ Bank Statement
○ Credit Card Statement

        ↓

Upload CSV

        ↓

Process

        ↓

Preview

        ↓

Review

        ↓

Import
```

The preview should provide:

* Statement type
* Associated account/card
* File name
* Transaction count
* Date range
* Detected transactions
* Potential duplicates
* Potential reconciliation items

---

# 20. Privacy Model

Finapse MVP is:

```text
Single User
      +
Single Laptop
      +
Local Financial Data
```

No cloud financial synchronization is required.

No external bank APIs are required.

No authentication system is required for the MVP.

The architecture should remain extensible enough to support secure multi-user/cloud functionality in the future.

---

# 21. Technology Direction

### Frontend

```text
Next.js
TypeScript
Tailwind CSS
shadcn/ui
```

### Backend

```text
Java
Spring Boot
Spring Data JPA
Hibernate
```

### Database

```text
MySQL
```

### CSV Processing

```text
Apache Commons CSV
```

### API

```text
REST
OpenAPI
```

### Testing

```text
JUnit
Mockito
```

---

# 22. MVP Exclusions

The following are deliberately excluded from the first release:

* Excel files
* PDF statements
* OCR
* Automatic bank synchronization
* Bank APIs
* UPI APIs
* Investment tracking
* Tax management
* Loan management
* Automated bill payments
* Mobile applications
* Multi-user accounts
* Cloud synchronization
* Advanced AI financial advisor
* Natural-language financial assistant
* Microservices
* Kafka
* Redis
* Kubernetes

These may be considered in future versions.

---

# 23. Long-Term Vision

Once the transaction reconciliation foundation is stable, Finapse can evolve into a broader personal finance intelligence platform.

Potential future capabilities:

```text
Finapse
│
├── Smart Expense Categorization
├── AI Financial Insights
├── Spending Pattern Detection
├── Budget Recommendations
├── Subscription Detection
├── Financial Goal Tracking
├── Cash Flow Forecasting
├── Credit Card Optimization
├── Cashback Optimization
├── Investment Tracking
└── Natural Language Financial Assistant
```

The long-term objective is to move from:

> **Financial tracking**

to:

> **Financial understanding and intelligence.**

---

# 24. Product Definition

### Finapse

> **A personal finance intelligence and transaction reconciliation platform that transforms fragmented bank and credit-card statements into one accurate, understandable view of a user's money.**

### Core differentiator

> **Finapse understands the relationship between financial transactions instead of simply counting them.**

### Core principle

> **Don't count transactions. Understand them.**
