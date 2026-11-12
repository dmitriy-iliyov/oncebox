package io.github.dmitriyiliyov.oncebox.starter.schema;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// The classpath and bean-displacement cases run only here: they do not depend on the database
class PostgreSqlOnceboxSchemaAutoConfigurationIntegrationTests {

    private final OnceboxSchemaAutoConfigurationVerifier verifier =
            new OnceboxSchemaAutoConfigurationVerifier(SchemaDialect.POSTGRESQL);

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
    @DisplayName("IT context() when Flyway and Liquibase are on the classpath should register their suppliers")
    void context_whenFlywayAndLiquibaseAreOnClasspath_shouldRegisterTheirSuppliers() {
        verifier.context_whenFlywayAndLiquibaseAreOnClasspath_shouldRegisterTheirSuppliers();
    }

    @Test
    @DisplayName("IT context() when Flyway is not on the classpath should not register the Flyway supplier")
    void context_whenFlywayIsNotOnClasspath_shouldNotRegisterFlywaySupplier() {
        verifier.context_whenFlywayIsNotOnClasspath_shouldNotRegisterFlywaySupplier();
    }

    @Test
    @DisplayName("IT context() when Liquibase is not on the classpath should not register the Liquibase supplier")
    void context_whenLiquibaseIsNotOnClasspath_shouldNotRegisterLiquibaseSupplier() {
        verifier.context_whenLiquibaseIsNotOnClasspath_shouldNotRegisterLiquibaseSupplier();
    }

    @Test
    @DisplayName("IT context() when mode is FLYWAY and Flyway is not on the classpath should fail naming the mode")
    void context_whenModeIsFlywayAndFlywayIsNotOnClasspath_shouldFailNamingTheMode() {
        verifier.context_whenModeIsFlywayAndFlywayIsNotOnClasspath_shouldFailNamingTheMode();
    }

    @Test
    @DisplayName("IT context() when user defines onceboxSchemaMigrator should use it instead of the default")
    void context_whenUserDefinesOnceboxSchemaMigrator_shouldUseItInsteadOfDefault() {
        verifier.context_whenUserDefinesOnceboxSchemaMigrator_shouldUseItInsteadOfDefault();
    }

    @Test
    @DisplayName("IT context() when a bean uses a repository while initialising should find the tables migrated")
    void context_whenBeanUsesOnceboxRepositoryWhileInitialising_shouldFindTablesAlreadyMigrated() {
        verifier.context_whenBeanUsesOnceboxRepositoryWhileInitialising_shouldFindTablesAlreadyMigrated();
    }
}
