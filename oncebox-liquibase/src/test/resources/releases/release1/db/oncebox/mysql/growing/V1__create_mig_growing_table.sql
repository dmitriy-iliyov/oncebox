-- liquibase formatted sql

-- changeset oncebox-test:growing-1
CREATE TABLE mig_growing (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE INDEX idx_mig_growing_name ON mig_growing(name);
