package io.github.dmitriyiliyov.oncebox.starter.schema.testkit;

import org.junit.jupiter.api.DisplayName;

@DisplayName("IT oncebox repositories on a PostgreSQL schema migrated by Flyway")
class PostgreSqlFlywayRepositoriesOnMigratedSchemaIntegrationTests
        extends AbstractRepositoriesOnMigratedSchemaIntegrationTests {

    @Override
    TestkitDialect dialect() {
        return TestkitDialect.POSTGRESQL;
    }

    @Override
    TestkitMigrator migrator() {
        return TestkitMigrator.FLYWAY;
    }
}
