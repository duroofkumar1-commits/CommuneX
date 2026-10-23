-- Frequently ordered or counted user-specific data.
CREATE INDEX IF NOT EXISTS idx_notifications_user_created_at
    ON notifications (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON notifications (user_id)
    WHERE read = FALSE;

CREATE INDEX IF NOT EXISTS idx_direct_messages_recipient_unread
    ON direct_messages (recipient_id, read_at)
    WHERE read_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_community_events_status_starts_at
    ON community_events (status, starts_at);

CREATE INDEX IF NOT EXISTS idx_volunteers_available_created_at
    ON volunteers (available, created_at DESC);

-- pg_trgm is a trusted PostgreSQL extension. If the database owner cannot install it,
-- the migration still succeeds; the query-level result limits remain active.
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS pg_trgm;
EXCEPTION
    WHEN insufficient_privilege OR undefined_file THEN
        RAISE NOTICE 'pg_trgm extension unavailable; substring searches will use normal query plans';
END $$;

-- Trigram indexes are installed only when the extension is available to this database.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm') THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_facilities_name_trgm ON facilities USING gin (name gin_trgm_ops)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_facilities_address_trgm ON facilities USING gin (address gin_trgm_ops)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_facilities_description_trgm ON facilities USING gin (description gin_trgm_ops)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_resource_items_name_trgm ON resource_items USING gin (name gin_trgm_ops)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_resource_items_location_trgm ON resource_items USING gin (location gin_trgm_ops)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_resource_items_description_trgm ON resource_items USING gin (description gin_trgm_ops)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_community_events_title_trgm ON community_events USING gin (title gin_trgm_ops)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_community_events_description_trgm ON community_events USING gin (description gin_trgm_ops)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_community_events_location_trgm ON community_events USING gin (location gin_trgm_ops)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_community_posts_content_trgm ON community_posts USING gin (content gin_trgm_ops)';
    END IF;
END $$;
