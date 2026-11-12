package io.github.dmitriyiliyov.oncebox.starter.schema;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MySqlOnceboxSchemaAutoConfigurationIntegrationTests {

    private final OnceboxSchemaAutoConfigurationVerifier verifier =
            new OnceboxSchemaAutoConfigurationVerifier(SchemaDialect.MYSQL);

    @BeforeEach
    void setUp() {
        verifier.cleanUp();
    }

    @AfterEach
    void tearDown() {
        verifier.cleanUp();
    }

    @Test
    @DisplayName("IT context() when mode is FLYWAY should create the tables through Flyway")
    void context_whenModeIsFlyway_shouldCreateTablesThroughFlyway() {
        verifier.context_whenModeIsFlyway_shouldCreateTablesThroughFlyway();
    }

    @Test
    @DisplayName("IT context() when mode is LIQUIBASE should create the tables through Liquibase")
    void context_whenModeIsLiquibase_shouldCreateTablesThroughLiquibase() {
        verifier.context_whenModeIsLiquibase_shouldCreateTablesThroughLiquibase();
    }

    @Test
    @DisplayName("IT context() when mode is DEV should create the tables without a history table")
    void context_whenModeIsDev_shouldCreateTablesWithoutHistory() {
        verifier.context_whenModeIsDev_shouldCreateTablesWithoutHistory();
    }

    @Test
    @DisplayName("IT context() when mode is NONE should create no tables")
    void context_whenModeIsNone_shouldCreateNoTables() {
        verifier.context_whenModeIsNone_shouldCreateNoTables();
    }

    @Test
    @DisplayName("IT context() when mode is absent should create no tables, NONE being the default")
    void context_whenModeIsAbsent_shouldCreateNoTables() {
        verifier.context_whenModeIsAbsent_shouldCreateNoTables();
    }

    @Test
    @DisplayName("IT context() when outbox is disabled should create no outbox tables")
    void context_whenOutboxIsDisabled_shouldCreateNoOutboxTables() {
        verifier.context_whenOutboxIsDisabled_shouldCreateNoOutboxTables();
    }

    @Test
    @DisplayName("IT context() when a bean uses a repository while initialising should find the tables migrated")
    void context_whenBeanUsesOnceboxRepositoryWhileInitialising_shouldFindTablesAlreadyMigrated() {
        verifier.context_whenBeanUsesOnceboxRepositoryWhileInitialising_shouldFindTablesAlreadyMigrated();
    }
}
