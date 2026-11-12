package io.github.dmitriyiliyov.oncebox.flyway;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostgreSqlFlywayOnceboxSchemaMigratorIntegrationTests {

    private final FlywayOnceboxSchemaMigratorIntegrationVerifier verifier =
            new FlywayOnceboxSchemaMigratorIntegrationVerifier(TestDatabase.postgresql());

    @BeforeEach
    void setUp() {
        verifier.cleanUp();
    }

    @AfterEach
    void tearDown() {
        verifier.cleanUp();
    }

    @Test
    @DisplayName("IT migrate() when schema is empty should create tables of feature on PostgreSQL")
    void migrate_whenSchemaIsEmpty_shouldCreateTablesOfFeature() {
        verifier.migrate_whenSchemaIsEmpty_shouldCreateTablesOfFeature();
    }

    @Test
    @DisplayName("IT migrate() when schema is up to date should change nothing on PostgreSQL")
    void migrate_whenSchemaIsUpToDate_shouldChangeNothing() {
        verifier.migrate_whenSchemaIsUpToDate_shouldChangeNothing();
    }

    @Test
    @DisplayName("IT migrate() when two features are migrated should keep separate history per feature on PostgreSQL")
    void migrate_whenTwoFeaturesAreMigrated_shouldKeepSeparateHistoryPerFeature() {
        verifier.migrate_whenTwoFeaturesAreMigrated_shouldKeepSeparateHistoryPerFeature();
    }

    @Test
    @DisplayName("IT migrate() when schema holds application tables should apply every script of feature on PostgreSQL")
    void migrate_whenSchemaHoldsApplicationTables_shouldApplyEveryScriptOfFeature() {
        verifier.migrate_whenSchemaHoldsApplicationTables_shouldApplyEveryScriptOfFeature();
    }

    @Test
    @DisplayName("IT migrate() when V2 and V10 sort differently as text should apply them numerically on PostgreSQL")
    void migrate_whenVersionsSortDifferentlyAsText_shouldApplyThemInNumericOrder() {
        verifier.migrate_whenVersionsSortDifferentlyAsText_shouldApplyThemInNumericOrder();
    }

    @Test
    @DisplayName("IT migrate() when later release adds version should apply it on top of the history on PostgreSQL")
    void migrate_whenLaterReleaseAddsVersion_shouldApplyItOnTopOfExistingHistory() {
        verifier.migrate_whenLaterReleaseAddsVersion_shouldApplyItOnTopOfExistingHistory();
    }

    @Test
    @DisplayName("IT migrate() when script fails should throw OnceboxMigrationException on PostgreSQL")
    void migrate_whenScriptFails_shouldThrowOnceboxMigrationExceptionForFeatureAndVendor() {
        verifier.migrate_whenScriptFails_shouldThrowOnceboxMigrationExceptionForFeatureAndVendor();
    }
}
