package io.github.dmitriyiliyov.oncebox.tests.integration.schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("IT oncebox schema in an application running its own Flyway")
class FlywayApplicationSchemaIntegrationTests extends BaseSchemaIntegrationTests {

    FlywayApplicationSchemaIntegrationTests() {
        super(ApplicationMigrations.FLYWAY);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(OnceboxSchemaMode.class)
    @DisplayName("IT startup() when database is empty should create the application and oncebox tables")
    void startup_whenDatabaseIsEmpty_shouldCreateApplicationAndOnceboxTables(OnceboxSchemaMode mode) {
        verifier.startup_whenDatabaseIsEmpty_shouldCreateApplicationAndOnceboxTables(mode);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(OnceboxSchemaMode.class)
    @DisplayName("IT restart() should start again, apply nothing new and keep the rows")
    void restart_shouldStartAgainAndKeepRowsAndHistory(OnceboxSchemaMode mode) {
        verifier.restart_shouldStartAgainAndKeepRowsAndHistory(mode);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("modeChanges")
    @DisplayName("IT restart() when 'oncebox.schema.mode' changes should start and keep the rows")
    void restart_whenModeChanges_shouldStartAndKeepRows(OnceboxSchemaMode from, OnceboxSchemaMode to) {
        verifier.restart_whenModeChanges_shouldStartAndKeepRows(from, to);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(OnceboxSchemaMode.class)
    @DisplayName("IT startup() when the application already ran without oncebox should add the oncebox tables and"
            + " leave the application history alone")
    void startup_whenApplicationAlreadyRanWithoutOncebox_shouldAddOnceboxTablesAndKeepApplicationHistory(
            OnceboxSchemaMode mode
    ) {
        verifier.startup_whenApplicationAlreadyRanWithoutOncebox_shouldAddOnceboxTablesAndKeepApplicationHistory(mode);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(OnceboxSchemaMode.class)
    @DisplayName("IT startup() when the tables come from auto-create of 2.0.0 should migrate them and keep the rows")
    void startup_whenTablesComeFromAutoCreateOfRelease200_shouldMigrateAndKeepRows(OnceboxSchemaMode mode) {
        verifier.startup_whenTablesComeFromAutoCreateOfRelease200_shouldMigrateAndKeepRows(mode);
    }
}
