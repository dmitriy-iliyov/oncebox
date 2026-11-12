INSERT INTO outbox_events (id, status, event_type, payload_type, payload, retry_count)
VALUES ('dddddddd-0000-0000-0000-000000000001', 'PENDING', 'gap-sql-init', 'io.example.OrderCreated', '{}', 0);
