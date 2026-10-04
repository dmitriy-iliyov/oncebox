CREATE TABLE IF NOT EXISTS inbox_events(
    id UUID PRIMARY KEY,
    consumed_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_inbox_by_consumed_at ON inbox_events(consumed_at);