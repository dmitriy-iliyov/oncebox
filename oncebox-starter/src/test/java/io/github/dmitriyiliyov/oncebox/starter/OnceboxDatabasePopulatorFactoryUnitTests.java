package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.starter.inbox.InboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.outbox.OutboxProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.MetaDataAccessException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OnceboxDatabasePopulatorFactoryUnitTests {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Mock
    private OnceboxProperties properties;

    @Mock
    private OutboxProperties outboxProperties;

    @Mock
    private OutboxProperties.DlqProperties dlqProperties;

    @Mock
    private InboxProperties inboxProperties;

    @Mock
    private OnceboxProperties.CleanUpProperties publisherCleanUpProperties;

    @Mock
    private OnceboxProperties.CleanUpProperties consumerCleanUpProperties;

    private void mockDbProductName(String productName) throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn(productName);
    }

    private void mockOutbox(boolean enabled, boolean dlqEnabled, boolean cleanUpEnabled) {
        when(properties.getOutbox()).thenReturn(outboxProperties);
        when(outboxProperties.isEnabled()).thenReturn(enabled);
        when(outboxProperties.getDlq()).thenReturn(dlqProperties);
        when(dlqProperties.isEnabled()).thenReturn(dlqEnabled);
        when(outboxProperties.getCleanUp()).thenReturn(publisherCleanUpProperties);
        when(publisherCleanUpProperties.isEnabled()).thenReturn(cleanUpEnabled);
    }

    private void mockInbox(boolean enabled, boolean cleanUpEnabled) {
        when(properties.getInbox()).thenReturn(inboxProperties);
        when(inboxProperties.isEnabled()).thenReturn(enabled);
        when(inboxProperties.getCleanUp()).thenReturn(consumerCleanUpProperties);
        when(consumerCleanUpProperties.isEnabled()).thenReturn(cleanUpEnabled);
    }

    private static List<String> filenamesOf(List<Resource> scripts) {
        return scripts.stream().map(Resource::getFilename).toList();
    }

    @Test
    @DisplayName("UT create() when properties is null should throw NullPointerException")
    void create_whenPropertiesNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> OnceboxDatabasePopulatorFactory.create(null, dataSource))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("properties cannot be null");
    }

    @Test
    @DisplayName("UT create() when dataSource is null should throw NullPointerException")
    void create_whenDataSourceNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> OnceboxDatabasePopulatorFactory.create(properties, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("dataSource cannot be null");
    }

    @Test
    @DisplayName("UT create() when PostgreSQL and only base tables enabled should return populator")
    void create_whenPostgreSqlAndBaseTables_shouldReturnPopulator() throws SQLException {
        mockDbProductName("PostgreSQL");
        mockOutbox(true, false, true);
        when(properties.getInbox()).thenReturn(null);

        DatabasePopulator result = OnceboxDatabasePopulatorFactory.create(properties, dataSource);

        assertThat(result).isNotNull().isInstanceOf(ResourceDatabasePopulator.class);
        verify(connection).close();
    }

    @Test
    @DisplayName("UT create() when connection fails should throw RuntimeException")
    void create_whenConnectionFails_shouldThrowRuntimeException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        assertThatThrownBy(() -> OnceboxDatabasePopulatorFactory.create(properties, dataSource))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(MetaDataAccessException.class);
    }

    @Test
    @DisplayName("UT create() when metadata extraction fails should throw RuntimeException")
    void create_whenMetadataExtractionFails_shouldThrowRuntimeException() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenThrow(new SQLException("Metadata failed"));

        assertThatThrownBy(() -> OnceboxDatabasePopulatorFactory.create(properties, dataSource))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(MetaDataAccessException.class);
    }

    @Test
    @DisplayName("UT resolveScripts() when outbox only should select outbox and jobs tables")
    void resolveScripts_whenOutboxOnly_shouldSelectOutboxAndJobsTables() {
        mockOutbox(true, false, true);
        mockInbox(false, false);

        List<Resource> scripts = OnceboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.POSTGRESQL);

        assertThat(filenamesOf(scripts))
                .containsExactly("psql_outbox_table.sql", "psql_oncebox_jobs_table.sql");
    }

    @Test
    @DisplayName("UT resolveScripts() when outbox with DLQ should select DLQ table too")
    void resolveScripts_whenOutboxWithDlq_shouldSelectDlqTable() {
        mockOutbox(true, true, true);
        mockInbox(false, false);

        List<Resource> scripts = OnceboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.POSTGRESQL);

        assertThat(filenamesOf(scripts)).containsExactly(
                "psql_outbox_table.sql",
                "psql_outbox_dlq_table.sql",
                "psql_oncebox_jobs_table.sql"
        );
    }

    @Test
    @DisplayName("UT resolveScripts() when outbox disabled should not select outbox and DLQ tables")
    void resolveScripts_whenOutboxDisabled_shouldNotSelectOutboxTable() {
        mockOutbox(false, true, true);
        mockInbox(true, true);

        List<Resource> scripts = OnceboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.POSTGRESQL);

        assertThat(filenamesOf(scripts))
                .containsExactly("psql_inbox_table.sql", "psql_oncebox_jobs_table.sql");
    }

    @Test
    @DisplayName("UT resolveScripts() when inbox disabled should not select inbox table")
    void resolveScripts_whenInboxDisabled_shouldNotSelectInboxTable() {
        mockOutbox(true, false, true);
        mockInbox(false, true);

        List<Resource> scripts = OnceboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.POSTGRESQL);

        assertThat(filenamesOf(scripts)).doesNotContain("psql_inbox_table.sql");
    }

    @Test
    @DisplayName("UT resolveScripts() when no clean-up enabled should not select jobs table")
    void resolveScripts_whenNoCleanUpEnabled_shouldNotSelectJobsTable() {
        mockOutbox(true, false, false);
        mockInbox(true, false);

        List<Resource> scripts = OnceboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.POSTGRESQL);

        assertThat(filenamesOf(scripts))
                .containsExactly("psql_outbox_table.sql", "psql_inbox_table.sql");
    }

    @Test
    @DisplayName("UT resolveScripts() when only inbox clean-up enabled should select jobs table")
    void resolveScripts_whenOnlyInboxCleanUpEnabled_shouldSelectJobsTable() {
        mockOutbox(false, false, false);
        mockInbox(true, true);

        List<Resource> scripts = OnceboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.POSTGRESQL);

        assertThat(filenamesOf(scripts))
                .containsExactly("psql_inbox_table.sql", "psql_oncebox_jobs_table.sql");
    }

    @Test
    @DisplayName("UT resolveScripts() when everything disabled should select nothing")
    void resolveScripts_whenEverythingDisabled_shouldSelectNothing() {
        mockOutbox(false, false, false);
        mockInbox(false, false);

        List<Resource> scripts = OnceboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.POSTGRESQL);

        assertThat(scripts).isEmpty();
    }

    @Test
    @DisplayName("UT resolveScripts() when MySQL should select MySQL scripts")
    void resolveScripts_whenMySql_shouldSelectMySqlScripts() {
        mockOutbox(true, true, true);
        mockInbox(true, true);

        List<Resource> scripts = OnceboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.MYSQL);

        assertThat(filenamesOf(scripts)).containsExactly(
                "mysql_outbox_table.sql",
                "mysql_outbox_dlq_table.sql",
                "mysql_inbox_table.sql",
                "mysql_oncebox_jobs_table.sql"
        );
    }

    @Test
    @DisplayName("UT resolveScripts() when Oracle should select Oracle scripts")
    void resolveScripts_whenOracle_shouldSelectOracleScripts() {
        mockOutbox(true, true, true);
        mockInbox(true, true);

        List<Resource> scripts = OnceboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.ORACLE);

        assertThat(filenamesOf(scripts)).containsExactly(
                "oracle_outbox_table.sql",
                "oracle_outbox_dlq_table.sql",
                "oracle_inbox_table.sql",
                "oracle_oncebox_jobs_table.sql"
        );
    }
}
