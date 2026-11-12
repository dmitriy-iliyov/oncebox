package io.github.dmitriyiliyov.oncebox.starter.schema.testkit;

import org.junit.jupiter.api.DisplayName;

@DisplayName("IT oncebox repositories on a PostgreSQL schema migrated by Liquibase")
class PostgreSqlLiquibaseRepositoriesOnMigratedSchemaIntegrationTests
        extends AbstractRepositoriesOnMigratedSchemaIntegrationTests {

    @Override
    TestkitDialect dialect() {
        return TestkitDialect.POSTGRESQL;
    }

    @Override
    TestkitMigrator migrator() {
        return TestkitMigrator.LIQUIBASE;
    }
}
