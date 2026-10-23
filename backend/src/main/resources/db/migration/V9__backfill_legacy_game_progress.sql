-- V9: Safely migrate legacy arcade progress columns to the current schema.
-- V8 remains unchanged because it has already been applied to some databases.

DO $migration$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'game_progress' AND column_name = 'game_id'
    ) THEN
        EXECUTE 'UPDATE game_progress SET game = game_id WHERE game_id IS NOT NULL';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'game_progress' AND column_name = 'unlocked_level'
    ) THEN
        EXECUTE 'UPDATE game_progress SET highest_unlocked_level = GREATEST(highest_unlocked_level, unlocked_level)';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'game_progress' AND column_name = 'total_xp'
    ) THEN
        EXECUTE 'UPDATE game_progress SET total_score = GREATEST(total_score, total_xp)';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'game_progress' AND column_name = 'best_score'
    ) THEN
        EXECUTE 'UPDATE game_progress SET total_score = GREATEST(total_score, best_score)';
    END IF;
END
$migration$;

-- Ensure the current schema has all the columns expected by the JPA entity.
ALTER TABLE game_progress ADD COLUMN IF NOT EXISTS game VARCHAR(40);
ALTER TABLE game_progress ADD COLUMN IF NOT EXISTS current_level INTEGER NOT NULL DEFAULT 1;
ALTER TABLE game_progress ADD COLUMN IF NOT EXISTS highest_unlocked_level INTEGER NOT NULL DEFAULT 1;
ALTER TABLE game_progress ADD COLUMN IF NOT EXISTS total_score BIGINT NOT NULL DEFAULT 0;
ALTER TABLE game_progress ADD COLUMN IF NOT EXISTS victories INTEGER NOT NULL DEFAULT 0;
ALTER TABLE game_progress ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE game_progress ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE game_progress SET game = 'last-zone' WHERE game IS NULL OR game = '';
ALTER TABLE game_progress ALTER COLUMN game SET DEFAULT 'last-zone';
ALTER TABLE game_progress ALTER COLUMN game SET NOT NULL;

-- V7's constraint with this name may refer to (user_id, game_id). Rename only
-- that legacy constraint so the desired unique index can use the expected name.
DO $rename$
DECLARE
    constraint_columns TEXT[];
BEGIN
    SELECT array_agg(a.attname ORDER BY key_column.ordinality)
    INTO constraint_columns
    FROM pg_constraint c
    JOIN LATERAL unnest(c.conkey) WITH ORDINALITY AS key_column(attnum, ordinality) ON TRUE
    JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = key_column.attnum
    WHERE c.conrelid = 'game_progress'::regclass
      AND c.conname = 'uq_game_progress_user_game'
      AND c.contype = 'u';

    IF constraint_columns = ARRAY['user_id', 'game_id']::TEXT[] THEN
        ALTER TABLE game_progress
            RENAME CONSTRAINT uq_game_progress_user_game TO uq_game_progress_user_game_legacy_v7;
    END IF;
END
$rename$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_game_progress_user_game ON game_progress(user_id, game);
CREATE INDEX IF NOT EXISTS idx_game_progress_user ON game_progress(user_id, updated_at DESC);
