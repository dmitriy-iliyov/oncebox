package io.github.dmitriyiliyov.oncebox.starter.schema.gap;

import io.github.dmitriyiliyov.oncebox.starter.DevOnceboxSchemaMigrator;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

// DevOnceboxSchemaMigrator over a fixture feature of three versions, V1, V2 and V10: each script records its version
// in gap_dev_log, so the order of the rows is the order the scripts ran in. The fixture lies under
// schema/gap/dev-classpath, put on the classpath as a root of its own for the migrator's classpath*: lookup.
public class DevVersionedScriptsVerifier {

    private static final String FEATURE = "gap_versions";
    private static final String FIXTURE_ROOT = "schema/gap/dev-classpath/";

    private final GapDatabase database;
    private final DevOnceboxSchemaMigrator tested;

    DevVersionedScriptsVerifier(GapDialect dialect) {
        this.database = new GapDatabase(Objects.requireNonNull(dialect, "dialect cannot be null"));
        this.tested = new DevOnceboxSchemaMigrator(database.dataSource(), dialect.vendor());
    }

    public void cleanUp() {
        database.dropTestTables();
    }

    public void migrate_whenFeatureHasSeveralVersions_shouldApplyAllInNumericOrder() throws Exception {
        // when
        migrateWithFixtureOnClasspath();

        // then
        assertThat(recordedVersions()).containsExactly("V1", "V2", "V10");
    }

    public void migrate_whenRunAgain_shouldChangeNothing() throws Exception {
        // given
        migrateWithFixtureOnClasspath();
        List<Map<String, Object>> afterFirstRun = recordedRows();

        // when
        migrateWithFixtureOnClasspath();

        // then
        assertThat(recordedRows()).isEqualTo(afterFirstRun);
    }

    private void migrateWithFixtureOnClasspath() throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        URL root = Objects.requireNonNull(original.getResource(FIXTURE_ROOT), "fixture root cannot be null");
        try (URLClassLoader withFixture = new URLClassLoader(new URL[] {root}, original)) {
            thread.setContextClassLoader(withFixture);
            tested.migrate(FEATURE);
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    private List<String> recordedVersions() {
        return database.jdbcTemplate().queryForList("SELECT version FROM gap_dev_log ORDER BY id", String.class);
    }

    private List<Map<String, Object>> recordedRows() {
        return database.jdbcTemplate().queryForList("SELECT id, version FROM gap_dev_log ORDER BY id");
    }
}
