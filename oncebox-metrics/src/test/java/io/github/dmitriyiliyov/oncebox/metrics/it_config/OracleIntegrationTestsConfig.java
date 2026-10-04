package io.github.dmitriyiliyov.oncebox.metrics.it_config;

import io.github.dmitriyiliyov.oncebox.metrics.outbox.MultiDialectOutboxMetricsRepository;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.OutboxMetricsRepository;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq.MultiDialectOutboxDlqMetricsRepository;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq.OutboxDlqMetricsRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

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
                new ClassPathResource("oracle/oracle_inbox_table.sql")
        );
        populator.setContinueOnError(false);
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }

    @Bean
    public JdbcTemplate oracleJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public OutboxDlqMetricsRepository oracleDlqMetricsRepo(@Qualifier("oracleJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return new MultiDialectOutboxDlqMetricsRepository(jdbcTemplate);
    }

    @Bean
    public OutboxMetricsRepository oracleMetricsRepo(@Qualifier("oracleJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return new MultiDialectOutboxMetricsRepository(jdbcTemplate);
    }
}
