-- liquibase formatted sql

-- changeset oncebox-test:ordered-2 endDelimiter:/
BEGIN
EXECUTE IMMEDIATE 'CREATE TABLE mig_ordered (id NUMBER(19) PRIMARY KEY, name VARCHAR2(255) NOT NULL)';
END;
/

BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_mig_ordered_name ON mig_ordered(name)';
END;
/
