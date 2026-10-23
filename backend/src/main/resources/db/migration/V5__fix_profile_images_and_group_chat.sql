-- Local uploads are stored as compact data URLs. 500 characters is too small for an image.
ALTER TABLE users
    ALTER COLUMN profile_image_url TYPE TEXT;

-- Bring the legacy group_memberships table in line with the current JPA entity.
ALTER TABLE group_memberships
    ADD COLUMN IF NOT EXISTS role VARCHAR(50) NOT NULL DEFAULT 'MEMBER';
ALTER TABLE group_memberships
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'APPROVED';
ALTER TABLE group_memberships
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;

UPDATE group_memberships
SET created_at = COALESCE(created_at, joined_at, CURRENT_TIMESTAMP)
WHERE created_at IS NULL;

ALTER TABLE group_memberships
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE group_memberships
    ALTER COLUMN created_at SET NOT NULL;

CREATE TABLE IF NOT EXISTS group_messages (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES community_groups(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    parent_message_id BIGINT REFERENCES group_messages(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_group_messages_group_created
    ON group_messages(group_id, created_at ASC);

CREATE TABLE IF NOT EXISTS message_reactions (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES group_messages(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    emoji VARCHAR(20) NOT NULL
);

-- Remove duplicate legacy reactions before enforcing one reaction per user/message/emoji.
DELETE FROM message_reactions older
USING message_reactions newer
WHERE older.id < newer.id
  AND older.message_id = newer.message_id
  AND older.user_id = newer.user_id
  AND older.emoji = newer.emoji;

CREATE UNIQUE INDEX IF NOT EXISTS uq_message_reaction_user_emoji
    ON message_reactions(message_id, user_id, emoji);
CREATE INDEX IF NOT EXISTS idx_message_reactions_message
    ON message_reactions(message_id);
