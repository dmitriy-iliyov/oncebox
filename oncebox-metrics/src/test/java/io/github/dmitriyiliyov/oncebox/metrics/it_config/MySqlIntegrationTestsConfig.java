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
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

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
                        new Resource[] {
                                new ClassPathResource("mysql/mysql_outbox_table.sql"),
                                new ClassPathResource("mysql/mysql_outbox_dlq_table.sql"),
                                new ClassPathResource("mysql/mysql_inbox_table.sql")
                        }
                )
        );
        return dataSourceInitializer;
    }

    @Bean
    public JdbcTemplate mysqlJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public OutboxDlqMetricsRepository mysqlDlqMetricsRepo(@Qualifier("mysqlJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return new MultiDialectOutboxDlqMetricsRepository(jdbcTemplate);
    }

    @Bean
    public OutboxMetricsRepository mysqlMetricsRepo(@Qualifier("mysqlJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return new MultiDialectOutboxMetricsRepository(jdbcTemplate);
    }
}
