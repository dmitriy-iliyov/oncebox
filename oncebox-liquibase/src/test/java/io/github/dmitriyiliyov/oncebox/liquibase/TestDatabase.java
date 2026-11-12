package io.github.dmitriyiliyov.oncebox.liquibase;

import io.github.dmitriyiliyov.oncebox.tests.fixtures.MySqlTestContainerSingleton;
import io.github.dmitriyiliyov.oncebox.tests.fixtures.OracleTestContainerSingleton;
import io.github.dmitriyiliyov.oncebox.tests.fixtures.PostgresTestContainerSingleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;

import javax.sql.DataSource;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * One of the shared database containers, seen through the names a migration leaves behind. Table and column names
 * are compared in lower case, since Oracle folds unquoted identifiers to upper case. A twin lives in oncebox-flyway,
 * kept on purpose: neither tool module depends on the other, and oncebox-test-fixtures stays free of migration helpers.
 */
public final class TestDatabase {

    private static final Set<String> TEST_TABLE_PREFIXES = Set.of("mig_", "oncebox_");

    private final String vendor;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final String dropTableSuffix;

    private TestDatabase(String vendor, JdbcDatabaseContainer<?> container, String dropTableSuffix) {
        this.vendor = vendor;
        this.dataSource = new DriverManagerDataSource(
                container.getJdbcUrl(),
                container.getUsername(),
                container.getPassword()
        );
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.dropTableSuffix = dropTableSuffix;
    }

    public static TestDatabase postgresql() {
        return new TestDatabase("postgresql", PostgresTestContainerSingleton.INSTANCE, " CASCADE");
    }

    public static TestDatabase mysql() {
        return new TestDatabase("mysql", MySqlTestContainerSingleton.INSTANCE, "");
    }

    public static TestDatabase oracle() {
        return new TestDatabase("oracle", OracleTestContainerSingleton.INSTANCE, " CASCADE CONSTRAINTS PURGE");
    }

    public String vendor() {
        return vendor;
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public JdbcTemplate jdbc() {
        return jdbcTemplate;
    }

    public Set<String> tableNames() {
        return actualTableNames().keySet();
    }

    public Set<String> columnNames(String table) {
        String actualName = Objects.requireNonNull(
                actualTableNames().get(table),
                "table %s does not exist".formatted(table)
        );
        return withMetaData(metaData -> {
            Set<String> columns = new HashSet<>();
            Connection connection = metaData.getConnection();
            try (ResultSet rs = metaData.getColumns(
                    connection.getCatalog(), connection.getSchema(), actualName, "%")) {
                while (rs.next()) {
                    if (actualName.equals(rs.getString("TABLE_NAME"))) {
                        columns.add(rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
                    }
                }
            }
            return columns;
        });
    }

    /**
     * Quotes a lower-case identifier the way the engine expects, for the tables a tool creates under a quoted name.
     */
    public String quote(String identifier) {
        String quote = withMetaData(DatabaseMetaData::getIdentifierQuoteString).trim();
        return quote + identifier + quote;
    }

    /**
     * Drops the fixture tables ({@code mig_*}) and every history and lock table of a feature ({@code oncebox_*}).
     */
    public void dropTestTables() {
        actualTableNames().forEach((lowerCaseName, actualName) -> {
            if (TEST_TABLE_PREFIXES.stream().anyMatch(lowerCaseName::startsWith)) {
                jdbcTemplate.execute("DROP TABLE " + quote(actualName) + dropTableSuffix);
            }
        });
    }

    /**
     * A class loader that sees the scripts of one release from {@code src/test/resources/releases/<release>} on top
     * of the test classpath, as if that release's jar were deployed.
     */
    public static URLClassLoader releaseClassLoader(String release) {
        ClassLoader parent = TestDatabase.class.getClassLoader();
        URL root = Objects.requireNonNull(parent.getResource("releases/" + release), release);
        try {
            URL directory = Path.of(root.toURI()).toUri().toURL();
            return new URLClassLoader(new URL[] {directory}, parent);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, String> actualTableNames() {
        return withMetaData(metaData -> {
            Map<String, String> names = new HashMap<>();
            Connection connection = metaData.getConnection();
            try (ResultSet rs = metaData.getTables(
                    connection.getCatalog(), connection.getSchema(), "%", new String[] {"TABLE"})) {
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    names.put(name.toLowerCase(Locale.ROOT), name);
                }
            }
            return names;
        });
    }

    private <T> T withMetaData(MetaDataCallback<T> callback) {
        try (Connection connection = dataSource.getConnection()) {
            return callback.apply(connection.getMetaData());
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @FunctionalInterface
    private interface MetaDataCallback<T> {
        T apply(DatabaseMetaData metaData) throws SQLException;
    }
}
