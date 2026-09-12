CREATE TABLE IF NOT EXISTS outbox_dlq_events (
    id UUID PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    dlq_status VARCHAR(50) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    retry_count INTEGER NOT NULL,
    next_retry_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    moved_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_dlq_by_moved_at
    ON outbox_dlq_events(moved_at)
    WHERE dlq_status = 'TO_RETRY';