package io.github.dmitriyiliyov.oncebox.liquibase;

import io.github.dmitriyiliyov.oncebox.core.OnceboxMigrationException;
import liquibase.exception.LiquibaseException;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * The scenarios of {@link LiquibaseOnceboxSchemaMigrator} on its fixture scripts under
 * {@code src/test/resources/db/oncebox/<vendor>}, run by one thin class per dialect.
 */
public class LiquibaseOnceboxSchemaMigratorIntegrationVerifier {

    private final TestDatabase database;
    private final LiquibaseOnceboxSchemaMigrator tested;

    public LiquibaseOnceboxSchemaMigratorIntegrationVerifier(TestDatabase database) {
        this.database = database;
        this.tested = migrator(new DefaultResourceLoader());
    }

    public void cleanUp() {
        database.dropTestTables();
    }

    public void migrate_whenSchemaIsEmpty_shouldCreateTablesOfFeature() {
        // when
        tested.migrate("alpha");

        // then
        assertThat(database.tableNames()).contains(
                "mig_alpha",
                "oncebox_alpha_changelog",
                "oncebox_alpha_changelog_lock"
        );
        assertThat(database.columnNames("mig_alpha")).containsExactlyInAnyOrder("id", "name");
        assertThat(appliedChangeSets("alpha")).containsExactly("alpha-1:EXECUTED");
    }

    public void migrate_whenSchemaIsUpToDate_shouldChangeNothing() {
        // given
        tested.migrate("alpha");
        database.jdbc().update("INSERT INTO mig_alpha (id, name) VALUES (1, 'kept')");
        List<Map<String, Object>> changelogBefore = changelog("alpha");

        // when
        assertThatCode(() -> tested.migrate("alpha")).doesNotThrowAnyException();

        // then
        assertThat(changelog("alpha")).isEqualTo(changelogBefore);
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
                "oncebox_alpha_changelog",
                "oncebox_alpha_changelog_lock",
                "oncebox_beta_changelog",
                "oncebox_beta_changelog_lock"
        );
        assertThat(appliedChangeSets("alpha")).containsExactly("alpha-1:EXECUTED");
        assertThat(appliedChangeSets("beta")).containsExactly("beta-1:EXECUTED");
    }

    public void migrate_whenSchemaHoldsApplicationTables_shouldApplyEveryScriptOfFeature() {
        // given
        database.jdbc().execute("CREATE TABLE mig_app_orders (id INT PRIMARY KEY)");
        database.jdbc().update("INSERT INTO mig_app_orders (id) VALUES (7)");

        // when
        tested.migrate("alpha");

        // then
        assertThat(database.tableNames()).contains("mig_alpha", "oncebox_alpha_changelog");
        assertThat(appliedChangeSets("alpha")).containsExactly("alpha-1:EXECUTED");
        assertThat(database.jdbc().queryForList("SELECT id FROM mig_app_orders", Integer.class)).containsExactly(7);
    }

    // V10 alters the table V2 creates, so applying V10 first fails the migration.
    public void migrate_whenVersionsSortDifferentlyAsText_shouldApplyThemInNumericOrder() {
        // when
        tested.migrate("ordered");

        // then
        assertThat(database.columnNames("mig_ordered")).contains("note");
        assertThat(appliedChangeSets("ordered")).containsExactly("ordered-2:EXECUTED", "ordered-10:EXECUTED");
    }

    public void migrate_whenLaterReleaseAddsVersion_shouldApplyItOnTopOfExistingHistory() {
        // given
        migrateFromRelease("release1", "growing");
        database.jdbc().update("INSERT INTO mig_growing (id, name) VALUES (1, 'kept')");

        // when
        migrateFromRelease("release2", "growing");

        // then
        assertThat(database.columnNames("mig_growing")).contains("note");
        assertThat(appliedChangeSets("growing")).containsExactly("growing-1:EXECUTED", "growing-2:EXECUTED");
        assertThat(database.jdbc().queryForList("SELECT name FROM mig_growing", String.class)).containsExactly("kept");
    }

    public void migrate_whenScriptFails_shouldThrowOnceboxMigrationExceptionForFeatureAndVendor() {
        // when / then
        assertThatThrownBy(() -> tested.migrate("broken"))
                .isExactlyInstanceOf(OnceboxMigrationException.class)
                .hasCauseInstanceOf(LiquibaseException.class)
                .satisfies(e -> {
                    OnceboxMigrationException exception = (OnceboxMigrationException) e;
                    assertThat(exception.getFeature()).isEqualTo("broken");
                    assertThat(exception.getVendor()).isEqualTo(database.vendor());
                });
    }

    private void migrateFromRelease(String release, String feature) {
        try (URLClassLoader releaseClassLoader = TestDatabase.releaseClassLoader(release)) {
            migrator(new DefaultResourceLoader(releaseClassLoader)).migrate(feature);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private LiquibaseOnceboxSchemaMigrator migrator(ResourceLoader resourceLoader) {
        return new LiquibaseOnceboxSchemaMigrator(database.dataSource(), resourceLoader, database.vendor());
    }

    private List<String> appliedChangeSets(String feature) {
        return database.jdbc().query(
                "SELECT id, exectype FROM %s ORDER BY orderexecuted".formatted(changelogTable(feature)),
                (rs, rowNum) -> rs.getString(1) + ":" + rs.getString(2)
        );
    }

    private List<Map<String, Object>> changelog(String feature) {
        return database.jdbc()
                .queryForList("SELECT * FROM %s ORDER BY orderexecuted".formatted(changelogTable(feature)));
    }

    private static String changelogTable(String feature) {
        return "oncebox_" + feature + "_changelog";
    }
}
