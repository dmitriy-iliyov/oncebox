BEGIN
EXECUTE IMMEDIATE '
        CREATE TABLE outbox_jobs(
            job_name VARCHAR(255) PRIMARY KEY,
            lock_until TIMESTAMP(3) NOT NULL,
            locked_by RAW(16),
            locked_at TIMESTAMP(3) NOT NULL,
            lock_at_least_for NUMBER(19) NOT NULL,
            lock_at_most_for NUMBER(19) NOT NULL
        )';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/