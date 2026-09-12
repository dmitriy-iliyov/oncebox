CREATE TABLE IF NOT EXISTS outbox_consumed_events(
    id UUID PRIMARY KEY,
    consumed_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_consumed_by_consumed_at ON outbox_consumed_events(consumed_at);