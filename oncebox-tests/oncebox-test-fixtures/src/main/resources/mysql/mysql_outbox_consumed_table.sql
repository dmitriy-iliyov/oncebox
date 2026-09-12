CREATE TABLE IF NOT EXISTS outbox_consumed_events (
    id BINARY(16) PRIMARY KEY,
    consumed_at DATETIME NOT NULL
);

SET @exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE table_schema = DATABASE()
    AND table_name = 'outbox_consumed_events'
    AND index_name = 'idx_outbox_consumed_by_consumed_at'
);
SET @sql := IF(@exists = 0,
    'CREATE INDEX idx_outbox_consumed_by_consumed_at ON outbox_consumed_events(consumed_at, id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;