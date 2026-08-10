# Finapse — Frontend Architecture & UI/UX Specification

**Version:** 1.0
**Status:** MVP Baseline
**Frontend:** Next.js + TypeScript
**Styling:** Tailwind CSS
**Components:** shadcn/ui
**Backend:** Spring Boot REST API

---

# 1. Frontend Objective

The Finapse frontend should provide a clean financial command center that allows the user to:

* Understand their money flow
* Track income and spending
* Manage bank accounts and credit cards
* Upload statements
* Review potential duplicates
* Review reconciliation suggestions
* Understand cashback and refunds

The UI should prioritize:

> **Clarity over visual complexity.**

Financial information should be immediately understandable.

---

# 2. Frontend Architecture

Finapse will use:

```text
Next.js
    +
TypeScript
    +
Tailwind CSS
    +
shadcn/ui
```

Architecture:

```text
┌─────────────────────────────────────────────┐
│                  Next.js                    │
│                                             │
│  ┌─────────────┐      ┌─────────────────┐  │
│  │    Pages    │─────►│    Components   │  │
│  └─────────────┘      └────────┬────────┘  │
│                                │           │
│                                ▼           │
│                       ┌─────────────────┐  │
│                       │ API / Services  │  │
│                       └────────┬────────┘  │
└────────────────────────────────┼───────────┘
                                 │
                              REST API
                                 │
                                 ▼
                         Spring Boot Backend
```

---

# 3. Application Structure

The MVP contains four primary areas:

```text
Landing
   ↓
Money
   ↓
Cards
   ↓
Statements
```

The application should use a persistent sidebar/navigation after entering the main application.

---

# 4. Page Structure

## Public

```text
/
└── Landing Page
```

## Application

```text
/app
│
├── /money
│
├── /cards
│
├── /statements
│
└── /settings
```

`Settings` can initially be minimal and may be implemented later.

---

# 5. Landing Page

Route:

```text
/
```

Purpose:

Introduce Finapse and communicate the core problem.

---

## 5.1 Hero Section

The hero should immediately communicate:

> **Understand where your money actually goes.**

Supporting message:

> Import your bank and credit-card statements, reconcile transactions, eliminate double-counting, and see your real cash flow.

Primary CTA:

```text
Get Started
```

Secondary CTA:

```text See How It Works
```

---

## 5.2 Double-Counting Visualization

The landing page should demonstrate Finapse's key differentiator.

Example:

```text
┌──────────────────────┐
│ Credit Card Purchase │
│                      │
│ Amazon       ₹2,000  │
└──────────┬───────────┘
           │
           │
           ▼
┌──────────────────────┐
│ Credit Card Payment  │
│                      │
│ Bank         ₹2,000  │
└──────────────────────┘

        Finapse
           ↓

      Actual Spending
           ₹2,000
```

The visual should make the concept understandable without reading technical documentation.

---

# 6. Application Shell

Once the user enters the application, use a persistent layout.

```text
┌─────────────────────────────────────────────────────┐
│ FINAPSE                              Notifications   │
├───────────────┬─────────────────────────────────────┤
│               │                                     │
│  Money        │                                     │
│  Cards        │            Page Content             │
│  Statements   │                                     │
│               │                                     │
│               │                                     │
│  ─────────    │                                     │
│  Settings     │                                     │
│               │                                     │
└───────────────┴─────────────────────────────────────┘
```

---

# 7. Navigation

Primary navigation:

```text
Money
Cards
Statements
```

Secondary:

```text
Settings
```

The active section must be visually obvious.

---

# 8. Money Page

Route:

```text
/app/money
```

This is the **core Finapse page**.

The page should answer:

> How is my money moving?

---

## 8.1 Page Header

Display:

```text
Money

Your financial overview
```

Include a date-range selector:

```text
This Month
```

with options:

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

# 9. Money Summary Cards

Top-level cards:

```text
┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ Income       │ │ Spending     │ │ Cash Flow    │ │ Cashback     │
│ ₹26,399      │ │ ₹8,450       │ │ ₹17,949      │ │ ₹325         │
└──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
```

These should be the first major visual elements after the header.

---

# 10. Cash Flow Chart

Primary chart:

```text
Income vs Spending
```

Example:

```text
₹30K ┤      ╭────╮
     │      │    │
₹20K ┤╭────╮│    │
     ││    ││    │
₹10K ┤│    │╰────╯
     └────────────────
       W1   W2   W3   W4
```

The chart should allow the user to understand financial movement over time.

---

# 11. Spending Breakdown

Display:

```text
Spending by Category
```

Example:

```text
Food & Dining       ₹3,200
Shopping            ₹2,400
Transportation      ₹1,200
Bills               ₹1,000
Entertainment         ₹650
```

