package io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared;

import io.github.dmitriyiliyov.oncebox.tests.integration.*;
import io.github.dmitriyiliyov.oncebox.tests.integration.utils.IdExtractor;
import io.github.dmitriyiliyov.oncebox.tests.integration.utils.IdPreparer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;

import javax.sql.DataSource;

@TestConfiguration
public class InboxIntegrationTestsConfig {

    private static final DatabaseContainer CONTAINER = DatabaseContainerFactory.DB_CONTAINER;
    private static final DatabaseType DATABASE_TYPE = CONTAINER.getDatabaseType();

    @Bean
    public DataSourceInitializer testOutboxDataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setEnabled(true);
        dataSourceInitializer.setDataSource(dataSource);
        dataSourceInitializer.setDatabasePopulator(ResourceDatabasePopulatorFactory.generate(DATABASE_TYPE));
        return dataSourceInitializer;
    }

    @Bean
    public IdPreparer idPreparer() {
        return IdPreparerFactory.generate(DATABASE_TYPE);
    }

    @Bean
    public IdExtractor idExtractor() {
        return IdExtractorFactory.generate(DATABASE_TYPE);
    }

    @Bean
    public ConsumerBusinessRepository jdbcConsumerBusinessRepository(
            @Qualifier("onceboxJdbcTemplate") JdbcTemplate jdbcTemplate,
            IdPreparer idPreparer
    ) {
        return new JdbcConsumerBusinessRepository(jdbcTemplate, idPreparer);
    }

    @Bean
    public MeterRegistry simpleMeterRegistry() {
        return new SimpleMeterRegistry();
    }
}
