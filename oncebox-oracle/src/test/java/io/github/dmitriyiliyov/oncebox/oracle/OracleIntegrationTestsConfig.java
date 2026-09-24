package io.github.dmitriyiliyov.oncebox.oracle;

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
import java.time.Clock;

@TestConfiguration
@Profile("oracle-it")
public class OracleIntegrationTestsConfig {

    @Bean
    public DataSourceInitializer oracleOutboxDataSourceInitializer(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSeparator("/");
        populator.setScripts(
                new ClassPathResource("oracle/oracle_outbox_table.sql"),
                new ClassPathResource("oracle/oracle_outbox_dlq_table.sql"),
                new ClassPathResource("oracle/oracle_outbox_consumed_table.sql"),
                new ClassPathResource("oracle/oracle_outbox_jobs_table.sql"),
                new ClassPathResource("oracle/oracle_business_table.sql")
        );
        populator.setContinueOnError(false);
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public OutboxDlqRepository oracleOutboxDlqRepository(DataSource dataSource, Clock clock) {
        return new OracleOutboxDlqRepository(new JdbcTemplate(dataSource), new OracleSqlUuidHelper(), new DefaultBytesResultSetMapper(), clock);
    }

    @Bean
    public OutboxRepository oracleOutboxRepository(DataSource dataSource, Clock clock) {
        return new OracleOutboxRepository(new JdbcTemplate(dataSource), clock, new OracleSqlUuidHelper(), new DefaultBytesResultSetMapper());
    }

    @Bean
    public ConsumedOutboxRepository oracleConsumedOutboxRepository(DataSource dataSource, Clock clock) {
        return new OracleConsumedOutboxRepository(new JdbcTemplate(dataSource), clock, new OracleSqlUuidHelper(), new DefaultBytesResultSetMapper());
    }

    @Bean
    public JdbcTemplate oracleJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public OutboxDlqManager oracleOutboxDlqManager(@Qualifier("oracleOutboxDlqRepository") OutboxDlqRepository repository,
                                                   Clock clock) {
        return new DefaultOutboxDlqManager(repository, clock);
    }

    @Bean
    public OutboxManager oracleOutboxManager(@Qualifier("oracleOutboxRepository") OutboxRepository repository,
                                             Clock clock) {
        return new DefaultOutboxManager(repository, clock);
    }

    @Bean
    public OutboxDlqTransfer oracleOutboxDlqTransfer(
            PlatformTransactionManager transactionManager,
            @Qualifier("oracleOutboxManager") OutboxManager manager,
            @Qualifier("oracleOutboxDlqManager") OutboxDlqManager dlqManager,
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
