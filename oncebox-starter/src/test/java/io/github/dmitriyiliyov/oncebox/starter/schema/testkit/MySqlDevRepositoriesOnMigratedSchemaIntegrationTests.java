package io.github.dmitriyiliyov.oncebox.starter.schema.testkit;

import org.junit.jupiter.api.DisplayName;

@DisplayName("IT oncebox repositories on a MySQL schema migrated by DEV")
class MySqlDevRepositoriesOnMigratedSchemaIntegrationTests
        extends AbstractRepositoriesOnMigratedSchemaIntegrationTests {

    @Override
    TestkitDialect dialect() {
        return TestkitDialect.MYSQL;
    }

    @Override
    TestkitMigrator migrator() {
        return TestkitMigrator.DEV;
    }
}
