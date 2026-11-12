package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.OnceboxMigrationException;
import io.github.dmitriyiliyov.oncebox.core.OnceboxSchemaMigrator;
import io.github.dmitriyiliyov.oncebox.liquibase.LiquibaseOnceboxSchemaMigrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiquibaseOnceboxSchemaMigratorSupplierUnitTests {

    @Mock
    private DataSource dataSource;

    @Mock
    private ResourceLoader resourceLoader;

    @Test
    @DisplayName("UT constructor when dataSource is null should throw NullPointerException")
    void constructor_whenDataSourceIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new LiquibaseOnceboxSchemaMigratorSupplier(null, resourceLoader))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("dataSource cannot be null");
    }

    @Test
    @DisplayName("UT constructor when resourceLoader is null should throw NullPointerException")
    void constructor_whenResourceLoaderIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new LiquibaseOnceboxSchemaMigratorSupplier(dataSource, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("resourceLoader cannot be null");
    }

    @Test
    @DisplayName("UT getMode() when supplier is built should return LIQUIBASE")
    void getMode_whenSupplierIsBuilt_shouldReturnLiquibase() {
        // given
        LiquibaseOnceboxSchemaMigratorSupplier tested = new LiquibaseOnceboxSchemaMigratorSupplier(
                dataSource,
                resourceLoader
        );

        // when
        OnceboxProperties.SchemaProperties.Mode mode = tested.getMode();

        // then
        assertThat(mode).isEqualTo(OnceboxProperties.SchemaProperties.Mode.LIQUIBASE);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DatabaseType.class)
    @DisplayName("UT supply() when vendor is given should return a liquibase migrator and touch nothing")
    void supply_whenVendorIsGiven_shouldReturnLiquibaseMigrator(DatabaseType databaseType) {
        // given
        LiquibaseOnceboxSchemaMigratorSupplier tested = new LiquibaseOnceboxSchemaMigratorSupplier(
                dataSource,
                resourceLoader
        );

        // when
        OnceboxSchemaMigrator migrator = tested.supply(databaseType);

        // then
        assertThat(migrator).isExactlyInstanceOf(LiquibaseOnceboxSchemaMigrator.class);
        verifyNoInteractions(dataSource, resourceLoader);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DatabaseType.class)
    @DisplayName("UT supply() when the supplied migrator runs should migrate over the data source for the vendor")
    void supply_whenSuppliedMigratorRuns_shouldMigrateOverDataSourceForVendor(DatabaseType databaseType)
            throws SQLException {
        // given
        // the data source refuses connections, so the failure shows which data source and vendor the migrator got
        SQLException failure = new SQLException("connection refused");
        when(dataSource.getConnection()).thenThrow(failure);
        // a strict stub: left unused, it fails the test, so it shows the scripts are looked up through this loader
        when(resourceLoader.getClassLoader()).thenReturn(getClass().getClassLoader());
        OnceboxSchemaMigrator migrator = new LiquibaseOnceboxSchemaMigratorSupplier(dataSource, resourceLoader)
                .supply(databaseType);

        // when / then
        assertThatThrownBy(() -> migrator.migrate(OnceboxFeature.OUTBOX.getValue()))
                .isExactlyInstanceOf(OnceboxMigrationException.class)
                .hasRootCause(failure)
                .satisfies(e -> assertThat(((OnceboxMigrationException) e).getVendor())
                        .isEqualTo(databaseType.getVendorName()));
    }
}
