package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.DefaultOutboxManager;
import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxManager;
import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.*;
import io.github.dmitriyiliyov.oncebox.core.utils.DefaultBytesResultSetMapper;
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
@Profile("mysql-it")
public class MySqlIntegrationTestsConfig {

    @Bean
    public DataSourceInitializer myslqOutboxDataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setEnabled(true);
        dataSourceInitializer.setDataSource(dataSource);
        dataSourceInitializer.setDatabasePopulator(
                new ResourceDatabasePopulator(
                        false,
                        false,
                        StandardCharsets.UTF_8.name(),
                        new ClassPathResource("mysql/mysql_outbox_table.sql"),
                        new ClassPathResource("mysql/mysql_outbox_dlq_table.sql"),
                        new ClassPathResource("mysql/mysql_outbox_consumed_table.sql"),
                        new ClassPathResource("mysql/mysql_outbox_jobs_table.sql"),
                        new ClassPathResource("mysql/mysql_business_table.sql")
                )
        );
        return dataSourceInitializer;
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public OutboxDlqRepository mysqlOutboxDlqRepository(DataSource dataSource, Clock clock) {
        return new MySqlOutboxDlqRepository(new JdbcTemplate(dataSource), new MySqlUuidHelper(), new DefaultBytesResultSetMapper(), clock);
    }

    @Bean
    public OutboxRepository mysqlOutboxRepository(DataSource dataSource, Clock clock) {
        return new MySqlOutboxRepository(new JdbcTemplate(dataSource), clock, new MySqlUuidHelper(), new DefaultBytesResultSetMapper());
    }

    @Bean
    public ConsumedOutboxRepository mysqlConsumedOutboxRepository(DataSource dataSource, Clock clock) {
        return new MySqlConsumedOutboxRepository(new JdbcTemplate(dataSource), clock, new MySqlUuidHelper(), new DefaultBytesResultSetMapper());
    }

    @Bean
    public JdbcTemplate mysqlJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public OutboxDlqManager mysqlOutboxDlqManager(@Qualifier("mysqlOutboxDlqRepository") OutboxDlqRepository repository,
                                                  Clock clock) {
        return new DefaultOutboxDlqManager(repository, clock);
    }

    @Bean
    public OutboxManager mysqlOutboxManager(@Qualifier("mysqlOutboxRepository") OutboxRepository repository,
                                            Clock clock) {
        return new DefaultOutboxManager(repository, clock);
    }

    @Bean
    public OutboxDlqTransfer mysqlOutboxDlqTransfer(
            PlatformTransactionManager transactionManager,
            @Qualifier("mysqlOutboxManager") OutboxManager manager,
            @Qualifier("mysqlOutboxDlqManager") OutboxDlqManager dlqManager,
            Clock clock
    ) {
        return new DefaultOutboxDlqTransfer(
                new TransactionTemplate(transactionManager),
                manager,
                dlqManager,
                new DefaultOutboxDlqEventMapper(clock),
                new LogOutboxDlqHandler()
        );
    }
}
