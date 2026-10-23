# Security checklist

## Implemented in source
- BCrypt password hashing and JWT validation.
- Database-backed principal/role authority.
- Method-level role checks for admin/facility/moderation/help-request actions.
- Bean Validation on major request DTOs.
- DTO response patterns and `@JsonIgnore` for password hash.
- Ownership/state checks for important help-request/document/user operations.
- Transactional request lifecycle and inventory updates with history records.
- CORS allowlist configured through environment variables.
- Database credentials/JWT signing key configured via environment variables.
- Generic server error settings.

## Before public deployment
1. Rotate database/JWT secrets previously pasted in chat or stored in the original archive.
2. Generate a cryptographically random JWT secret and store it in the deployment secret manager.
3. Set `CORS_ALLOWED_ORIGINS` to the exact production frontend origin.
4. Reconcile Flyway with a backup and test migrations against a staging copy.
5. Configure SMTP and test verification/recovery expiry and replay cases.
6. Add edge rate limits to login, recovery, posts and messaging.
7. Test blocked-user behavior across all community/message routes and restrict resource-history access to the intended audience.
8. Test invalid/expired JWTs, role escalation, unauthorized document/message/request access, XSS/input validation and duplicate operations.
9. Use HTTPS. JWT in browser localStorage has XSS exposure; consider a secure httpOnly cookie/refresh-token design after a threat-model review.
10. Keep admin provisioning restricted; never trust a frontend role field.

CommuneX is coordination software, not an emergency dispatch service.
