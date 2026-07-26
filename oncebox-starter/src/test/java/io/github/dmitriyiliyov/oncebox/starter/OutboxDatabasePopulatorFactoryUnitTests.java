package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.starter.consumer.OutboxConsumerProperties;
import io.github.dmitriyiliyov.oncebox.starter.publisher.OutboxPublisherProperties;
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
class OutboxDatabasePopulatorFactoryUnitTests {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Mock
    private OutboxProperties properties;

    @Mock
    private OutboxPublisherProperties publisherProperties;

    @Mock
    private OutboxPublisherProperties.DlqProperties dlqProperties;

    @Mock
    private OutboxConsumerProperties consumerProperties;

    @Mock
    private OutboxProperties.CleanUpProperties publisherCleanUpProperties;

    @Mock
    private OutboxProperties.CleanUpProperties consumerCleanUpProperties;

    private void mockDbProductName(String productName) throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn(productName);
    }

    private void mockPublisher(boolean enabled, boolean dlqEnabled, boolean cleanUpEnabled) {
        when(properties.getPublisher()).thenReturn(publisherProperties);
        when(publisherProperties.isEnabled()).thenReturn(enabled);
        when(publisherProperties.getDlq()).thenReturn(dlqProperties);
        when(dlqProperties.isEnabled()).thenReturn(dlqEnabled);
        when(publisherProperties.getCleanUp()).thenReturn(publisherCleanUpProperties);
        when(publisherCleanUpProperties.isEnabled()).thenReturn(cleanUpEnabled);
    }

    private void mockConsumer(boolean enabled, boolean cleanUpEnabled) {
        when(properties.getConsumer()).thenReturn(consumerProperties);
        when(consumerProperties.isEnabled()).thenReturn(enabled);
        when(consumerProperties.getCleanUp()).thenReturn(consumerCleanUpProperties);
        when(consumerCleanUpProperties.isEnabled()).thenReturn(cleanUpEnabled);
    }

    private static List<String> filenamesOf(List<Resource> scripts) {
        return scripts.stream().map(Resource::getFilename).toList();
    }

    @Test
    @DisplayName("UT create() when properties is null should throw NullPointerException")
    void create_whenPropertiesNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> OutboxDatabasePopulatorFactory.create(null, dataSource))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("properties cannot be null");
    }

    @Test
    @DisplayName("UT create() when dataSource is null should throw NullPointerException")
    void create_whenDataSourceNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> OutboxDatabasePopulatorFactory.create(properties, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("dataSource cannot be null");
    }

    @Test
    @DisplayName("UT create() when PostgreSQL and only base tables enabled should return populator")
    void create_whenPostgreSqlAndBaseTables_shouldReturnPopulator() throws SQLException {
        mockDbProductName("PostgreSQL");
        mockPublisher(true, false, true);
        when(properties.getConsumer()).thenReturn(null);

        DatabasePopulator result = OutboxDatabasePopulatorFactory.create(properties, dataSource);

        assertThat(result).isNotNull().isInstanceOf(ResourceDatabasePopulator.class);
        verify(connection).close();
    }

    @Test
    @DisplayName("UT create() when connection fails should throw RuntimeException")
    void create_whenConnectionFails_shouldThrowRuntimeException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        assertThatThrownBy(() -> OutboxDatabasePopulatorFactory.create(properties, dataSource))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(MetaDataAccessException.class);
    }

    @Test
    @DisplayName("UT create() when metadata extraction fails should throw RuntimeException")
    void create_whenMetadataExtractionFails_shouldThrowRuntimeException() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenThrow(new SQLException("Metadata failed"));

        assertThatThrownBy(() -> OutboxDatabasePopulatorFactory.create(properties, dataSource))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(MetaDataAccessException.class);
    }

    @Test
    @DisplayName("UT resolveScripts() when publisher only should select outbox and jobs tables")
    void resolveScripts_whenPublisherOnly_shouldSelectOutboxAndJobsTables() {
        mockPublisher(true, false, true);
        mockConsumer(false, false);

        List<Resource> scripts = OutboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.POSTGRESQL);

        assertThat(filenamesOf(scripts))
                .containsExactly("psql_outbox_table.sql", "psql_outbox_jobs_table.sql");
    }

    @Test
    @DisplayName("UT resolveScripts() when publisher with DLQ should select DLQ table too")
    void resolveScripts_whenPublisherWithDlq_shouldSelectDlqTable() {
        mockPublisher(true, true, true);
        mockConsumer(false, false);

        List<Resource> scripts = OutboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.POSTGRESQL);

        assertThat(filenamesOf(scripts)).containsExactly(
                "psql_outbox_table.sql",
                "psql_outbox_dlq_table.sql",
                "psql_outbox_jobs_table.sql"
        );
    }

    @Test
    @DisplayName("UT resolveScripts() when publisher disabled should not select outbox and DLQ tables")
    void resolveScripts_whenPublisherDisabled_shouldNotSelectOutboxTable() {
        mockPublisher(false, true, true);
        mockConsumer(true, true);

        List<Resource> scripts = OutboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.POSTGRESQL);

        assertThat(filenamesOf(scripts))
                .containsExactly("psql_outbox_consumed_table.sql", "psql_outbox_jobs_table.sql");
    }

    @Test
    @DisplayName("UT resolveScripts() when consumer disabled should not select consumed table")
    void resolveScripts_whenConsumerDisabled_shouldNotSelectConsumedTable() {
        mockPublisher(true, false, true);
        mockConsumer(false, true);

        List<Resource> scripts = OutboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.POSTGRESQL);

        assertThat(filenamesOf(scripts)).doesNotContain("psql_outbox_consumed_table.sql");
    }

    @Test
    @DisplayName("UT resolveScripts() when no clean-up enabled should not select jobs table")
    void resolveScripts_whenNoCleanUpEnabled_shouldNotSelectJobsTable() {
        mockPublisher(true, false, false);
        mockConsumer(true, false);

        List<Resource> scripts = OutboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.POSTGRESQL);

        assertThat(filenamesOf(scripts))
                .containsExactly("psql_outbox_table.sql", "psql_outbox_consumed_table.sql");
    }

    @Test
    @DisplayName("UT resolveScripts() when only consumer clean-up enabled should select jobs table")
    void resolveScripts_whenOnlyConsumerCleanUpEnabled_shouldSelectJobsTable() {
        mockPublisher(false, false, false);
        mockConsumer(true, true);

        List<Resource> scripts = OutboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.POSTGRESQL);

        assertThat(filenamesOf(scripts))
                .containsExactly("psql_outbox_consumed_table.sql", "psql_outbox_jobs_table.sql");
    }

    @Test
    @DisplayName("UT resolveScripts() when everything disabled should select nothing")
    void resolveScripts_whenEverythingDisabled_shouldSelectNothing() {
        mockPublisher(false, false, false);
        mockConsumer(false, false);

        List<Resource> scripts = OutboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.POSTGRESQL);

        assertThat(scripts).isEmpty();
    }

    @Test
    @DisplayName("UT resolveScripts() when MySQL should select MySQL scripts")
    void resolveScripts_whenMySql_shouldSelectMySqlScripts() {
        mockPublisher(true, true, true);
        mockConsumer(true, true);

        List<Resource> scripts = OutboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.MYSQL);

        assertThat(filenamesOf(scripts)).containsExactly(
                "mysql_outbox_table.sql",
                "mysql_outbox_dlq_table.sql",
                "mysql_outbox_consumed_table.sql",
                "mysql_outbox_jobs_table.sql"
        );
    }

    @Test
    @DisplayName("UT resolveScripts() when Oracle should select Oracle scripts")
    void resolveScripts_whenOracle_shouldSelectOracleScripts() {
        mockPublisher(true, true, true);
        mockConsumer(true, true);

        List<Resource> scripts = OutboxDatabasePopulatorFactory.resolveScripts(properties, DatabaseType.ORACLE);

        assertThat(filenamesOf(scripts)).containsExactly(
                "oracle_outbox_table.sql",
                "oracle_outbox_dlq_table.sql",
                "oracle_outbox_consumed_table.sql",
                "oracle_outbox_jobs_table.sql"
        );
    }
}
