# Deployment guide

## Suggested topology
- Frontend: Cloudflare Pages or a static host supporting Vite SPA fallback.
- Backend: a Java 21/Docker-compatible host.
- Database: Neon PostgreSQL or another managed PostgreSQL provider.
- Secrets: hosting environment settings/secret manager, not committed `.env` files.

## Frontend production variable
Set `VITE_API_URL` to the absolute backend origin plus `/api`, e.g. `https://api.example.com/api`. It is compiled into public assets and must never contain secrets. Set backend `CORS_ALLOWED_ORIGINS` to the exact deployed frontend origin.

## Backend variables
Set `DATABASE_URL` (JDBC URL), `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION`, `CORS_ALLOWED_ORIGINS`, and a reviewed `FLYWAY_ENABLED`/`DDL_AUTO` combination. Set `PORT` if the host requires it. SMTP/AI settings are optional but those features remain unavailable until configured.

## Existing Neon
First run `backend\\run-local.ps1 -InspectDatabase`, take a backup/branch, and review columns/constraints. The project includes additive/baseline-aware migrations, but the current schema was also managed using Hibernate `update`; this does not guarantee automatic baseline adoption is safe. Do not drop the production schema.

## Docker Compose
At the repository root:

```powershell
Copy-Item .env.example .env
# Edit .env; replace DATABASE_PASSWORD and JWT_SECRET
docker compose up --build
```

Compose starts its own local PostgreSQL database and does not connect to existing Neon. Open `http://localhost:5173`. Stop using `docker compose down`; don't use `down -v` unless you intentionally want to remove the local volume.

## Free tier caveat
Free-tier status, sleep behavior, quotas, build minutes, database compute, bandwidth and credit-card requirements change. Check provider terms before deployment. No provider is guaranteed permanently free. Public demo map/routing services are not a production SLA.
