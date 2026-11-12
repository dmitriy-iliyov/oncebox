package io.github.dmitriyiliyov.oncebox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DatabaseTypeUnitTests {

    private static final String INVALID_INPUT_MESSAGE = "DatabaseType is null, empty or blank";

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Test
    @DisplayName("UT fromString(), should throws when value is null")
    public void fromString_whenValueIsNull_shouldThrows() {
        // given
        String value = null;

        // when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> DatabaseType.fromString(value));

        // then
        assertEquals(e.getMessage(), INVALID_INPUT_MESSAGE);
    }

    @Test
    @DisplayName("UT fromString(), should throws when value is empty")
    public void fromString_whenValueIsEmpty_shouldThrows() {
        // given
        String value = "";

        // when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> DatabaseType.fromString(value));

        // then
        assertEquals(e.getMessage(), INVALID_INPUT_MESSAGE);
    }

    @Test
    @DisplayName("UT fromString(), should throws when value is blank")
    public void fromString_whenValueIsBlank_shouldThrows() {
        // given
        String value = "    ";

        // when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> DatabaseType.fromString(value));

        // then
        assertEquals(e.getMessage(), INVALID_INPUT_MESSAGE);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "postgresql, POSTGRESQL",
            "PostgreSQL, POSTGRESQL",
            "POSTGRESQL, POSTGRESQL",
            "mysql, MYSQL",
            "MySQL, MYSQL",
            "oracle, ORACLE",
            "Oracle, ORACLE"
    })
    @DisplayName("UT fromString() when name differs only in case should resolve the database type")
    void fromString_whenNameDiffersOnlyInCase_shouldResolveDatabaseType(String value, DatabaseType expected) {
        // when
        DatabaseType resolved = DatabaseType.fromString(value);

        // then
        assertThat(resolved).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"h2", "MariaDB", "Microsoft SQL Server", " postgresql"})
    @DisplayName("UT fromString() when database is unsupported should throw naming the value")
    void fromString_whenDatabaseUnsupported_shouldThrowNamingValue(String value) {
        // when / then
        assertThatThrownBy(() -> DatabaseType.fromString(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported database '%s'".formatted(value));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "PostgreSQL, POSTGRESQL",
            "MySQL, MYSQL",
            "Oracle, ORACLE"
    })
    @DisplayName("UT fromDataSource() when driver reports a supported product name should resolve the database type")
    void fromDataSource_whenProductNameIsSupported_shouldResolveDatabaseType(String productName, DatabaseType expected)
            throws SQLException {
        // given
        givenProductName(productName);

        // when
        DatabaseType resolved = DatabaseType.fromDataSource(dataSource);

        // then
        assertThat(resolved).isEqualTo(expected);
    }

    @Test
    @DisplayName("UT fromDataSource() when database is unsupported should throw IllegalArgumentException")
    void fromDataSource_whenDatabaseUnsupported_shouldThrowIllegalArgumentException() throws SQLException {
        // given
        givenProductName("H2");

        // when / then
        assertThatThrownBy(() -> DatabaseType.fromDataSource(dataSource))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported database 'H2'");
    }

    @Test
    @DisplayName("UT fromDataSource() when metadata cannot be read should throw with the metadata failure as cause")
    void fromDataSource_whenMetaDataCannotBeRead_shouldThrowWithMetaDataFailureAsCause() throws SQLException {
        // given
        SQLException failure = new SQLException("connection refused");
        when(dataSource.getConnection()).thenThrow(failure);

        // when / then
        // the javadoc names no type for this failure, so only that it is not swallowed is the contract
        assertThatThrownBy(() -> DatabaseType.fromDataSource(dataSource))
                .isInstanceOf(RuntimeException.class)
                .hasRootCause(failure);
    }

    private void givenProductName(String productName) throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn(productName);
    }
}