A donut/pie visualization may be used, but the numeric breakdown must remain visible.

Do not rely solely on charts.

---

# 12. Top Merchants

Display the merchants contributing most to spending.

Example:

```text
Top Merchants

Swiggy          ₹2,100
Amazon          ₹1,850
Uber            ₹1,200
Netflix           ₹649
```

---

# 13. Financial Attention Section

The Money page must prominently surface transactions requiring user attention.

Example:

```text
Needs Your Review

⚠ 2 possible credit-card payments
⚠ 1 possible duplicate
⚠ 1 possible refund
```

CTA:

```text
Review
```

This is more important than decorative analytics.

---

# 14. Recent Transactions

Display a compact transaction list.

Example:

```text
Recent Transactions

Amazon
Shopping
07 Aug
- ₹1,500

Swiggy
Food & Dining
09 Aug
- ₹500

Salary
Income
01 Aug
+ ₹26,399
```

Each transaction should be clickable.

---

# 15. Transaction Details

Selecting a transaction opens a detail panel/modal.

Display:

```text
Merchant
Description
Amount
Date
Posted Date
Account/Card
Category
Transaction Type
Statement
Source Row
Reconciliation Status
```

If linked:

```text
Related Transactions
```

Example:

```text
SBI Credit Card Payment
₹2,000

Related:
Credit Card Settlement
```

---

# 16. Cards Page

Route:

```text
/app/cards
```

Purpose:

Provide a centralized view of credit cards.

---

# 17. Cards Overview

Display cards as visual cards.

Example:

```text
┌───────────────────────────────┐
│ SBI Cashback                  │
│                               │
│ •••• 4821                     │
│                               │
│ Outstanding     ₹12,450       │
│ Limit           ₹1,00,000     │
│ Available       ₹87,550       │
└───────────────────────────────┘
```

Each card should show:

* Card name
* Issuer
* Last four digits
* Outstanding
* Credit limit
* Available credit

---

# 18. Card Detail

Selecting a card should provide:

```text
Card Overview
Spending
Cashback
Payments
Transactions
```

Summary:

```text
Current Spending
Cashback Earned
Outstanding
Available Credit
```

---

# 19. Card Spending

The card page must clearly separate:

```text
Card Purchases
```

from:

```text
Credit Card Payments
```

Example:

```text
Card Purchases       ₹8,250
Card Payments        ₹8,250
```

These are not both counted as spending.

---

# 20. Card Cashback

Display:

```text
Cashback Earned

August
₹325
```

Optionally show cashback by transaction:

```text
Amazon
₹1,500
Cashback
₹75
```

---

# 21. Statements Page

Route:

```text
/app/statements
```

Purpose:

Manage financial statement imports.

---

# 22. Statement Upload

The primary action should be:

```text
+ Upload Statement
```

Upload flow:

```text
Step 1
Select Statement Type

○ Bank Statement
○ Credit Card Statement

        ↓

Step 2
Select Account / Card

        ↓

Step 3
Upload CSV

        ↓

Step 4
Preview

        ↓

Step 5
Review

        ↓

Step 6
Import
```

---

# 23. Statement Type Selection

The user must explicitly select:

```text
Bank Statement
```

or:

```text
Credit Card Statement
```

Do not hide this decision.

---

# 24. CSV Upload Area

Use a drag-and-drop upload area.

Example:

```text
┌───────────────────────────────────────────┐
│                                           │
│              Upload CSV                   │
│                                           │
│       Drag & drop your statement          │
│       or click to browse                  │
│                                           │
│       CSV files only                      │
│                                           │
└───────────────────────────────────────────┘
```

The UI must clearly communicate that the MVP accepts CSV only.

---

# 25. Statement Preview

After parsing:

```text
Statement Preview

File:
hdfc_august_2026.csv

Type:
Bank Statement

Transactions:
142

Period:
01 Aug 2026 → 31 Aug 2026
```

Then show transaction preview.

---

# 26. Import Review

Before finalizing:

```text
Import Summary

142 transactions detected

Potential duplicates      3
Possible card payments    2
Possible refunds          1

[ Cancel ]
[ Import Statement ]
```

This is an important safety checkpoint.

---

# 27. Statement History

Display imported statements:

```text
┌────────────────────────────────────────────────────────────┐
│ File                       Type        Transactions Status │
├────────────────────────────────────────────────────────────┤
│ hdfc_august_2026.csv       Bank        142          ✓      │
│ sbi_august_2026.csv        Card        86           ✓      │
└────────────────────────────────────────────────────────────┘
```

---

# 28. Review Required UI

This is a core interaction.

Example:

