-- liquibase formatted sql

-- changeset oncebox-test:growing-2 endDelimiter:/
BEGIN
EXECUTE IMMEDIATE 'ALTER TABLE mig_growing ADD note VARCHAR2(255)';
END;
/
