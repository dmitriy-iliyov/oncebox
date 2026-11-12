-- liquibase formatted sql

-- changeset oncebox-test:alpha-1 endDelimiter:/
BEGIN
EXECUTE IMMEDIATE 'CREATE TABLE mig_alpha (id NUMBER(19) PRIMARY KEY, name VARCHAR2(255) NOT NULL)';
END;
/

BEGIN
EXECUTE IMMEDIATE 'CREATE INDEX idx_mig_alpha_name ON mig_alpha(name)';
END;
/
