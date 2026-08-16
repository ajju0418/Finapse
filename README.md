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

## Getting Started (New Local Clone Setup)

### Prerequisites

Ensure you have the following installed on your machine:
- **Java 21 JDK**
- **Node.js 18+** & `npm`
- **Maven 3.8+**
- **MySQL 8.0+** running locally on port `3306`

---

### Step 1: Clone the Repository

```bash
git clone <repository-url>
cd Finapse
```

---

### Step 2: Configure MySQL Database

Ensure your local MySQL service is running. 

> 💡 **Automatic Setup**: The backend is configured to **automatically create** the `finapse` database on first run and auto-seed the default user & default categories! You do **not** need to manually run any SQL scripts unless you want to inspect or manual seed.

> ⚠️ **Database Password Configuration**: Make sure to set your local MySQL server password in `backend/src/main/resources/application.properties` (under `spring.datasource.password=your_password`) or pass it as an environment variable before starting the backend!

```bash
# Set your local MySQL credentials via environment variables (or edit application.properties directly)
export DB_USERNAME=root
export DB_PASSWORD=your_local_mysql_password
```

*(Manual schema setup optional)*:
```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS finapse;"
mysql -u root -p finapse < database/schema.sql
mysql -u root -p finapse < database/seed.sql
```

---

### Step 3: Start Backend (Spring Boot)

Navigate to the `backend` directory and start the application:

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

- **Backend API Server**: `http://localhost:8080`
- **Health Check**: `http://localhost:8080/api/health`

---

### Step 4: Start Frontend (Next.js)

Open a new terminal window, navigate to the `frontend` directory, install dependencies, and run the development server:

```bash
cd frontend
npm install
npm run dev
```

- **Frontend Application**: `http://localhost:3000`
- **Main App Dashboard**: `http://localhost:3000/app/money`

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

