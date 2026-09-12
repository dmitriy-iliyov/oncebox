BEGIN
EXECUTE IMMEDIATE '
        CREATE TABLE outbox_events (
            id RAW(16) PRIMARY KEY,
            status VARCHAR2(50) NOT NULL,
            event_type VARCHAR2(255) NOT NULL,
            payload_type VARCHAR2(255) NOT NULL,
            payload CLOB NOT NULL,
            retry_count INTEGER NOT NULL,
            next_retry_at TIMESTAMP NOT NULL,
            created_at TIMESTAMP NOT NULL,
            updated_at TIMESTAMP NOT NULL
        )';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_outbox_by_event_type_and_next_retry_at ON outbox_events(event_type, next_retry_at)';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_outbox_by_status_and_updated_at ON outbox_events(status, updated_at)';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/