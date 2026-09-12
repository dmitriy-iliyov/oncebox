CREATE TABLE IF NOT EXISTS outbox_jobs(
    job_name VARCHAR(255) PRIMARY KEY,
    lock_until TIMESTAMPTZ NOT NULL,
    locked_by UUID,
    locked_at TIMESTAMPTZ NOT NULL,
    lock_at_least_for BIGINT NOT NULL,
    lock_at_most_for BIGINT NOT NULL
);