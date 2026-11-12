-- liquibase formatted sql

-- changeset oncebox-test:growing-2
ALTER TABLE mig_growing ADD COLUMN note VARCHAR(255);
