CREATE TABLE IF NOT EXISTS inbox_events (
    id BINARY(16) PRIMARY KEY,
    consumed_at DATETIME(6) NOT NULL DEFAULT (UTC_TIMESTAMP(6))
);

SET @exists := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE table_schema = DATABASE()
    AND table_name = 'inbox_events'
    AND index_name = 'idx_inbox_by_consumed_at'
);
SET @sql := IF(@exists = 0,
    'CREATE INDEX idx_inbox_by_consumed_at ON inbox_events(consumed_at, id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;