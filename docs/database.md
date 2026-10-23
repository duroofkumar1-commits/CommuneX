# Database and migrations

PostgreSQL is the primary relational database. Neon works through a JDBC URL with SSL enabled.

## Existing Neon safety procedure

This project has already been used with Hibernate `ddl-auto=update`. Do not assume the live schema is exactly the same as the checked-in migration scripts.

1. Copy `backend/.env.example` to `backend/.env` and configure current rotated credentials.
2. Run `./run-local.ps1 -InspectDatabase` from the backend folder in PowerShell.
3. Save the table/column/key output privately.
4. Create a Neon backup/branch before any schema-changing step.
5. Compare metadata with JPA entities and migrations V1–V4.
6. Choose an explicit baseline/adoption plan. Only then set `FLYWAY_ENABLED=true`; after parity is verified, plan to change `DDL_AUTO` from `update` to `validate`.

Never drop or recreate the existing Neon schema to resolve a migration failure.

## Migration inventory
- `V1__community_module.sql`: core user and community tables for a fresh schema.
- `V2__communex_platform_modules.sql`: volunteers, help requests, notifications, facilities, resources, events, messages, documents, scores, activity, groups, polls, blocks and bookmarks.
- `V3__request_workflow_and_community_features.sql`: priority/status history, comment replies, group discussions, announcements and preferences.
- `V4__account_security.sql`: email verification state and hashed account action tokens.

`IF NOT EXISTS` makes some additions idempotent, but it does not prove existing columns, types, indexes or constraints are correct. Metadata comparison and a backup remain required.
