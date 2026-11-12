-- liquibase formatted sql

-- changeset oncebox-test:growing-1 endDelimiter:/
BEGIN
EXECUTE IMMEDIATE 'CREATE TABLE mig_growing (id NUMBER(19) PRIMARY KEY, name VARCHAR2(255) NOT NULL)';
END;
/

BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_mig_growing_name ON mig_growing(name)';
END;
/
