CREATE TABLE IF NOT EXISTS game_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    game_id VARCHAR(40) NOT NULL,
    current_level INTEGER NOT NULL DEFAULT 1 CHECK (current_level BETWEEN 1 AND 50),
    unlocked_level INTEGER NOT NULL DEFAULT 1 CHECK (unlocked_level BETWEEN 1 AND 50),
    completed_levels TEXT NOT NULL DEFAULT '',
    best_score INTEGER NOT NULL DEFAULT 0 CHECK (best_score >= 0),
    total_xp INTEGER NOT NULL DEFAULT 0 CHECK (total_xp >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_game_progress_user_game UNIQUE (user_id, game_id)
);

CREATE INDEX IF NOT EXISTS idx_game_progress_user_updated
    ON game_progress(user_id, updated_at DESC);
