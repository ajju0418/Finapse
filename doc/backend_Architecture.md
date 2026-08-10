# Finapse — Backend Architecture

**Version:** 1.0
**Status:** MVP Architecture Baseline
**Backend:** Java + Spring Boot
**Database:** MySQL
**ORM:** Spring Data JPA / Hibernate
**API:** REST
**Architecture Style:** Modular Monolith + Layered Architecture

---

# 1. Architecture Objective

The Finapse backend shall provide a reliable, maintainable foundation for:

* CSV statement ingestion
* Transaction normalization
* Transaction classification
* Duplicate detection
* Transaction reconciliation
* Cashback detection
* Refund detection
* Financial analytics
* Credit-card management
* Statement management

The backend must prioritize:

1. Financial correctness
2. Data integrity
3. Separation of concerns
4. Testability
5. Explainability
6. Maintainability

---

# 2. Architecture Decision

Finapse MVP will use a:

> **Modular Monolith with Layered Architecture**

The application will be deployed as a single Spring Boot application.

```text id="4wfr0d"
                    FINAPSE BACKEND
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
     REST API          BUSINESS           DATA
     LAYER             LOGIC              ACCESS
        │                 │                 │
        ▼                 ▼                 ▼
   Controllers         Services         Repositories
        │                 │                 │
        └─────────────────┼─────────────────┘
                          │
                          ▼
                         MySQL
```

Microservices are explicitly **not required for the MVP**.

---

# 3. Why Modular Monolith

Finapse is:

* Single user
* Single laptop
* Local
* Relatively small in initial scope
* Primarily transaction-processing based

Microservices would introduce unnecessary complexity:

* Service discovery
* Network communication
* Multiple deployments
* Distributed transactions
* Additional infrastructure
* More difficult local development

A modular monolith gives us clear boundaries while keeping development simple.

---

# 4. High-Level System Architecture

```text id="9j1y7h"
┌────────────────────────────────────────────────────┐
│                    FINAPSE UI                      │
│                                                    │
│             Next.js + TypeScript                   │
└───────────────────────┬────────────────────────────┘
                        │
                        │ REST / JSON
                        ▼
┌────────────────────────────────────────────────────┐
│               SPRING BOOT BACKEND                  │
│                                                    │
│  ┌──────────────┐     ┌─────────────────────────┐ │
│  │ Controllers  │────►│      Application        │ │
│  └──────────────┘     │       Services          │ │
│                       └────────────┬────────────┘ │
│                                    │              │
│                       ┌────────────▼────────────┐ │
│                       │      Domain Logic       │ │
│                       │                         │ │
│                       │ Classification          │ │
│                       │ Reconciliation          │ │
│                       │ Duplicate Detection     │ │
│                       │ Normalization           │ │
│                       └────────────┬────────────┘ │
│                                    │              │
│                       ┌────────────▼────────────┐ │
│                       │      Repositories       │ │
│                       └────────────┬────────────┘ │
└────────────────────────────────────┼─────────────┘
                                     │
                                     ▼
                              ┌─────────────┐
                              │    MySQL    │
                              └─────────────┘
```

---

# 5. Backend Package Structure

The recommended package structure is:

```text id="d0l8kx"
backend/
└── src/
    └── main/
        └── java/
            └── com/
                └── finapse/
                    │
                    ├── FinapseApplication.java
                    │
                    ├── config/
                    │
                    ├── controller/
                    │
                    ├── dto/
                    │
                    ├── entity/
                    │
                    ├── enums/
                    │
                    ├── exception/
                    │
                    ├── repository/
                    │
                    ├── service/
                    │
                    └── util/
```

---

# 6. Package Responsibilities

## `config`

Application-level configuration.

Examples:

```text id="3z8qyi"
Database configuration
CORS configuration
Jackson configuration
OpenAPI configuration
Application properties
```

---

## `controller`

REST API entry points.

Controllers should:

* Receive HTTP requests
* Validate request DTOs
* Call services
* Return response DTOs

