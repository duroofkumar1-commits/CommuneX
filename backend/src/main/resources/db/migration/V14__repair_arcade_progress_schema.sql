-- Forward-only repair for the legacy V7/V8 game_progress schema.
-- Preserve existing data while aligning the schema with the current GameProgress entity.
ALTER TABLE game_progress ADD COLUMN IF NOT EXISTS game VARCHAR(40);
UPDATE game_progress SET game = COALESCE(NULLIF(game, ''), NULLIF(game_id, ''), 'last-zone')
WHERE game IS NULL OR game = '';
ALTER TABLE game_progress ALTER COLUMN game SET DEFAULT 'last-zone';
ALTER TABLE game_progress ALTER COLUMN game SET NOT NULL;

-- V7's named unique constraint may own an index with the same name.
ALTER TABLE game_progress DROP CONSTRAINT IF EXISTS uq_game_progress_user_game;
DROP INDEX IF EXISTS uq_game_progress_user_game;
CREATE UNIQUE INDEX uq_game_progress_user_game ON game_progress(user_id, game);

-- Current JPA entity does not write game_id; retain legacy values but make it optional.
ALTER TABLE game_progress ALTER COLUMN game_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_game_progress_user_updated
    ON game_progress(user_id, updated_at DESC);
