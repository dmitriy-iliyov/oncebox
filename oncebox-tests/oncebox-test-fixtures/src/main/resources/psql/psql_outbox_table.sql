CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    retry_count INTEGER NOT NULL,
    next_retry_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_by_event_type_and_next_retry_at
    ON outbox_events(event_type, next_retry_at)
    WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_outbox_by_status_and_updated_at
    ON outbox_events(status, updated_at)
    WHERE status IN ('IN_PROCESS', 'FAILED', 'PROCESSED')
