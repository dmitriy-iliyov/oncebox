CREATE TABLE IF NOT EXISTS outbox_jobs (
    job_name VARCHAR(255) PRIMARY KEY,
    lock_until DATETIME(3) NOT NULL,
    locked_by BINARY(16),
    locked_at DATETIME(3) NOT NULL,
    lock_at_least_for BIGINT NOT NULL,
    lock_at_most_for BIGINT NOT NULL
);