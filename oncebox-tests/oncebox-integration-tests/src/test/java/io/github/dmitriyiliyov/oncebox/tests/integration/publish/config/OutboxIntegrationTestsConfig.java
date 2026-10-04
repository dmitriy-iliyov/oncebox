package io.github.dmitriyiliyov.oncebox.tests.integration.publish.config;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxPublisher;
import io.github.dmitriyiliyov.oncebox.tests.integration.*;
import io.github.dmitriyiliyov.oncebox.tests.integration.publish.BusinessRepository;
import io.github.dmitriyiliyov.oncebox.tests.integration.publish.JdbcBusinessRepository;
import io.github.dmitriyiliyov.oncebox.tests.integration.publish.aop.AopBusinessService;
import io.github.dmitriyiliyov.oncebox.tests.integration.publish.manual.ManualBusinessService;
import io.github.dmitriyiliyov.oncebox.tests.integration.utils.IdExtractor;
import io.github.dmitriyiliyov.oncebox.tests.integration.utils.IdPreparer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import javax.sql.DataSource;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class OutboxIntegrationTestsConfig {

    public static final DatabaseContainer CONTAINER = DatabaseContainerFactory.DB_CONTAINER;
    public static final DatabaseType DATABASE_TYPE = CONTAINER.getDatabaseType();

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
    public ManualBusinessService manualJdbcBusinessService(OutboxPublisher publisher,
                                                           JdbcTemplate jdbcTemplate,
                                                           IdPreparer idPreparer) {
        return new ManualBusinessService(
                publisher,
                new JdbcBusinessRepository(jdbcTemplate, idPreparer)
        );
    }

    @Bean
    public ManualBusinessService manualJpaBusinessService(
            OutboxPublisher publisher,
            @Qualifier("jpaBusinessRepositoryProxy") BusinessRepository repository
    ) {
        return new ManualBusinessService(publisher, repository);
    }

    @Bean
    public AopBusinessService aopJdbcBusinessService(
            @Qualifier("onceboxJdbcTemplate") JdbcTemplate jdbcTemplate,
            IdPreparer idPreparer
    ) {
        return new AopBusinessService(
                new JdbcBusinessRepository(jdbcTemplate, idPreparer)
        );
    }

    @Bean
    public AopBusinessService aopJpaBusinessService(@Qualifier("jpaBusinessRepositoryProxy") BusinessRepository repository) {
        return new AopBusinessService(repository);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        ProducerFactory<String, Object> producerFactory = mock(ProducerFactory.class);
        when(producerFactory.getConfigurationProperties()).thenReturn(Map.of());
        when(kafkaTemplate.getProducerFactory()).thenReturn(producerFactory);
        return kafkaTemplate;
    }

    @Bean
    public MeterRegistry simpleMeterRegistry() {
        return new SimpleMeterRegistry();
    }
}
