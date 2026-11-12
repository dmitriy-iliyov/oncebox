package io.github.dmitriyiliyov.oncebox.starter.schema.testkit;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

// The oncebox tables of a shared container and their clean-up.
// Not safe for test classes running in parallel on one container: it drops and empties the oncebox tables by name.
final class TestkitDatabase {

    static final Set<String> ONCEBOX_TABLES = Set.of(
            "outbox_events",
            "outbox_dlq_events",
            "inbox_events",
            "oncebox_jobs"
    );

    private final TestkitDialect dialect;
    private final JdbcTemplate jdbcTemplate;

    TestkitDatabase(TestkitDialect dialect, DataSource dataSource) {
        this.dialect = Objects.requireNonNull(dialect, "dialect cannot be null");
        this.jdbcTemplate = new JdbcTemplate(Objects.requireNonNull(dataSource, "dataSource cannot be null"));
    }

    // the oncebox tables, the history tables of every migrator and the history table of a plain Flyway run
    void dropOnceboxTables() {
        for (String table : tables()) {
            if (isOnceboxOwned(table.toLowerCase(Locale.ROOT))) {
                jdbcTemplate.execute("DROP TABLE " + quote(table) + dropSuffix());
            }
        }
    }

    void emptyOnceboxTables() {
        for (String table : tables()) {
            if (ONCEBOX_TABLES.contains(table.toLowerCase(Locale.ROOT))) {
                jdbcTemplate.execute("DELETE FROM " + quote(table));
            }
        }
    }

    // the names as the database spells them
    private List<String> tables() {
        return jdbcTemplate.execute((ConnectionCallback<List<String>>) connection -> {
            List<String> tables = new ArrayList<>();
            DatabaseMetaData metaData = connection.getMetaData();
            String[] types = {"TABLE"};
            try (ResultSet rs = metaData.getTables(connection.getCatalog(), connection.getSchema(), "%", types)) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            return tables;
        });
    }

    private static boolean isOnceboxOwned(String lower) {
        return ONCEBOX_TABLES.contains(lower)
                || "flyway_schema_history".equals(lower)
                || lower.startsWith("oncebox_") && (lower.endsWith("_schema_history")
                || lower.endsWith("_changelog")
                || lower.endsWith("_changelog_lock"));
    }

    private String dropSuffix() {
        return TestkitDialect.ORACLE == dialect ? " CASCADE CONSTRAINTS PURGE" : "";
    }

    private String quote(String actual) {
        String quote = TestkitDialect.MYSQL == dialect ? "`" : "\"";
        return quote + actual + quote;
    }
}
