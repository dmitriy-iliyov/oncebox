-- liquibase formatted sql

-- changeset oncebox-test:ordered-2
CREATE TABLE mig_ordered (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE INDEX idx_mig_ordered_name ON mig_ordered(name);
