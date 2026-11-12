-- liquibase formatted sql

-- changeset oncebox-test:ordered-10
ALTER TABLE mig_ordered ADD COLUMN note VARCHAR(255);
