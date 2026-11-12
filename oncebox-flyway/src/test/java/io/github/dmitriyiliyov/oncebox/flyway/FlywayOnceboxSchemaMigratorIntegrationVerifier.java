package io.github.dmitriyiliyov.oncebox.flyway;

import io.github.dmitriyiliyov.oncebox.core.OnceboxMigrationException;
import org.flywaydb.core.api.FlywayException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * The scenarios of {@link FlywayOnceboxSchemaMigrator} on its fixture scripts under
 * {@code src/test/resources/db/oncebox/<vendor>}, run by one thin class per dialect.
 */
public class FlywayOnceboxSchemaMigratorIntegrationVerifier {

    private final TestDatabase database;
    private final FlywayOnceboxSchemaMigrator tested;

    public FlywayOnceboxSchemaMigratorIntegrationVerifier(TestDatabase database) {
        this.database = database;
        this.tested = new FlywayOnceboxSchemaMigrator(database.dataSource(), database.vendor());
    }

    public void cleanUp() {
        database.dropTestTables();
    }

    public void migrate_whenSchemaIsEmpty_shouldCreateTablesOfFeature() {
        // when
        tested.migrate("alpha");

        // then
        assertThat(database.tableNames()).contains("mig_alpha", "oncebox_alpha_schema_history");
        assertThat(database.columnNames("mig_alpha")).containsExactlyInAnyOrder("id", "name");
        assertThat(appliedScripts("alpha")).containsExactly("1:V1__create_mig_alpha_table.sql:success");
    }

    public void migrate_whenSchemaIsUpToDate_shouldChangeNothing() {
        // given
        tested.migrate("alpha");
        database.jdbc().update("INSERT INTO mig_alpha (id, name) VALUES (1, 'kept')");
        List<Map<String, Object>> historyBefore = history("alpha");

        // when
        assertThatCode(() -> tested.migrate("alpha")).doesNotThrowAnyException();

        // then
        assertThat(history("alpha")).isEqualTo(historyBefore);
        assertThat(database.jdbc().queryForList("SELECT name FROM mig_alpha", String.class)).containsExactly("kept");
    }

    public void migrate_whenTwoFeaturesAreMigrated_shouldKeepSeparateHistoryPerFeature() {
        // when
        tested.migrate("alpha");
        tested.migrate("beta");

        // then
        assertThat(database.tableNames()).contains(
                "mig_alpha",
                "mig_beta",
                "oncebox_alpha_schema_history",
                "oncebox_beta_schema_history"
        );
        assertThat(appliedScripts("alpha")).containsExactly("1:V1__create_mig_alpha_table.sql:success");
        assertThat(appliedScripts("beta")).containsExactly("1:V1__create_mig_beta_table.sql:success");
    }

    public void migrate_whenSchemaHoldsApplicationTables_shouldApplyEveryScriptOfFeature() {
        // given
        database.jdbc().execute("CREATE TABLE mig_app_orders (id INT PRIMARY KEY)");
        database.jdbc().update("INSERT INTO mig_app_orders (id) VALUES (7)");

        // when
        tested.migrate("alpha");

        // then
        assertThat(database.tableNames()).contains("mig_alpha", "oncebox_alpha_schema_history");
        assertThat(appliedScripts("alpha")).containsExactly("1:V1__create_mig_alpha_table.sql:success");
        assertThat(database.jdbc().queryForList("SELECT id FROM mig_app_orders", Integer.class)).containsExactly(7);
    }

    public void migrate_whenVersionsSortDifferentlyAsText_shouldApplyThemInNumericOrder() {
        // when
        tested.migrate("ordered");

        // then
        assertThat(database.columnNames("mig_ordered")).contains("note");
        assertThat(appliedScripts("ordered")).containsExactly(
                "2:V2__create_mig_ordered_table.sql:success",
                "10:V10__add_mig_ordered_note_column.sql:success"
        );
    }

    public void migrate_whenLaterReleaseAddsVersion_shouldApplyItOnTopOfExistingHistory() {
        // given
        migrateFromRelease("release1", "growing");
        database.jdbc().update("INSERT INTO mig_growing (id, name) VALUES (1, 'kept')");

        // when
        migrateFromRelease("release2", "growing");

        // then
        assertThat(database.columnNames("mig_growing")).contains("note");
        assertThat(appliedScripts("growing")).containsExactly(
                "1:V1__create_mig_growing_table.sql:success",
                "2:V2__add_mig_growing_note_column.sql:success"
        );
        assertThat(database.jdbc().queryForList("SELECT name FROM mig_growing", String.class)).containsExactly("kept");
    }

    public void migrate_whenScriptFails_shouldThrowOnceboxMigrationExceptionForFeatureAndVendor() {
        // when / then
        assertThatThrownBy(() -> tested.migrate("broken"))
                .isExactlyInstanceOf(OnceboxMigrationException.class)
                .hasCauseInstanceOf(FlywayException.class)
                .satisfies(e -> {
                    OnceboxMigrationException exception = (OnceboxMigrationException) e;
                    assertThat(exception.getFeature()).isEqualTo("broken");
                    assertThat(exception.getVendor()).isEqualTo(database.vendor());
                });
    }

    // Flyway reads its locations through the context class loader it finds when configured.
    private void migrateFromRelease(String release, String feature) {
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        try (URLClassLoader releaseClassLoader = TestDatabase.releaseClassLoader(release)) {
            thread.setContextClassLoader(releaseClassLoader);
            tested.migrate(feature);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    private List<String> appliedScripts(String feature) {
        return database.jdbc().query(
                "SELECT %s, %s, %s FROM %s WHERE %s = 'SQL' ORDER BY %s".formatted(
                        database.quote("version"),
                        database.quote("script"),
                        database.quote("success"),
                        database.quote(historyTable(feature)),
                        database.quote("type"),
                        database.quote("installed_rank")
                ),
                (rs, rowNum) -> rs.getString(1) + ":" + rs.getString(2) + (rs.getBoolean(3) ? ":success" : ":failed")
        );
    }

    private List<Map<String, Object>> history(String feature) {
        return database.jdbc().queryForList("SELECT * FROM %s ORDER BY %s".formatted(
                database.quote(historyTable(feature)),
                database.quote("installed_rank")
        ));
    }

    private static String historyTable(String feature) {
        return "oncebox_" + feature + "_schema_history";
    }
}
