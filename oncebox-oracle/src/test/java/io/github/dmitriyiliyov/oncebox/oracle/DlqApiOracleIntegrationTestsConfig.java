package io.github.dmitriyiliyov.oncebox.oracle;

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
import java.time.Clock;

@TestConfiguration
@Profile("oracle-it")
public class DlqApiOracleIntegrationTestsConfig {

    @Bean
    public DataSourceInitializer oracleOutboxDataSourceInitializer(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setSeparator("/");
        populator.setScripts(
                new ClassPathResource("oracle/oracle_outbox_dlq_table.sql")
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
    public OutboxDlqApiRepository oracleOutboxDlqApiRepository(DataSource dataSource, Clock clock) {
        return new OracleOutboxDlqApiRepository(new JdbcTemplate(dataSource), new OracleSqlUuidHelper(), new DefaultBytesResultSetMapper(), clock);
    }
}
