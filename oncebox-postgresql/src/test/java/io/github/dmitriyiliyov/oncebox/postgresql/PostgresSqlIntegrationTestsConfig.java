package io.github.dmitriyiliyov.oncebox.postgresql;

import io.github.dmitriyiliyov.oncebox.core.inbox.InboxRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.DefaultOutboxManager;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxManager;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.*;
import io.github.dmitriyiliyov.oncebox.core.utils.DefaultResultSetMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

@TestConfiguration
@Profile("postgres-it")
public class PostgresSqlIntegrationTestsConfig {

    @Bean
    public DataSourceInitializer postgresOutboxDataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setEnabled(true);
        dataSourceInitializer.setDataSource(dataSource);
        dataSourceInitializer.setDatabasePopulator(
                new ResourceDatabasePopulator(
                        false,
                        false,
                        StandardCharsets.UTF_8.name(),
                        new ClassPathResource("psql/psql_outbox_table.sql"),
                        new ClassPathResource("psql/psql_outbox_dlq_table.sql"),
                        new ClassPathResource("psql/psql_inbox_table.sql"),
                        new ClassPathResource("psql/psql_oncebox_jobs_table.sql"),
                        new ClassPathResource("psql/psql_business_table.sql"))
        );
        return dataSourceInitializer;
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public OutboxDlqRepository postgresOutboxDlqRepository(DataSource dataSource) {
        return new PostgreSqlOutboxDlqRepository(new JdbcTemplate(dataSource), new PostgreSqlUuidHelper(), new DefaultResultSetMapper());
    }

    @Bean
    public OutboxRepository postgresOutboxRepository(DataSource dataSource) {
        return new PostgreSqlOutboxRepository(new JdbcTemplate(dataSource), new PostgreSqlUuidHelper(), new DefaultResultSetMapper());
    }

    @Bean
    public InboxRepository postgresInboxRepository(DataSource dataSource) {
        return new PostgreSqlInboxRepository(new JdbcTemplate(dataSource));
    }

    @Bean
    public JdbcTemplate postgresJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public OutboxDlqManager postgresOutboxDlqManager(@Qualifier("postgresOutboxDlqRepository") OutboxDlqRepository repository) {
        return new DefaultOutboxDlqManager(repository);
    }

    @Bean
    public OutboxManager postgresOutboxManager(@Qualifier("postgresOutboxRepository") OutboxRepository repository) {
        return new DefaultOutboxManager(repository);
    }

    @Bean
    public OutboxDlqTransfer postgresOutboxDlqTransfer(
            PlatformTransactionManager transactionManager,
            @Qualifier("postgresOutboxManager") OutboxManager manager,
            @Qualifier("postgresOutboxDlqManager") OutboxDlqManager dlqManager
    ) {
        return new DefaultOutboxDlqTransfer(
                new TransactionTemplate(transactionManager),
                manager,
                dlqManager,
                new DefaultOutboxDlqEventMapper(),
                new LogOutboxDlqHandler()
        );
    }
}
