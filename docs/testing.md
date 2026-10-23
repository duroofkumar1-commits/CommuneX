# Testing strategy

## Frontend
- `npm run lint`: ESLint check for JS/JSX and Node config files.
- `npm test`: Vitest + React Testing Library tests after `npm install`.
- `npm run build`: Vite production bundle.
- `npm run test:e2e`: Playwright smoke test; requires a running app and test configuration. Never run destructive test cases against production records.

## Backend
- `mvn test` / `mvn verify`: JUnit 5/Spring Boot test suite.
- DSA test classes cover sorting, searching, graph algorithms, caches, tree/heap and list/queue edge cases.
- Extend integration tests for auth, ownership, role escalation, notifications, request workflow, inventory rollback, migrations against ephemeral PostgreSQL and SMTP recovery.

## Release gate
- Clean frontend install, lint, tests and build on supported Node 22.
- Backend compile and tests on Java 21/Maven 3.9+.
- Migration/schema comparison completed, backup confirmed and Flyway strategy reviewed.
- Unauthorized accounts cannot access other users' documents/messages/request history; ADMIN-only APIs reject non-admin roles.
- Test expired/invalid JWT, duplicate registration, validation, duplicate votes/registrations, negative inventory quantities and terminal request states.
- Maps show no fabricated markers; route-provider outages show an explanation.
- AI is grounded, reports unavailable records and cannot run arbitrary SQL or fabricate emergency facts.
