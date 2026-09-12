BEGIN
EXECUTE IMMEDIATE '
        CREATE TABLE outbox_consumed_events (
            id RAW(16) PRIMARY KEY,
            consumed_at TIMESTAMP NOT NULL
        )';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_outbox_consumed_by_consumed_at ON outbox_consumed_events(consumed_at)';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/