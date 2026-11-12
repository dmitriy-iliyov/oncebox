-- liquibase formatted sql

-- changeset oncebox-test:ordered-10 endDelimiter:/
BEGIN
EXECUTE IMMEDIATE 'ALTER TABLE mig_ordered ADD note VARCHAR2(255)';
END;
/
