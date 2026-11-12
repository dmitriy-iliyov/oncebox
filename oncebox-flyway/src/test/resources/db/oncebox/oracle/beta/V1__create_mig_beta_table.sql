-- liquibase formatted sql

-- changeset oncebox-test:beta-1 endDelimiter:/
BEGIN
EXECUTE IMMEDIATE 'CREATE TABLE mig_beta (id NUMBER(19) PRIMARY KEY, name VARCHAR2(255) NOT NULL)';
END;
/

BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_mig_beta_name ON mig_beta(name)';
END;
/
