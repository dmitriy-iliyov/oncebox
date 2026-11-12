package io.github.dmitriyiliyov.oncebox.starter.schema;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

// The schema of a shared container seen by table name: what exists, what it holds, and the clean-up between tests
// Not safe for test classes run in parallel: the clean-up drops the library's tables by name in a shared container,
// so one class would drop the tables another is working on.
final class SchemaDatabase {

    static final Set<String> ONCEBOX_TABLES =
            Set.of("outbox_events", "outbox_dlq_events", "inbox_events", "oncebox_jobs");
    static final String UNRELATED_TABLE = "schema_it_orders";
    static final String APP_TABLE = "schema_it_app_orders";
    private static final Set<String> TEST_TABLES = Set.of(UNRELATED_TABLE, APP_TABLE, "flyway_schema_history");

    private final SchemaDialect dialect;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    SchemaDatabase(SchemaDialect dialect) {
        this.dialect = Objects.requireNonNull(dialect, "dialect cannot be null");
        this.dataSource = dialect.dataSource();
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    DataSource dataSource() {
        return dataSource;
    }

    JdbcTemplate jdbcTemplate() {
        return jdbcTemplate;
    }

    boolean tableExists(String table) {
        return tables().containsKey(table.toLowerCase(Locale.ROOT));
    }

    // lower-cased name -> name as the database spells it (Flyway quotes its history table, Oracle upper-cases the rest)
    Map<String, String> tables() {
        return jdbcTemplate.execute((ConnectionCallback<Map<String, String>>) connection -> {
            Map<String, String> tables = new TreeMap<>();
            DatabaseMetaData metaData = connection.getMetaData();
            String [] types = {"TABLE"};
            try (ResultSet rs = metaData.getTables(connection.getCatalog(), connection.getSchema(), "%", types)) {
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    tables.put(name.toLowerCase(Locale.ROOT), name);
                }
            }
            return tables;
        });
    }

    // the oncebox tables and every table of a migration tool, each with its row count
    Map<String, Integer> snapshot() {
        Map<String, Integer> snapshot = new TreeMap<>();
        tables().forEach((lower, actual) -> {
            if (isOnceboxOwned(lower)) {
                snapshot.put(lower, rowCount(actual));
            }
        });
        return snapshot;
    }

    int rowCount(String table) {
        String actual = tables().getOrDefault(table.toLowerCase(Locale.ROOT), table);
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + quote(actual), Integer.class);
        return count == null ? 0 : count;
    }

    void runScript(String classpathScript) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                false,
                false,
                StandardCharsets.UTF_8.name(),
                new ClassPathResource(classpathScript)
        );
        populator.setSeparator(dialect.scriptSeparator());
        populator.execute(dataSource);
    }

    void createUnrelatedTableWithRow() {
        jdbcTemplate.execute("CREATE TABLE " + UNRELATED_TABLE + " (id INT PRIMARY KEY, title VARCHAR(100) NOT NULL)");
        jdbcTemplate.update("INSERT INTO " + UNRELATED_TABLE + " (id, title) VALUES (1, 'kept')");
    }

    void dropTestTables() {
        tables().forEach((lower, actual) -> {
            if (isOnceboxOwned(lower) || TEST_TABLES.contains(lower)) {
                jdbcTemplate.execute("DROP TABLE " + quote(actual) + dropSuffix());
            }
        });
    }

    private static boolean isOnceboxOwned(String lower) {
        return ONCEBOX_TABLES.contains(lower)
                || lower.startsWith("oncebox_") && (lower.endsWith("_schema_history")
                || lower.endsWith("_changelog")
                || lower.endsWith("_changelog_lock"));
    }

    private String dropSuffix() {
        return SchemaDialect.ORACLE == dialect ? " CASCADE CONSTRAINTS PURGE" : "";
    }

    private String quote(String actual) {
        String quote = SchemaDialect.MYSQL == dialect ? "`" : "\"";
        return quote + actual + quote;
    }
}