Controllers must not contain financial business logic.

Example:

```text id="6ue2uk"
StatementController
TransactionController
AccountController
CardController
DashboardController
ReconciliationController
```

---

## `dto`

Data Transfer Objects used between frontend and backend.

Example:

```text id="7c9y2e"
StatementUploadRequest
StatementResponse
TransactionResponse
TransactionFilterRequest
CardResponse
DashboardResponse
ReconciliationReviewResponse
```

Entities must not be exposed directly through REST APIs.

---

## `entity`

JPA persistence entities.

```text id="sgtvw4"
User
Account
Card
Statement
Transaction
Merchant
Category
TransactionLink
ReconciliationReview
```

Entities represent database persistence models.

They should not contain controller or presentation logic.

---

## `enums`

Application and domain enumerations.

Examples:

```text id="z2p2ps"
StatementType
ImportStatus
TransactionDirection
TransactionType
ReconciliationStatus
TransactionLinkType
TransactionLinkStatus
ReviewType
ReviewStatus
```

---

## `exception`

Centralized exception handling.

Examples:

```text id="g6tz84"
GlobalExceptionHandler
StatementProcessingException
InvalidCsvException
TransactionProcessingException
ResourceNotFoundException
DuplicateStatementException
InvalidReconciliationException
```

---

## `repository`

Spring Data JPA repositories.

Examples:

```text id="jv8yaj"
UserRepository
AccountRepository
CardRepository
StatementRepository
TransactionRepository
MerchantRepository
CategoryRepository
TransactionLinkRepository
ReconciliationReviewRepository
```

Repositories should handle data access only.

---

## `service`

Core application and business logic.

This is the most important backend layer.

---

# 7. Service Architecture

```text id="f5rqky"
service/
│
├── StatementService
│
├── CsvImportService
│
├── TransactionService
│
├── TransactionNormalizationService
│
├── TransactionClassificationService
│
├── DuplicateDetectionService
│
├── ReconciliationService
│
├── CashbackService
│
├── RefundService
│
├── MerchantService
│
├── CategoryService
│
├── AccountService
│
├── CardService
│
└── DashboardService
```

---

# 8. Statement Service

Responsible for statement lifecycle.

Responsibilities:

* Create statement record
* Validate statement source
* Track import status
* Start CSV processing
* Update processing status
* Store statement metadata
* Prevent duplicate file imports

Example:

```text id="7l7hcv"
StatementService
    │
    ├── createStatement()
    ├── validateStatement()
    ├── processStatement()
    ├── updateStatus()
    └── getStatement()
```

It should coordinate processing rather than implement CSV parsing itself.

---

# 9. CSV Import Service

Responsible exclusively for reading CSV files.

Responsibilities:

* Open CSV
* Read headers
* Parse rows
* Detect invalid rows
* Extract source values
* Return normalized raw records

It should not decide whether something is:

```text id="4u2k9b"
EXPENSE
TRANSFER
CREDIT_CARD_PAYMENT
```

That belongs to classification/reconciliation services.

---

# 10. CSV Processing Pipeline

```text id="ndnygx"
CSV File
   │
   ▼
CsvImportService
   │
   ▼
Raw CSV Records
   │
   ▼
TransactionNormalizationService
   │
   ▼
Normalized Transaction Data
   │
   ▼
TransactionClassificationService
   │
   ▼
Initial Classification
   │
   ▼
DuplicateDetectionService
   │
   ▼
ReconciliationService
   │
   ▼
Persistence
```

---

# 11. Transaction Normalization Service

Different banks format statements differently.

This service converts source-specific representations into the Finapse transaction model.

Responsibilities:

* Normalize dates
* Normalize amounts
* Normalize descriptions
* Determine debit/credit direction
* Normalize whitespace
* Normalize encoding
* Generate transaction fingerprint
* Preserve original source information

Example:

```text id="p8p24q"
"  SWIGGY INDIA PVT LTD  "
            ↓
"SWIGGY INDIA PVT LTD"
```

---

