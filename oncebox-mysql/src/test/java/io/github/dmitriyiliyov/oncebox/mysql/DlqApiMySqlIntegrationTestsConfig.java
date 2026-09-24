package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.utils.DefaultBytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.dlq.api.OutboxDlqApiRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

@TestConfiguration
@Profile("mysql-it")
public class DlqApiMySqlIntegrationTestsConfig {

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
                        new ClassPathResource("mysql/mysql_outbox_dlq_table.sql"))
        );
        return dataSourceInitializer;
    }

    @Bean
    public OutboxDlqApiRepository mysqlOutboxDlqApiRepository(DataSource dataSource, Clock clock) {
        return new MySqlOutboxDlqApiRepository(new JdbcTemplate(dataSource), new MySqlUuidHelper(), new DefaultBytesResultSetMapper(), clock);
    }

    @Bean
    public JdbcTemplate mysqlJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
