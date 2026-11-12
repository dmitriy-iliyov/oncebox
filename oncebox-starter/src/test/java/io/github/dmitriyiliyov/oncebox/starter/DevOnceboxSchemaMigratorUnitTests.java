package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.OnceboxMigrationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// The supplier builds this migrator with DatabaseType#getVendorName, so every case runs with those values.
// What the real databases make of the scripts is covered by the *DevOnceboxSchemaIntegrationTests; here only the
// statement separator is checked, the one branch of the migrator decided without a database.
@ExtendWith(MockitoExtension.class)
class DevOnceboxSchemaMigratorUnitTests {

    @Mock
    private DataSource dataSource;

    @Test
    @DisplayName("UT constructor when dataSource is null should throw NullPointerException")
    void constructor_whenDataSourceIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new DevOnceboxSchemaMigrator(null, DatabaseType.POSTGRESQL.getVendorName()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("dataSource cannot be null");
    }

    @Test
    @DisplayName("UT constructor when vendor is null should throw NullPointerException")
    void constructor_whenVendorIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new DevOnceboxSchemaMigrator(dataSource, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("vendor cannot be null");
    }

    @Test
    @DisplayName("UT constructor when arguments are valid should not touch the data source")
    void constructor_whenArgumentsAreValid_shouldNotTouchDataSource() {
        // when
        new DevOnceboxSchemaMigrator(dataSource, DatabaseType.POSTGRESQL.getVendorName());

        // then
        verifyNoInteractions(dataSource);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(DatabaseType.class)
    @DisplayName("UT migrate() when the connection fails should throw OnceboxMigrationException for feature and vendor")
    void migrate_whenConnectionFails_shouldThrowOnceboxMigrationException(DatabaseType databaseType)
            throws SQLException {
        // given
        String vendor = databaseType.getVendorName();
        SQLException failure = new SQLException("connection refused");
        when(dataSource.getConnection()).thenThrow(failure);
        DevOnceboxSchemaMigrator tested = new DevOnceboxSchemaMigrator(dataSource, vendor);

        // when / then
        assertThatThrownBy(() -> tested.migrate(OnceboxFeature.OUTBOX.getValue()))
                .isExactlyInstanceOf(OnceboxMigrationException.class)
                .hasRootCause(failure)
                .satisfies(e -> {
                    OnceboxMigrationException exception = (OnceboxMigrationException) e;
                    assertThat(exception.getFeature()).isEqualTo(OnceboxFeature.OUTBOX.getValue());
                    assertThat(exception.getVendor()).isEqualTo(vendor);
                });
    }

    @Test
    @DisplayName("UT migrate() when vendor is oracle should execute every PL/SQL block whole, split by '/'")
    void migrate_whenVendorIsOracle_shouldExecuteWholePlSqlBlocks() {
        // given
        RecordingDataSource recording = new RecordingDataSource();
        DevOnceboxSchemaMigrator tested = new DevOnceboxSchemaMigrator(
                recording.dataSource(),
                DatabaseType.ORACLE.getVendorName()
        );

        // when
        tested.migrate(OnceboxFeature.OUTBOX.getValue());

        // then
        assertThat(recording.executed())
                .isNotEmpty()
                .allSatisfy(sql -> assertThat(sql.strip()).startsWith("BEGIN").endsWith("END;"))
                .anySatisfy(sql -> assertThat(sql).contains("CREATE TABLE outbox_events"));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = DatabaseType.class, names = {"POSTGRESQL", "MYSQL"})
    @DisplayName("UT migrate() when vendor is not oracle should split the scripts into statements by ';'")
    void migrate_whenVendorIsNotOracle_shouldSplitStatementsBySemicolon(DatabaseType databaseType) {
        // given
        RecordingDataSource recording = new RecordingDataSource();
        DevOnceboxSchemaMigrator tested = new DevOnceboxSchemaMigrator(
                recording.dataSource(),
                databaseType.getVendorName()
        );

        // when
        tested.migrate(OnceboxFeature.OUTBOX.getValue());

        // then
        assertThat(recording.executed())
                .hasSizeGreaterThan(1)
                .allSatisfy(sql -> assertThat(sql).doesNotContain(";"))
                .anySatisfy(sql -> assertThat(sql).containsIgnoringCase("CREATE TABLE")
                        .contains("outbox_events"));
    }

    /**
     * A JDBC stub recording every statement executed through it. A Mockito Statement does not fit: ScriptUtils reads
     * results until getUpdateCount() answers -1, which a mock never does (it answers 0), so the run never ends.
     */
    private static final class RecordingDataSource {

        private final List<String> executed = new CopyOnWriteArrayList<>();

        List<String> executed() {
            return executed;
        }

        DataSource dataSource() {
            Connection connection = proxy(Connection.class, (proxy, method, args) -> switch (method.getName()) {
                case "createStatement" -> statement();
                default -> defaultValue(method.getReturnType());
            });
            return proxy(DataSource.class, (proxy, method, args) -> switch (method.getName()) {
                case "getConnection" -> connection;
                default -> defaultValue(method.getReturnType());
            });
        }

        private Statement statement() {
            return proxy(Statement.class, (proxy, method, args) -> {
                if ("execute".equals(method.getName()) && args != null && args.length == 1) {
                    executed.add((String) args[0]);
                    return false;
                }
                if ("getUpdateCount".equals(method.getName())) {
                    return -1;
                }
                return defaultValue(method.getReturnType());
            });
        }

        private static Object defaultValue(Class<?> type) {
            if (type == boolean.class) {
                return false;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private static <T> T proxy(Class<T> type, InvocationHandler handler) {
            return (T) Proxy.newProxyInstance(
                    RecordingDataSource.class.getClassLoader(),
                    new Class<?>[] {type},
                    handler
            );
        }
    }
}