# 12. Transaction Classification Service

This service determines the initial financial type.

Possible classifications:

```text id="w2e4ka"
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

Classification may use:

* Description patterns
* Account/card source
* Debit/credit direction
* Merchant
* Known keywords
* Transaction context

However:

> Classification must not make high-impact assumptions when evidence is insufficient.

---

# 13. Duplicate Detection Service

Responsible for identifying potential duplicate transactions.

Possible inputs:

```text id="6c1pcr"
Date
Amount
Direction
Description
Account/Card
Transaction Hash
```

The service should produce:

```text id="0pj2g5"
Potential Duplicate
```

rather than deleting anything.

The resulting relationship should be handled through `TransactionLink`.

---

# 14. Reconciliation Service

This is the core Finapse business service.

Responsibilities:

* Identify related transactions
* Detect possible credit-card payments
* Detect transfers
* Detect refunds
* Detect cashback relationships
* Generate confidence scores
* Create transaction links
* Create reconciliation reviews
* Apply user-confirmed decisions

Example:

```text id="d4w4c7"
Bank Transaction
₹5,000
"SBI CREDIT CARD PAYMENT"

        +

Card Statement
₹5,000

        ↓

ReconciliationService

        ↓

Potential CREDIT_CARD_PAYMENT

        ↓

TransactionLink
status = REVIEW_REQUIRED

        ↓

ReconciliationReview
status = PENDING
```

---

# 15. Reconciliation Rule

The service must follow this principle:

```text id="5m0vvi"
High confidence + deterministic evidence
                ↓
        Safe classification

Ambiguous evidence
                ↓
        REVIEW_REQUIRED

User confirmation
                ↓
        CONFIRMED
```

The system must prefer uncertainty over incorrect financial classification.

---

# 16. Transaction Relationship Model

Transactions must never be merged or deleted during reconciliation.

Instead:

```text id="42xv8n"
Transaction A
      │
      ▼
TransactionLink
      │
      ▼
Transaction B
```

This allows relationships such as:

```text id="fbbz9f"
CREDIT_CARD_PAYMENT
TRANSFER
REFUND
DUPLICATE
CASHBACK
```

---

# 17. Financial Calculation Layer

Financial calculations belong in services, not controllers.

Example:

```text id="8njj8v"
DashboardService
       │
       ▼
TransactionRepository
       │
       ▼
Financial classification
       │
       ▼
Metrics
```

Examples:

```text id="i8x6p3"
Income
Actual Spending
Refunds
Cashback
Net Cash Flow
Category Spending
Card Spending
```

---

# 18. Actual Spending Calculation

Conceptually:

```text id="b2xgrb"
Actual Spending
=
EXPENSE
-
REFUND
```

The following must not be counted as new spending:

```text id="lrbx8x"
CREDIT_CARD_PAYMENT
TRANSFER
```

Example:

```text id="t5j8d7"
Amazon Purchase        ₹2,000
Credit Card Payment    ₹2,000

Actual Spending        ₹2,000
```

---

# 19. Dashboard Service

Responsible for aggregating financial information.

Responsibilities:

* Income calculation
* Actual spending calculation
* Cash-flow calculation
* Cashback calculation
* Category summaries
* Merchant summaries
* Card summaries
* Reconciliation alerts

The dashboard should consume domain/service data rather than directly manipulating JPA entities.

---

# 20. Repository Layer

Repositories provide database access.

Example:

```java
public interface TransactionRepository
        extends JpaRepository<Transaction, UUID> {
}
```

Custom query methods should be added only when required by actual application use cases.

Avoid creating unnecessary repository methods in advance.

---

# 21. Controller Design

Controllers should remain thin.

Example:

```text id="5ivc7p"
HTTP Request
     ↓
Controller
     ↓
DTO Validation
     ↓
Service
     ↓
DTO Response
     ↓
