package io.github.dmitriyiliyov.oncebox.starter.schema.testkit;

import org.junit.jupiter.api.DisplayName;

@DisplayName("IT oncebox repositories on an Oracle schema migrated by DEV")
class OracleDevRepositoriesOnMigratedSchemaIntegrationTests
        extends AbstractRepositoriesOnMigratedSchemaIntegrationTests {

    @Override
    TestkitDialect dialect() {
        return TestkitDialect.ORACLE;
    }

    @Override
    TestkitMigrator migrator() {
        return TestkitMigrator.DEV;
    }
}
