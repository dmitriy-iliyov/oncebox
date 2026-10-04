package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.starter.inbox.InboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.outbox.OutboxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A factory for creating a {@link DatabasePopulator} that initializes the required oncebox tables.
 * <p>
 * It detects the database type and selects the appropriate SQL scripts based on the configuration.
 */
public final class OnceboxDatabasePopulatorFactory {

    private static final Logger log = LoggerFactory.getLogger(OnceboxDatabasePopulatorFactory.class);
    private static final Map<DatabaseType, Map<TableSupplierType, Supplier<Resource>>> OUTBOX_TABLE_SUPPLIERS = Map.of(
            DatabaseType.POSTGRESQL, Map.of(
                    TableSupplierType.OUTBOX, new PostgreSqlOutboxTableSqlResourceSupplier(),
                    TableSupplierType.ONCEBOX_JOBS, new PostgreSqlOnceboxJobsTableSqlResourceSupplier(),
                    TableSupplierType.OUTBOX_DLQ, new PostgreSqlOutboxDlqTableSqlResourceSupplier(),
                    TableSupplierType.INBOX, new PostgreSqlInboxTableSqlResourceSupplier()
            ),
            DatabaseType.MYSQL, Map.of(
                    TableSupplierType.OUTBOX, new MySqlOutboxTableSqlResourceSupplier(),
                    TableSupplierType.ONCEBOX_JOBS, new MySqlOnceboxJobsTableSqlResourceSupplier(),
                    TableSupplierType.OUTBOX_DLQ, new MySqlOutboxDlqTableSqlResourceSupplier(),
                    TableSupplierType.INBOX, new MySqlInboxTableSqlResourceSupplier()
            ),
            DatabaseType.ORACLE, Map.of(
                    TableSupplierType.OUTBOX, new OracleOutboxTableSqlResourceSupplier(),
                    TableSupplierType.ONCEBOX_JOBS, new OracleOnceboxJobsTableSqlResourceSupplier(),
                    TableSupplierType.OUTBOX_DLQ, new OracleOutboxDlqTableSqlResourceSupplier(),
                    TableSupplierType.INBOX, new OracleInboxTableSqlResourceSupplier()
            )
    );

    private OnceboxDatabasePopulatorFactory() {}

    /**
     * Generates a {@link DatabasePopulator} based on the provided properties and data source.
     *
     * @param properties             the oncebox configuration properties.
     * @param dataSource             the data source to connect to the database.
     * @return                       a configured {@link DatabasePopulator} with the necessary SQL scripts.
     * @throws NullPointerException  if some of the passed parameters is null.
     * @throws RuntimeException      if a database connection cannot be established.
     */
    public static DatabasePopulator create(OnceboxProperties properties, DataSource dataSource) {
        Objects.requireNonNull(properties, "properties cannot be null");
        Objects.requireNonNull(dataSource, "dataSource cannot be null");

        List<Resource> scripts;
        DatabaseType databaseType;
        try {
            String dbProductName = JdbcUtils.extractDatabaseMetaData(
                    dataSource, DatabaseMetaData::getDatabaseProductName
            );
            databaseType = DatabaseType.fromString(dbProductName);
            scripts = resolveScripts(properties, databaseType);
        } catch (MetaDataAccessException e) {
            log.error("Error when getting access to database metadata, ", e);
            throw new RuntimeException(e);
        }
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                false,
                false,
                StandardCharsets.UTF_8.name(),
                scripts.toArray(new Resource[0])
        );
        if (DatabaseType.ORACLE.equals(databaseType)) {
            populator.setSeparator("/");
        }
        return populator;
    }

    /**
     * Selects the SQL scripts to run, so that an application only gets the tables it actually uses:
     * an application that uses only the inbox must not end up with an empty {@code outbox_events} table, and one
     * that uses only the outbox must not end up with an empty {@code inbox_events} table.
     * <p>
     * Table ownership:
     * <ul>
     *   <li>{@code outbox_events}/{@code outbox_dlq_events} - written by the outbox</li>
     *   <li>{@code inbox_events} - written by the inbox</li>
     *   <li>{@code oncebox_jobs} - holds the distributed locks taken by the clean-up schedulers only</li>
     * </ul>
     *
     * @param properties    the oncebox configuration properties.
     * @param databaseType  the detected database type.
     * @return              the scripts required by the enabled features, may be empty.
     */
    static List<Resource> resolveScripts(OnceboxProperties properties, DatabaseType databaseType) {
        Map<TableSupplierType, Supplier<Resource>> suppliers = OUTBOX_TABLE_SUPPLIERS.get(databaseType);
        List<Resource> scripts = new ArrayList<>();

        OutboxProperties outbox = properties.getOutbox();
        boolean isOutboxEnabled = outbox != null && Boolean.TRUE.equals(outbox.isEnabled());
        if (isOutboxEnabled) {
            scripts.add(suppliers.get(TableSupplierType.OUTBOX).get());
            if (outbox.getDlq() != null && Boolean.TRUE.equals(outbox.getDlq().isEnabled())) {
                scripts.add(suppliers.get(TableSupplierType.OUTBOX_DLQ).get());
            }
        }

        InboxProperties inbox = properties.getInbox();
        boolean isInboxEnabled = inbox != null && Boolean.TRUE.equals(inbox.isEnabled());
        if (isInboxEnabled) {
            scripts.add(suppliers.get(TableSupplierType.INBOX).get());
        }

        if (isAnyCleanUpEnabled(outbox, isOutboxEnabled, inbox, isInboxEnabled)) {
            scripts.add(suppliers.get(TableSupplierType.ONCEBOX_JOBS).get());
        }
        return scripts;
    }

    private static boolean isAnyCleanUpEnabled(OutboxProperties outbox, boolean isOutboxEnabled,
                                               InboxProperties inbox, boolean isInboxEnabled) {
        if (isOutboxEnabled && outbox.getCleanUp() != null
                && Boolean.TRUE.equals(outbox.getCleanUp().isEnabled())) {
            return true;
        }
        return isInboxEnabled && inbox.getCleanUp() != null
                && Boolean.TRUE.equals(inbox.getCleanUp().isEnabled());
    }

    private static final class PostgreSqlOutboxTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("psql/psql_outbox_table.sql");
        }
    }

    private static final class PostgreSqlOnceboxJobsTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("psql/psql_oncebox_jobs_table.sql");
        }
    }

    private static final class PostgreSqlOutboxDlqTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("psql/psql_outbox_dlq_table.sql");
        }
    }

    private static final class PostgreSqlInboxTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("psql/psql_inbox_table.sql");
        }
    }

    private static final class MySqlOutboxTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("mysql/mysql_outbox_table.sql");
        }
    }

    private static final class MySqlOnceboxJobsTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("mysql/mysql_oncebox_jobs_table.sql");
        }
    }

    private static final class MySqlOutboxDlqTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("mysql/mysql_outbox_dlq_table.sql");
        }
    }

    private static final class MySqlInboxTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("mysql/mysql_inbox_table.sql");
        }
    }

    private static final class OracleOutboxTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("oracle/oracle_outbox_table.sql");
        }
    }

    private static final class OracleOnceboxJobsTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("oracle/oracle_oncebox_jobs_table.sql");
        }
    }

    private static final class OracleOutboxDlqTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("oracle/oracle_outbox_dlq_table.sql");
        }
    }

    private static final class OracleInboxTableSqlResourceSupplier implements Supplier<Resource> {

        @Override
        public ClassPathResource get() {
            return new ClassPathResource("oracle/oracle_inbox_table.sql");
        }
    }
}
