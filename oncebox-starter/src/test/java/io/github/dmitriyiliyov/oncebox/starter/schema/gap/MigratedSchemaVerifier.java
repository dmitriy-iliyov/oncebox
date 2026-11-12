package io.github.dmitriyiliyov.oncebox.starter.schema.gap;

import io.github.dmitriyiliyov.oncebox.core.OnceboxSchemaMigrator;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxFeature;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

// The tables a migrator creates from db/oncebox/<vendor>/<feature> compared with the tables the testkit repository
// verifiers run on in the dialect modules (oncebox-test-fixtures scripts): the same shape means those verifiers hold
// on a migrated schema too. The verifiers themselves run on a migrated schema in starter.schema.testkit.
public class MigratedSchemaVerifier {

    private final GapDialect dialect;
    private final GapDatabase database;

    MigratedSchemaVerifier(GapDialect dialect) {
        this.dialect = Objects.requireNonNull(dialect, "dialect cannot be null");
        this.database = new GapDatabase(dialect);
    }

    public void cleanUp() {
        database.dropTestTables();
    }

    public void migrate_whenSchemaIsEmpty_shouldCreateTablesShapedAsRepositoryTestSchema(GapMigrator migrator) {
        // given
        Map<String, List<String>> expected = repositoryTestSchema();
        OnceboxSchemaMigrator tested = migrator.create(database.dataSource(), dialect.vendor());

        // when
        for (OnceboxFeature feature : OnceboxFeature.values()) {
            tested.migrate(feature.getValue());
        }

        // then
        assertThat(expected).as("the repository test schema was read").allSatisfy((table, shape) ->
                assertThat(shape).as(table).isNotEmpty());
        assertThat(shapes()).isEqualTo(expected);
    }

    private Map<String, List<String>> repositoryTestSchema() {
        dialect.repositoryTestScripts().forEach(database::runScript);
        Map<String, List<String>> shapes = shapes();
        database.dropTestTables();
        return shapes;
    }

    private Map<String, List<String>> shapes() {
        Map<String, List<String>> shapes = new TreeMap<>();
        GapDatabase.ONCEBOX_TABLES.forEach(table -> shapes.put(table, database.shape(table)));
        return shapes;
    }
}
