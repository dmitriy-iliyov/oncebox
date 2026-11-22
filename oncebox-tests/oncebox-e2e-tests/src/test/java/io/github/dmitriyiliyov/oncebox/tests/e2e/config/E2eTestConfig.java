package io.github.dmitriyiliyov.oncebox.tests.e2e.config;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxPublisher;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxSender;
import io.github.dmitriyiliyov.oncebox.tests.e2e.consume.RecordingBusinessOperation;
import io.github.dmitriyiliyov.oncebox.tests.e2e.publish.CrashAfterSendOutboxSender;
import io.github.dmitriyiliyov.oncebox.tests.e2e.publish.PublisherBusinessService;
import io.github.dmitriyiliyov.oncebox.tests.e2e.repository.TestOnceboxRepositoryFactory;
import io.github.dmitriyiliyov.oncebox.tests.e2e.repository.TestOutboxRepository;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

/**
 * Broker-agnostic beans shared across all runs, the library's sender among them wrapped into a
 * {@link CrashAfterSendOutboxSender}, which stays transparent until a test turns crashing on. Broker-specific wiring (sender template, topology,
 * consumer, raw resender) lives in {@link KafkaBrokerConfig} / {@link RabbitBrokerConfig}, gated by profile.
 */
@TestConfiguration
public class E2eTestConfig {

    public static final DatabaseContainer CONTAINER = DatabaseContainerFactory.DB_CONTAINER;
    public static final DatabaseType DATABASE_TYPE = CONTAINER.getDatabaseType();

    @Bean
    public DataSourceInitializer e2eBusinessTablesInitializer(DataSource dataSource) {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setEnabled(true);
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(new ResourceDatabasePopulator(
                // continueOnError so re-running a plain CREATE TABLE (Oracle has no IF NOT EXISTS) is idempotent
                true,
                false,
                StandardCharsets.UTF_8.name(),
                new ClassPathResource(businessTablesScript())
        ));
        return initializer;
    }

    private String businessTablesScript() {
        return switch (DATABASE_TYPE) {
            case POSTGRES_SQL -> "psql/e2e_business_tables.sql";
            case MY_SQL -> "mysql/e2e_business_tables.sql";
            case ORACLE -> "oracle/e2e_business_tables.sql";
        };
    }

    @Bean
    public TestOutboxRepository testOutboxRepository(JdbcTemplate jdbcTemplate) {
        return TestOnceboxRepositoryFactory.generate(DATABASE_TYPE, jdbcTemplate);
    }

    @Bean
    public RecordingBusinessOperation recordingBusinessOperation(TestOutboxRepository repository) {
        return new RecordingBusinessOperation(repository);
    }

    // Static, so the post-processor exists before the sender is created and wraps it
    @Bean
    public static BeanPostProcessor crashAfterSendOutboxSenderPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                return bean instanceof OutboxSender sender ? new CrashAfterSendOutboxSender(sender) : bean;
            }
        };
    }

    @Bean
    public PublisherBusinessService publisherBusinessService(OutboxPublisher publisher,
                                                             TestOutboxRepository repository) {
        return new PublisherBusinessService(publisher, repository);
    }
}
