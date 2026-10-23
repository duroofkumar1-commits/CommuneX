# Database setup

CommuneX uses PostgreSQL. The local Docker Compose profile starts a separate local database; it does not point at Neon unless you deliberately configure it to do so.

## Fresh local database

1. Copy the root `.env.example` to `.env`.
2. Replace both placeholder secrets with local-only values.
3. Run `docker compose up --build` from the repository root.

Docker Compose uses a fresh `communex` database and enables Flyway migrations. Do not copy a production database password into the root Compose `.env`.

## Existing Neon database

Before applying any migration to an existing Neon project:

1. Rotate any credentials that have been exposed and configure new values in `backend/.env`.
2. From `backend`, run `./run-local.ps1 -InspectDatabase` in PowerShell. The script disables Flyway and Hibernate schema mutation for that inspection profile; it prints metadata only.
3. Review the table/column/key output and compare it with `backend/src/main/resources/db/migration/V1__community_module.sql` through `V4__account_security.sql`.
4. Create a Neon branch/backup and resolve schema differences before enabling Flyway.
5. Do not drop tables or reset the schema to make a migration pass.

The migration scripts are an initial schema baseline for a fresh database, not proof that every older database is already aligned. Keep `DDL_AUTO=update` only during controlled local development; switch to `validate` after migration parity is explicitly verified.
