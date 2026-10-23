-- "My requests" filters by requester and sorts by newest first.
CREATE INDEX IF NOT EXISTS idx_help_requests_created_by_created_at
    ON help_requests (created_by, created_at DESC);

-- "Open requests" only ever reads rows whose status is OPEN, so a small partial index is enough.
CREATE INDEX IF NOT EXISTS idx_help_requests_open_created_at
    ON help_requests (created_at DESC)
    WHERE status = 'OPEN';
