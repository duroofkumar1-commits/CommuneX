-- Forward-only compatibility migration for databases that already applied
-- an earlier version of migration V7. Do not edit an already-applied V7.
CREATE TABLE IF NOT EXISTS game_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    game VARCHAR(40) NOT NULL DEFAULT 'last-zone',
    current_level INTEGER NOT NULL DEFAULT 1,
    highest_unlocked_level INTEGER NOT NULL DEFAULT 1,
    total_score BIGINT NOT NULL DEFAULT 0,
    victories INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Fill in fields if an older iteration created this table with fewer columns.
ALTER TABLE game_progress ADD COLUMN IF NOT EXISTS game VARCHAR(40) NOT NULL DEFAULT 'last-zone';
ALTER TABLE game_progress ADD COLUMN IF NOT EXISTS current_level INTEGER NOT NULL DEFAULT 1;
ALTER TABLE game_progress ADD COLUMN IF NOT EXISTS highest_unlocked_level INTEGER NOT NULL DEFAULT 1;
ALTER TABLE game_progress ADD COLUMN IF NOT EXISTS total_score BIGINT NOT NULL DEFAULT 0;
ALTER TABLE game_progress ADD COLUMN IF NOT EXISTS victories INTEGER NOT NULL DEFAULT 0;
ALTER TABLE game_progress ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE game_progress ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uq_game_progress_user_game ON game_progress(user_id, game);
CREATE INDEX IF NOT EXISTS idx_game_progress_user ON game_progress(user_id, updated_at DESC);
