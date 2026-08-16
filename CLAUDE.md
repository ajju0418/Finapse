# Finapse - Project Guide

Finapse is a privacy-first personal finance intelligence and transaction reconciliation platform.

## Build & Run Commands
### Backend (Spring Boot)
- Run: `cd backend && mvn spring-boot:run`
- Test: `cd backend && mvn test`
- Clean: `cd backend && mvn clean`

### Frontend (Next.js)
- Install: `cd frontend && npm install`
- Run: `cd frontend && npm run dev`
- Build: `cd frontend && npm run build`
- Lint: `cd frontend && npm run lint`

## Database Setup
- Schema: `mysql -u root -p < database/schema.sql`
- Seed: `mysql -u root -p finapse < database/seed.sql`

## Coding Standards
### General
- Follow the existing project structure (`backend/`, `frontend/`, `database/`, `doc/`).
- Use descriptive naming for variables and functions.

### Backend (Java/Spring Boot)
- Language: Java 21
- Framework: Spring Boot 3, Spring Data JPA
- Patterns: Repository pattern for data access, Service layer for business logic, Controller layer for REST APIs.
- Naming: PascalCase for classes, camelCase for methods and variables.
- Error Handling: Use `@ControllerAdvice` for global exception handling.

### Frontend (Next.js)
- Language: TypeScript
- Framework: Next.js 14 (App Router), Tailwind CSS, shadcn/ui
- Styling: Use Tailwind utility classes.
- Component Structure: Modular, reusable components in `components/` directory.
- State Management: Use React hooks (`useState`, `useContext`) or external libraries if necessary.

## Key Project Areas
- `backend/src/main/java/com/finapse/service/`: Contains the core "Intelligence" logic (duplicate detection, reconciliation).
- `frontend/app/`: Next.js App Router pages and layouts.
- `database/`: SQL scripts for schema and seed data.
