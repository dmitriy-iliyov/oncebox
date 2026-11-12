package io.github.dmitriyiliyov.oncebox.starter.schema;

import io.github.dmitriyiliyov.oncebox.liquibase.LiquibaseOnceboxSchemaMigrator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

// the real MySQL scripts migrated by Liquibase
class MySqlLiquibaseOnceboxSchemaIntegrationTests {

    private final OnceboxSchemaIntegrationVerifier verifier =
            new OnceboxSchemaIntegrationVerifier(
                    SchemaDialect.MYSQL,
                    dataSource -> new LiquibaseOnceboxSchemaMigrator(
                            dataSource,
                            new DefaultResourceLoader(),
                            SchemaDialect.MYSQL.vendor()
                    )
            );

    @BeforeEach
    void setUp() {
        verifier.cleanUp();
    }

    @AfterEach
    void tearDown() {
        verifier.cleanUp();
    }

    @Test
    @DisplayName("IT migrate() when schema is empty should create the outbox table the repository works on")
    void migrate_whenSchemaIsEmpty_shouldCreateOutboxTableTheRepositoryWorksOn() {
        verifier.migrate_whenSchemaIsEmpty_shouldCreateOutboxTableTheRepositoryWorksOn();
    }

    @Test
    @DisplayName("IT migrate() when schema is empty should create the DLQ table the repository works on")
    void migrate_whenSchemaIsEmpty_shouldCreateDlqTableTheRepositoryWorksOn() {
        verifier.migrate_whenSchemaIsEmpty_shouldCreateDlqTableTheRepositoryWorksOn();
    }

    @Test
    @DisplayName("IT migrate() when schema is empty should create the inbox table the repository works on")
    void migrate_whenSchemaIsEmpty_shouldCreateInboxTableTheRepositoryWorksOn() {
        verifier.migrate_whenSchemaIsEmpty_shouldCreateInboxTableTheRepositoryWorksOn();
    }

    @Test
    @DisplayName("IT migrate() when schema is empty should create the jobs table the lock repository works on")
    void migrate_whenSchemaIsEmpty_shouldCreateJobsTableTheLockRepositoryWorksOn() {
        verifier.migrate_whenSchemaIsEmpty_shouldCreateJobsTableTheLockRepositoryWorksOn();
    }

    @Test
    @DisplayName("IT migrate() when run twice should change nothing and not fail")
    void migrate_whenRunTwice_shouldChangeNothingAndNotFail() {
        verifier.migrate_whenRunTwice_shouldChangeNothingAndNotFail();
    }

    @Test
    @DisplayName("IT migrate() when tables of the previous release hold data should migrate and keep the rows")
    void migrate_whenTablesOfPreviousReleaseHoldData_shouldMigrateAndKeepTheRows() {
        verifier.migrate_whenTablesOfPreviousReleaseHoldData_shouldMigrateAndKeepTheRows();
    }

    @Test
    @DisplayName("IT migrate() when schema holds unrelated tables should create oncebox tables and leave them alone")
    void migrate_whenSchemaHoldsUnrelatedTables_shouldCreateOnceboxTablesAndLeaveThemAlone() {
        verifier.migrate_whenSchemaHoldsUnrelatedTables_shouldCreateOnceboxTablesAndLeaveThemAlone();
    }
}