```text
┌──────────────────────────────────────────────────────┐
│ Possible Credit Card Payment                         │
│                                                      │
│ Bank Transaction                                     │
│ SBI CREDIT CARD PAYMENT                 ₹2,000       │
│                                                      │
│ Possible Related Card Activity                       │
│ Credit Card Statement                    ₹2,000       │
│                                                      │
│ Confidence                               91.5%       │
│                                                      │
│ Why?                                                 │
│ Amount and transaction descriptions strongly match. │
│                                                      │
│ [ Reject ]                          [ Confirm ]      │
└──────────────────────────────────────────────────────┘
```

The user must understand:

* What Finapse detected
* Why it detected it
* What will happen after confirmation

---

# 29. Review States

Use clear states:

```text
PENDING
APPROVED
REJECTED
```

For transaction relationships:

```text
SUGGESTED
REVIEW_REQUIRED
CONFIRMED
REJECTED
```

---

# 30. Design Principles

## 30.1 Financial Clarity

Numbers must be readable immediately.

Do not overload screens with unnecessary visualizations.

---

## 30.2 Hierarchy

Every page should follow:

```text
Page Context
     ↓
Primary Metrics
     ↓
Important Insights
     ↓
Detailed Data
```

---

## 30.3 Review First

Potential financial issues should be more visually prominent than secondary analytics.

---

## 30.4 Progressive Disclosure

Do not display every transaction attribute immediately.

Show:

```text
Merchant
Amount
Date
Category
```

first.

Detailed metadata appears when the user opens the transaction.

---

## 30.5 Consistent Financial Semantics

Use consistent terminology everywhere.

Correct:

```text
Spending
Income
Cashback
Refund
Card Payment
Transfer
```

Avoid ambiguous labels such as:

```text
Money Out
Money In
Adjustment
Other Payment
```

unless technically necessary.

---

# 31. Color Semantics

Colors should communicate financial meaning consistently.

Recommended semantic system:

```text
Income / Positive
→ Positive semantic color

Expense / Negative
→ Negative semantic color

Warning / Review
→ Warning semantic color

Neutral
→ Neutral semantic color
```

Do not rely on color alone.

Example:

```text
₹5,000
EXPENSE
```

should communicate the meaning through text as well as visual styling.

---

# 32. Typography

Use a modern, highly readable sans-serif typeface.

Hierarchy:

```text
Page Title
    ↓
Section Title
    ↓
Metric
    ↓
Supporting Label
    ↓
Metadata
```

Large financial values should use strong typographic hierarchy.

Example:

```text
₹26,399
Income this month
```

rather than:

```text
Income this month: ₹26,399
```

---

# 33. Component Architecture

Recommended frontend structure:

```text
frontend/
└── src/
    ├── app/
    │   ├── page.tsx
    │   └── app/
    │       ├── money/
    │       ├── cards/
    │       └── statements/
    │
    ├── components/
    │   ├── layout/
    │   ├── navigation/
    │   ├── dashboard/
    │   ├── transactions/
    │   ├── cards/
    │   ├── statements/
    │   ├── reconciliation/
    │   └── ui/
    │
    ├── lib/
    │   ├── api/
    │   ├── utils/
    │   └── constants/
    │
    ├── hooks/
    │
    ├── types/
    │
    └── services/
```

---

# 34. Component Responsibilities

## `layout/`

Application shell.

```text
AppShell
Sidebar
Header
PageContainer
```

## `navigation/`

```text
SidebarNav
NavItem
Breadcrumbs
```

## `dashboard/`

```text
FinancialSummary
CashFlowChart
SpendingBreakdown
TopMerchants
AttentionPanel
RecentTransactions
```

## `transactions/`

```text
TransactionTable
TransactionRow
TransactionFilters
TransactionDetails
TransactionBadge
```

## `cards/`

```text
CreditCardTile
CardGrid
CardSummary
CardSpendingChart
CardCashback
CardTransactions
```

## `statements/`

```text
StatementUploader
StatementTypeSelector
AccountSelector
CardSelector
StatementPreview
ImportSummary
StatementHistory
```

## `reconciliation/`

```text
ReviewQueue
ReviewCard
RelationshipDetails
ReviewActions
```

---

# 35. State Management

Avoid introducing global state management unless the application actually requires it.

For MVP:

* Server state should come from API calls.
* Local UI state should use React state/hooks.
* Forms should use controlled/form-library patterns where appropriate.
* URL query parameters may be used for filters and date ranges.

Do not introduce Redux solely because the application is large enough to justify it conceptually.

---

# 36. API Integration

Frontend should communicate with Spring Boot through a dedicated API layer.

Example:

```text
components
    ↓
hooks / feature logic
    ↓
API service
    ↓
REST endpoint
```

