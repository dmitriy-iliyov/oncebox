CREATE TABLE IF NOT EXISTS outbox_events (
    id BINARY(16) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    retry_count INTEGER NOT NULL,
    next_retry_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

SET @exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE table_schema = DATABASE()
    AND table_name = 'outbox_events'
    AND index_name = 'idx_outbox_by_event_type_and_next_retry_at'
    );
SET @sql := IF(@exists = 0,
    'CREATE INDEX idx_outbox_by_event_type_and_next_retry_at ON outbox_events(event_type, next_retry_at, id)',
    'SELECT 1'
    );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE table_schema = DATABASE()
    AND table_name = 'outbox_events'
    AND index_name = 'idx_outbox_by_status_and_updated_at'
    );
SET @sql := IF(@exists = 0,
    'CREATE INDEX idx_outbox_by_status_and_updated_at ON outbox_events(status, updated_at, id)',
    'SELECT 1'
    );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;