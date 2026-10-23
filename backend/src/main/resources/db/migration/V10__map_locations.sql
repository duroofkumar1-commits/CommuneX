-- V10: Map explorer. Exact coordinates for help requests, events and inventory,
-- plus opt-in live locations for volunteers/responders.
-- Existing tables are NOT altered; this only adds two new tables.

CREATE TABLE IF NOT EXISTS map_locations (
    id          BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(30)      NOT NULL,
    entity_id   BIGINT           NOT NULL,
    latitude    DOUBLE PRECISION NOT NULL,
    longitude   DOUBLE PRECISION NOT NULL,
    updated_at  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_map_locations_entity UNIQUE (entity_type, entity_id)
);

CREATE INDEX IF NOT EXISTS idx_map_locations_type ON map_locations (entity_type);

CREATE TABLE IF NOT EXISTS user_live_locations (
    user_id    BIGINT           PRIMARY KEY,
    latitude   DOUBLE PRECISION NOT NULL,
    longitude  DOUBLE PRECISION NOT NULL,
    accuracy   DOUBLE PRECISION,
    sharing    BOOLEAN          NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_live_locations_sharing ON user_live_locations (sharing, updated_at);
