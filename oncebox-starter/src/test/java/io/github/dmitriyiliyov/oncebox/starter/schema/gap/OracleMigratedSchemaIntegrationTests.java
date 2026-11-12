package io.github.dmitriyiliyov.oncebox.starter.schema.gap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OracleMigratedSchemaIntegrationTests {

    private final MigratedSchemaVerifier verifier = new MigratedSchemaVerifier(GapDialect.ORACLE);

    @BeforeEach
    void setUp() {
        verifier.cleanUp();
    }

    @AfterEach
    void tearDown() {
        verifier.cleanUp();
    }

    @Test
    @DisplayName("IT migrate() when Flyway migrates an empty schema should match the repository test schema")
    void migrate_whenFlywayMigratesEmptySchema_shouldShapeTablesAsRepositoryTestSchema() {
        verifier.migrate_whenSchemaIsEmpty_shouldCreateTablesShapedAsRepositoryTestSchema(GapMigrator.FLYWAY);
    }

    @Test
    @DisplayName("IT migrate() when Liquibase migrates an empty schema should match the repository test schema")
    void migrate_whenLiquibaseMigratesEmptySchema_shouldShapeTablesAsRepositoryTestSchema() {
        verifier.migrate_whenSchemaIsEmpty_shouldCreateTablesShapedAsRepositoryTestSchema(GapMigrator.LIQUIBASE);
    }

    @Test
    @DisplayName("IT migrate() when DEV migrates an empty schema should match the repository test schema")
    void migrate_whenDevMigratesEmptySchema_shouldShapeTablesAsRepositoryTestSchema() {
        verifier.migrate_whenSchemaIsEmpty_shouldCreateTablesShapedAsRepositoryTestSchema(GapMigrator.DEV);
    }
}
