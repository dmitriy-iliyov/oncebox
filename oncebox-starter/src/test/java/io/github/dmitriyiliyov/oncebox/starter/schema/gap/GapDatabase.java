package io.github.dmitriyiliyov.oncebox.starter.schema.gap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

// The schema of a shared container seen by table name, its clean-up between tests, and the shape of a table.
// Not safe for test classes running in parallel on one container: it drops the oncebox tables by name.
final class GapDatabase {

    static final Set<String> ONCEBOX_TABLES = Set.of(
            "outbox_events",
            "outbox_dlq_events",
            "inbox_events",
            "oncebox_jobs"
    );
    private static final String OWN_TABLE_PREFIX = "gap_";

    private final GapDialect dialect;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    GapDatabase(GapDialect dialect) {
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

    int rowCount(String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + quote(actualName(table)), Integer.class);
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

    // every table of the migration features, of a migration tool and of the gap tests
    void dropTestTables() {
        tables().forEach((lower, actual) -> {
            if (isTestOwned(lower)) {
                jdbcTemplate.execute("DROP TABLE " + quote(actual) + dropSuffix());
            }
        });
    }

    // columns, primary key and indexes of a table as the JDBC driver reports them, with the names the database
    // generates itself (Oracle SYS_C...) masked, so two tables created by the same DDL compare equal
    List<String> shape(String table) {
        String actual = actualName(table);
        return jdbcTemplate.execute((ConnectionCallback<List<String>>) connection -> {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            String schema = connection.getSchema();
            List<String> shape = new ArrayList<>();
            shape.addAll(columns(metaData, catalog, schema, actual));
            shape.add(primaryKey(metaData, catalog, schema, actual));
            shape.addAll(indexes(metaData, catalog, schema, actual));
            return shape;
        });
    }

    // lower-cased name -> name as the database spells it
    private Map<String, String> tables() {
        return jdbcTemplate.execute((ConnectionCallback<Map<String, String>>) connection -> {
            Map<String, String> tables = new TreeMap<>();
            DatabaseMetaData metaData = connection.getMetaData();
            String[] types = {"TABLE"};
            try (ResultSet rs = metaData.getTables(connection.getCatalog(), connection.getSchema(), "%", types)) {
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    tables.put(name.toLowerCase(Locale.ROOT), name);
                }
            }
            return tables;
        });
    }

    private String actualName(String table) {
        return tables().getOrDefault(table.toLowerCase(Locale.ROOT), table);
    }

    private static List<String> columns(DatabaseMetaData metaData, String catalog, String schema, String table)
            throws SQLException {
        Map<Integer, String> columns = new TreeMap<>();
        try (ResultSet rs = metaData.getColumns(catalog, schema, table, "%")) {
            while (rs.next()) {
                // Oracle hands COLUMN_DEF out as a LONG stream, readable once
                String columnDefault = rs.getString("COLUMN_DEF");
                columns.put(rs.getInt("ORDINAL_POSITION"), "column %s %s(%s) nullable=%s default=%s".formatted(
                        rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT),
                        rs.getString("TYPE_NAME"),
                        rs.getString("COLUMN_SIZE"),
                        rs.getString("IS_NULLABLE"),
                        columnDefault == null ? null : columnDefault.trim()
                ));
            }
        }
        return new ArrayList<>(columns.values());
    }

    private static String primaryKey(DatabaseMetaData metaData, String catalog, String schema, String table)
            throws SQLException {
        Map<Integer, String> columns = new TreeMap<>();
        try (ResultSet rs = metaData.getPrimaryKeys(catalog, schema, table)) {
            while (rs.next()) {
                columns.put(rs.getInt("KEY_SEQ"), rs.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return "primary key " + columns.values();
    }

    private static Set<String> indexes(DatabaseMetaData metaData, String catalog, String schema, String table)
            throws SQLException {
        Map<String, Map<Integer, String>> columnsByIndex = new TreeMap<>();
        Map<String, String> headerByIndex = new TreeMap<>();
        try (ResultSet rs = metaData.getIndexInfo(catalog, schema, table, false, true)) {
            while (rs.next()) {
                String name = rs.getString("INDEX_NAME");
                if (name == null) {
                    continue;
                }
                String masked = name.toLowerCase(Locale.ROOT).replaceAll("^sys_c\\d+$", "sys_c#");
                headerByIndex.put(masked, "index %s unique=%s filter=%s".formatted(
                        masked,
                        !rs.getBoolean("NON_UNIQUE"),
                        rs.getString("FILTER_CONDITION")
                ));
                columnsByIndex.computeIfAbsent(masked, ignored -> new TreeMap<>())
                        .put(rs.getInt("ORDINAL_POSITION"), String.valueOf(rs.getString("COLUMN_NAME")));
            }
        }
        Set<String> indexes = new TreeSet<>();
        headerByIndex.forEach((name, header) -> indexes.add(header + " " + columnsByIndex.get(name).values()));
        return indexes;
    }

    private static boolean isTestOwned(String lower) {
        return ONCEBOX_TABLES.contains(lower)
                || "flyway_schema_history".equals(lower)
                || lower.startsWith(OWN_TABLE_PREFIX)
                || lower.startsWith("oncebox_") && (lower.endsWith("_schema_history")
                || lower.endsWith("_changelog")
                || lower.endsWith("_changelog_lock"));
    }

    private String dropSuffix() {
        return GapDialect.ORACLE == dialect ? " CASCADE CONSTRAINTS PURGE" : "";
    }

    private String quote(String actual) {
        String quote = GapDialect.MYSQL == dialect ? "`" : "\"";
        return quote + actual + quote;
    }
}
