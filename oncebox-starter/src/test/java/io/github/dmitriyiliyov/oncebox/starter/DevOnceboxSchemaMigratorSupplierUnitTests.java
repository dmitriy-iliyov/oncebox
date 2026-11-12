package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.OnceboxMigrationException;
import io.github.dmitriyiliyov.oncebox.core.OnceboxSchemaMigrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevOnceboxSchemaMigratorSupplierUnitTests {

    @Mock
    private DataSource dataSource;

    @Test
    @DisplayName("UT constructor when dataSource is null should throw NullPointerException")
    void constructor_whenDataSourceIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new DevOnceboxSchemaMigratorSupplier(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("dataSource cannot be null");
    }

    @Test
    @DisplayName("UT getMode() when supplier is built should return DEV")
    void getMode_whenSupplierIsBuilt_shouldReturnDev() {
        // given
        DevOnceboxSchemaMigratorSupplier tested = new DevOnceboxSchemaMigratorSupplier(dataSource);

        // when
        OnceboxProperties.SchemaProperties.Mode mode = tested.getMode();

        // then
        assertThat(mode).isEqualTo(OnceboxProperties.SchemaProperties.Mode.DEV);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DatabaseType.class)
    @DisplayName("UT supply() when vendor is given should return a dev migrator without touching the data source")
    void supply_whenVendorIsGiven_shouldReturnDevMigrator(DatabaseType databaseType) {
        // given
        DevOnceboxSchemaMigratorSupplier tested = new DevOnceboxSchemaMigratorSupplier(dataSource);

        // when
        OnceboxSchemaMigrator migrator = tested.supply(databaseType);

        // then
        assertThat(migrator).isExactlyInstanceOf(DevOnceboxSchemaMigrator.class);
        verifyNoInteractions(dataSource);
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
        OnceboxSchemaMigrator migrator = new DevOnceboxSchemaMigratorSupplier(dataSource).supply(databaseType);

        // when / then
        assertThatThrownBy(() -> migrator.migrate(OnceboxFeature.OUTBOX.getValue()))
                .isExactlyInstanceOf(OnceboxMigrationException.class)
                .hasRootCause(failure)
                .satisfies(e -> assertThat(((OnceboxMigrationException) e).getVendor())
                        .isEqualTo(databaseType.getVendorName()));
    }
}