Do not place raw `fetch()` calls throughout UI components.

Example:

```text
lib/api/
├── statements.ts
├── transactions.ts
├── cards.ts
├── dashboard.ts
└── reconciliation.ts
```

---

# 37. Type Safety

API responses should have corresponding TypeScript types.

Example:

```text
types/
├── transaction.ts
├── statement.ts
├── card.ts
├── dashboard.ts
└── reconciliation.ts
```

Avoid:

```typescript
const data: any = ...
```

unless there is a documented reason.

---

# 38. Loading States

Every data-dependent page must have a loading state.

Examples:

```text
Skeleton
Spinner
Loading placeholder
```

Avoid displaying an empty dashboard while data is still loading.

---

# 39. Empty States

Empty states should explain what the user should do next.

Example:

```text
No statements yet.

Upload your first bank or credit-card statement
to start understanding your spending.

[ Upload Statement ]
```

Avoid:

```text
No data.
```

---

# 40. Error States

Errors must be actionable.

Bad:

```text
Something went wrong.
```

Better:

```text
We couldn't process this CSV.

Check that the file contains transaction
date, description and amount information.

[ Try Again ]
```

---

# 41. Responsive Design

The application should support:

```text
Desktop
Tablet
Mobile
```

However, desktop is the primary MVP target because Finapse is initially a local laptop application.

The desktop layout should receive the highest design priority.

---

# 42. Accessibility

The frontend must support:

* Keyboard navigation
* Semantic HTML
* Accessible form labels
* Focus states
* Sufficient contrast
* Screen-reader-friendly controls
* Non-color-only status indicators

Interactive elements must have meaningful labels.

---

# 43. Financial Number Formatting

All currency values should use consistent formatting.

Default currency:

```text
INR
```

Example:

```text
₹26,399.00
```

For compact dashboard displays:

```text
₹26.4K
```

may be used where appropriate.

Do not use inconsistent formats across pages.

---

# 44. Transaction Display Rules

Transactions should visually distinguish:

```text
Income
Expense
Refund
Cashback
Transfer
Credit Card Payment
```

Example:

```text
Salary
+ ₹26,399
INCOME

Amazon
- ₹1,500
EXPENSE

Cashback
+ ₹75
CASHBACK
```

---

# 45. Review Interaction Rules

For every reconciliation review, the UI must show:

1. Relationship type
2. Source transaction
3. Target transaction
4. Amounts
5. Dates
6. Confidence
7. Reason
8. Confirm action
9. Reject action

Never present only:

```text
"Possible match found."
```

without context.

---

# 46. Frontend Performance

The frontend should:

* Avoid unnecessary API requests
* Use pagination for large transaction lists
* Avoid rendering thousands of rows simultaneously
* Lazy-load non-critical components where useful
* Cache stable data where appropriate
* Debounce search/filter requests where appropriate

For MVP-scale personal data, complexity should remain reasonable.

---

# 47. Frontend Security

The frontend must not:

* Store database credentials
* Connect directly to MySQL
* Trust user-provided financial classifications
* Render raw HTML from transaction descriptions
* Expose sensitive backend configuration

Architecture must remain:

```text
Browser
  ↓
Spring Boot API
  ↓
MySQL
```

Never:

```text
Browser
  ↓
MySQL
```

---

# 48. Frontend Development Order

Implementation should proceed in this order:

```text
1. Application shell
        ↓
2. Navigation
        ↓
3. Design system
        ↓
4. Landing page
        ↓
5. Money page skeleton
        ↓
6. Cards page skeleton
        ↓
7. Statements page skeleton
        ↓
8. API integration
        ↓
9. Transaction views
        ↓
10. Statement upload
        ↓
11. Review workflow
        ↓
12. Dashboard analytics
        ↓
13. Loading/error/empty states
        ↓
14. Responsive refinement
        ↓
15. Accessibility refinement
```

---

# 49. Frontend Definition of Done

A frontend feature is complete when:

* It follows the Finapse design system.
* It is responsive.
* Loading states exist.
* Empty states exist where applicable.
* Error states exist.
* API calls use the centralized API layer.
* TypeScript types are defined.
* No unnecessary `any` types are used.
* Accessibility basics are implemented.
* Financial terminology is consistent.
* Sensitive data is not unnecessarily exposed.
* The feature matches the approved requirements.

---

# 50. Core Frontend Principle

The Finapse frontend should follow:

> **Show the user what matters first, and let them investigate the details when needed.**

The visual hierarchy should always prioritize:

```text
Financial Health
      ↓
Important Exceptions
      ↓
Insights
      ↓
Transactions
      ↓
Raw Details
```

Finapse should feel like a **financial command center**, not a spreadsheet viewer.
