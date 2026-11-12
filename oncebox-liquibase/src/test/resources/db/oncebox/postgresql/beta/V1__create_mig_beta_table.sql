-- liquibase formatted sql

-- changeset oncebox-test:beta-1
CREATE TABLE mig_beta (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE INDEX idx_mig_beta_name ON mig_beta(name);
