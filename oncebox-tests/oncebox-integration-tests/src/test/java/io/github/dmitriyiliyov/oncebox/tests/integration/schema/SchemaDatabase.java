package io.github.dmitriyiliyov.oncebox.tests.integration.schema;

import io.github.dmitriyiliyov.oncebox.tests.integration.DatabaseContainer;
import io.github.dmitriyiliyov.oncebox.tests.integration.DatabaseType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

// The schema of the shared container seen by table name: what exists, the history rows, the clean-up between tests.
// The clean-up drops the tables by name, so the schema group runs alone in its JVM, as every cell of this module does.
public final class SchemaDatabase {

    public static final List<String> FEATURES = List.of("outbox", "dlq", "inbox", "jobs");
    public static final Set<String> ONCEBOX_TABLES =
            Set.of("outbox_events", "outbox_dlq_events", "inbox_events", "oncebox_jobs");
    public static final String APP_TABLE = "schema_app_orders";
    private static final Set<String> APP_TOOL_TABLES =
            Set.of("flyway_schema_history", "databasechangelog", "databasechangeloglock");

    private final DatabaseType type;
    private final JdbcTemplate jdbcTemplate;

    public SchemaDatabase(DatabaseContainer container) {
        Objects.requireNonNull(container, "container cannot be null");
        this.type = container.getDatabaseType();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                container.getJdbcUrl(), container.getUsername(), container.getPassword()
        );
        dataSource.setDriverClassName(type.getDriverClassName());
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public boolean tableExists(String table) {
        return tables().containsKey(table.toLowerCase(Locale.ROOT));
    }

    public int rowCount(String table) {
        String actual = tables().getOrDefault(table.toLowerCase(Locale.ROOT), table);
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + quote(actual), Integer.class);
        return count == null ? 0 : count;
    }

    // every history table of the application and of oncebox, with its row count
    public Map<String, Integer> histories() {
        Map<String, Integer> histories = new TreeMap<>();
        tables().forEach((lower, actual) -> {
            if (isHistory(lower)) {
                histories.put(lower, rowCount(actual));
            }
        });
        return histories;
    }

    // the tables oncebox 2.0.0 created through 'tables.auto-create', before schema migrations existed
    public void createRelease200Tables() {
        String directory = switch (type) {
            case POSTGRES_SQL -> "psql";
            case MY_SQL -> "mysql";
            case ORACLE -> "oracle";
        };
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        if (DatabaseType.ORACLE == type) {
            populator.setSeparator("/");
        }
        for (String table : List.of("outbox", "outbox_dlq", "inbox", "oncebox_jobs")) {
            populator.addScript(new ClassPathResource(
                    "schema/release-2.0.0/%s/%s_%s_table.sql".formatted(directory, directory, table)
            ));
        }
        populator.execute(jdbcTemplate.getDataSource());
    }

    public void insertInboxRecord(UUID id) {
        Object value = DatabaseType.POSTGRES_SQL == type ? id : bytes(id);
        jdbcTemplate.update("INSERT INTO inbox_events (id) VALUES (?)", value);
    }

    public void dropTestTables() {
        tables().forEach((lower, actual) -> {
            if (ONCEBOX_TABLES.contains(lower) || APP_TABLE.equals(lower) || isHistory(lower)
                    || lower.endsWith("_changelog_lock")) {
                jdbcTemplate.execute("DROP TABLE " + quote(actual) + dropSuffix());
            }
        });
    }

    private static boolean isHistory(String lower) {
        return APP_TOOL_TABLES.contains(lower)
                || lower.startsWith("oncebox_") && (lower.endsWith("_schema_history") || lower.endsWith("_changelog"));
    }

    // lower-cased name -> name as the database spells it (Flyway quotes its history table, Oracle upper-cases the rest)
    private Map<String, String> tables() {
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

    private static byte[] bytes(UUID id) {
        return ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }

    private String dropSuffix() {
        return DatabaseType.ORACLE == type ? " CASCADE CONSTRAINTS PURGE" : "";
    }

    private String quote(String actual) {
        String quote = DatabaseType.MY_SQL == type ? "`" : "\"";
        return quote + actual + quote;
    }
}
