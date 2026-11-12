-- liquibase formatted sql

-- changeset oncebox-test:alpha-1
CREATE TABLE mig_alpha (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE INDEX idx_mig_alpha_name ON mig_alpha(name);