HTTP Response
```

Controllers must not:

* Parse CSV
* Calculate spending
* Detect duplicates
* Reconcile transactions
* Directly manipulate repositories for complex workflows

---

# 22. DTO Architecture

Use separate DTOs for requests and responses.

Example:

```text id="8w6k1s"
StatementUploadRequest
StatementResponse

TransactionResponse
TransactionFilterRequest

CardCreateRequest
CardResponse

ReconciliationReviewResponse
ReconciliationDecisionRequest

DashboardResponse
```

DTOs should expose only information required by the frontend.

---

# 23. Validation

Request validation should occur at the API boundary.

Example:

```text id="r0d8xy"
StatementUploadRequest
        ↓
@NotNull
@Valid
        ↓
Controller
```

Business validation belongs in the service layer.

Example:

```text id="9r8xal"
Controller:
"Is a file provided?"

Service:
"Does this account exist?"
"Is this statement type compatible with the account?"
"Has this file already been imported?"
```

---

# 24. Exception Handling

Use centralized exception handling.

```text id="h0gq5v"
Exception
    ↓
GlobalExceptionHandler
    ↓
Standard Error Response
```

Example response:

```json
{
  "timestamp": "2026-08-10T20:00:00",
  "status": 400,
  "code": "INVALID_CSV",
  "message": "The uploaded CSV does not contain a recognizable transaction structure."
}
```

Do not expose:

* Stack traces
* SQL queries
* Internal class names
* Sensitive financial data

to the frontend.

---

# 25. API Response Convention

Responses should use consistent structures.

Success:

```json
{
  "data": {},
  "message": "Success"
}
```

Errors:

```json
{
  "timestamp": "...",
  "status": 400,
  "code": "ERROR_CODE",
  "message": "Human-readable message"
}
```

The exact API contract will be defined in:

```text id="y4v3an"
docs/06-API.md
```

---

# 26. Transaction Processing Boundaries

CSV processing should be designed so that a failed import does not leave the database in an inconsistent state.

Conceptually:

```text id="p7j6e5"
Start Import
     ↓
Create Statement
     ↓
Parse CSV
     ↓
Normalize
     ↓
Validate
     ↓
Persist Transactions
     ↓
Reconcile
     ↓
Update Statement
     ↓
Commit
```

Transactional boundaries should be carefully selected.

Large files should not necessarily be processed as one enormous database transaction.

The implementation should prioritize data integrity while remaining practical for normal personal-finance CSV sizes.

---

# 27. Security Architecture

Although the MVP does not require user authentication, the backend must still:

* Validate all inputs
* Validate uploaded files
* Restrict accepted file types
* Limit upload size
* Avoid arbitrary filesystem access
* Avoid exposing database credentials
* Avoid logging sensitive financial data
* Validate IDs before database operations

---

# 28. File Handling

Uploaded CSV files should not automatically become permanent application files.

The backend should:

1. Receive the file.
2. Validate it.
3. Calculate its hash.
4. Parse it.
5. Process its transactions.
6. Store required metadata.
7. Remove temporary files when no longer needed.

Raw statements should not remain in temporary storage unnecessarily.

---

# 29. Configuration

Application configuration should use environment variables or external configuration.

Sensitive values must not be hardcoded.

Example:

```text id="h4pm94"
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
```

Example configuration:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Do not commit credentials into Git.

---

# 30. Logging

Logging should be useful but privacy-conscious.

Allowed:

```text id="zq67c3"
Statement processing started
Statement processing completed
Transactions processed: 420
Potential matches found: 7
```

Avoid:

```text id="2pyw9m"
Full card number
Full bank account number
Complete transaction descriptions
Raw CSV contents
```

---

# 31. Testing Architecture

Testing must prioritize financial correctness.

## Unit Tests

Test:

```text id="5vhpks"
CSV parsing
Date normalization
Amount normalization
Classification
Duplicate detection
Reconciliation rules
Financial calculations
```

## Integration Tests

Test:

```text id="2j3k8y"
Spring Boot
+
MySQL/Test Database
+
Repositories
+
Services
```

## Controller Tests

Test:

```text id="j6cxp4"
HTTP request
→
Validation
→
Response
```

---

# 32. Reconciliation Test Scenarios

The following scenarios are mandatory.

### Scenario 1 — Normal Expense

```text id="qpl5cs"
Swiggy
₹500
DEBIT
```

Expected:

```text id="i3d7cu"
EXPENSE
```

---

### Scenario 2 — Income

```text id="f6r5xz"
Salary
₹26,399
CREDIT
```

Expected:

```text id="z0m8td"
INCOME
```

---

### Scenario 3 — Credit Card Payment

```text id="edm1zj"
Bank
₹2,000
"SBI CREDIT CARD PAYMENT"
```

Expected:

```text id="a3p8s1"
Potential CREDIT_CARD_PAYMENT
```

If uncertain:

```text id="3m8f1v"
REVIEW_REQUIRED
```

---

### Scenario 4 — Duplicate

Two highly similar transactions.

Expected:

```text id="a9iy1s"
Potential DUPLICATE
+
REVIEW_REQUIRED
```

---

### Scenario 5 — Refund

```text id="8z9fsl"
Purchase ₹2,000
Refund   ₹2,000
```

Expected:

```text id="h5b2h0"
REFUND relationship
```

---

### Scenario 6 — Cashback

```text id="f4tw7a"
Purchase ₹2,000
Cashback ₹100
```

Expected:

```text id="c9py3s"
CASHBACK relationship
```

---

# 33. Backend Dependency Direction

Dependencies should flow inward:

```text id="f6pk5c"
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

