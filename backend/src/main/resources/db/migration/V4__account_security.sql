-- Existing users are treated as verified to avoid locking out accounts created before this migration.
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS account_action_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    purpose VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_account_token_purpose CHECK (purpose IN ('EMAIL_VERIFICATION','PASSWORD_RESET'))
);
CREATE INDEX IF NOT EXISTS idx_account_token_user_purpose ON account_action_tokens(user_id, purpose, expires_at);
