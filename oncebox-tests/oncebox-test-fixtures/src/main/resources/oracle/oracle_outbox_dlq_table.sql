BEGIN
EXECUTE IMMEDIATE '
        CREATE TABLE outbox_dlq_events (
            id RAW(16) PRIMARY KEY,
            status VARCHAR2(50) NOT NULL,
            dlq_status VARCHAR2(50) NOT NULL,
            event_type VARCHAR2(255) NOT NULL,
            payload_type VARCHAR2(255) NOT NULL,
            payload CLOB NOT NULL,
            retry_count INTEGER NOT NULL,
            next_retry_at TIMESTAMP WITH TIME ZONE NOT NULL,
            created_at TIMESTAMP WITH TIME ZONE NOT NULL,
            updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
            moved_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL
        )';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_outbox_dlq_by_moved_at ON outbox_dlq_events(moved_at)';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/