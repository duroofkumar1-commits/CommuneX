-- Expand request lifecycle without deleting existing OPEN/ACCEPTED/COMPLETED values.
ALTER TABLE help_requests ADD COLUMN IF NOT EXISTS priority VARCHAR(16) NOT NULL DEFAULT 'NORMAL';
ALTER TABLE help_requests ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_help_requests_priority_created ON help_requests(priority, created_at);

CREATE TABLE IF NOT EXISTS help_request_updates (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL REFERENCES help_requests(id) ON DELETE CASCADE,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    note VARCHAR(500) NOT NULL,
    actor_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_request_updates_request_date ON help_request_updates(request_id, created_at);

ALTER TABLE community_comments ADD COLUMN IF NOT EXISTS parent_comment_id BIGINT REFERENCES community_comments(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_community_comments_parent ON community_comments(parent_comment_id, created_at);

CREATE TABLE IF NOT EXISTS group_discussions (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES community_groups(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id),
    content VARCHAR(5000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_group_discussions_feed ON group_discussions(group_id, created_at DESC);

CREATE TABLE IF NOT EXISTS community_announcements (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(3000) NOT NULL,
    category VARCHAR(60) NOT NULL DEFAULT 'GENERAL',
    published BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_announcements_published ON community_announcements(published, created_at DESC);

CREATE TABLE IF NOT EXISTS user_preferences (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    preferred_language VARCHAR(8) NOT NULL DEFAULT 'en',
    theme VARCHAR(12) NOT NULL DEFAULT 'light',
    show_on_leaderboard BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