The following should be avoided:

```text id="44m2ar"
Repository
    ↓
Controller
```

or:

```text id="i5plg8"
Entity
    ↓
Controller logic
```

Business logic belongs in services/domain components.

---

# 34. Avoid Overengineering

The MVP must not introduce infrastructure without a demonstrated requirement.

Do not add:

```text id="qu5w7q"
Microservices
Kafka
Redis
Kubernetes
RabbitMQ
GraphQL
Event Sourcing
CQRS
Elasticsearch
Cloud storage
```

unless a future requirement explicitly justifies them.

---

# 35. Backend Development Sequence

Implementation should proceed in this order:

```text id="8p8wcr"
1. Spring Boot project
        ↓
2. MySQL configuration
        ↓
3. Enums
        ↓
4. JPA entities
        ↓
5. Repositories
        ↓
6. Exception handling
        ↓
7. DTOs
        ↓
8. Account/Card management
        ↓
9. Statement upload
        ↓
10. CSV parser
        ↓
11. Transaction normalization
        ↓
12. Transaction classification
        ↓
13. Duplicate detection
        ↓
14. Reconciliation engine
        ↓
15. Review workflow
        ↓
16. Dashboard calculations
        ↓
17. API integration
```

---

# 36. Backend Feature Boundaries

Features should be independently developable.

Recommended branches:

```text id="q91jbe"
feature/backend-foundation
feature/account-management
feature/card-management
feature/csv-upload
feature/transaction-processing
feature/duplicate-detection
feature/reconciliation
feature/dashboard-api
```

The exact Git workflow is defined in:

```text id="e8f4lq"
docs/10-GIT-WORKFLOW.md
```

---

# 37. Backend Definition of Done

A backend feature is complete only when:

* Business logic is implemented in the correct service layer.
* API contracts are documented.
* Request/response DTOs are implemented.
* Validation exists.
* Errors are handled consistently.
* Database interactions are tested.
* Relevant unit tests exist.
* No sensitive information is unnecessarily logged.
* Existing requirements are not violated.
* Related documentation is updated.

---

# 38. Core Architectural Principle

The most important backend rule is:

> **Controllers coordinate. Services decide. Repositories persist.**

More specifically:

```text id="z0k7s4"
Controller
    = HTTP / API concerns

DTO
    = Data exchange

Service
    = Business decisions

Entity
    = Persistence model

Repository
    = Database access

Reconciliation Service
    = Financial relationship intelligence
```

The backend must preserve this separation throughout the MVP.
