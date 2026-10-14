package io.github.dmitriyiliyov.oncebox.starter.inbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.core.inbox.*;
import io.github.dmitriyiliyov.oncebox.core.inbox.cache.IdempotentConsumerCacheDecorator;
import io.github.dmitriyiliyov.oncebox.metrics.inbox.IdempotentConsumerMetricsDecorator;
import io.github.dmitriyiliyov.oncebox.metrics.inbox.InboxManagerMetricsDecorator;
import io.github.dmitriyiliyov.oncebox.metrics.inbox.MetricsInboxCacheListener;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxAutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InboxAutoConfigurationVerifier {

    private static final String TRANSACTIONAL_CONSUMER = "inboxTransactionalIdempotentConsumer";
    private static final String NON_TRANSACTIONAL_CONSUMER = "inboxNonTransactionalIdempotentConsumer";

    private final String dbUrl;
    private final String dbDriver;
    private final String dbPassword;
    private final String dbUsername;

    public InboxAutoConfigurationVerifier(String dbUrl, String dbDriver, String dbPassword, String dbUsername) {
        this.dbUrl = dbUrl;
        this.dbDriver = dbDriver;
        this.dbPassword = dbPassword;
        this.dbUsername = dbUsername;
    }

    private ApplicationContextRunner getBaseContextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class,
                        TransactionAutoConfiguration.class,
                        CacheAutoConfiguration.class,
                        OnceboxAutoConfiguration.class,
                        InboxAutoConfiguration.class,
                        KafkaAutoConfiguration.class,
                        RabbitAutoConfiguration.class
                ))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(CacheManager.class, () -> new ConcurrentMapCacheManager("oncebox"))
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword,
                        "oncebox.tables.auto-create=false",
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.source.type=kafka",
                        "oncebox.inbox.mappings.test-event=io.github.dmitriyiliyov.oncebox.starter.inbox.TestEvent",
                        "oncebox.inbox.cache.cache-name=oncebox"
                );
    }

    public void shouldLoadFullConfiguration_whenAllFeaturesEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.inbox.metrics.enabled=true",
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.cache.cache-name=oncebox",
                        "oncebox.inbox.clean-up.enabled=true",
                        "oncebox.inbox.clean-up.interval=PT1M",
                        "oncebox.inbox.clean-up.retention=PT24H"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(InboxRepository.class);
                    assertThat(ctx).hasSingleBean(DefaultInboxManager.class);
                    assertThat(ctx).hasSingleBean(InboxManagerMetricsDecorator.class);
                    assertThat(ctx).hasSingleBean(TransactionalIdempotentConsumer.class);
                    assertThat(ctx).hasSingleBean(NonTransactionalIdempotentConsumer.class);
                    assertThat(ctx).hasSingleBean(MetricsInboxCacheListener.class);

                    assertThat(ctx).hasBean("inboxKafkaRecordMessageConverter");
                    assertThat(ctx).hasBean("inboxKafkaListenerContainerFactory");
                    assertThat(ctx).doesNotHaveBean("inboxRabbitMessageConverter");
                    assertThat(ctx).doesNotHaveBean("inboxRabbitListenerContainerFactory");

                    assertThat(ctx).hasSingleBean(IdempotentConsumerCacheDecoratorSupplier.class);
                    assertThat(ctx).hasSingleBean(IdempotentConsumerMetricsDecoratorSupplier.class);

                    InboxManager primaryInboxManager = ctx.getBean(InboxManager.class);
                    assertThat(primaryInboxManager).isInstanceOf(InboxManagerMetricsDecorator.class);

                    assertThat(ctx.getBean(TRANSACTIONAL_CONSUMER)).isInstanceOf(IdempotentConsumerMetricsDecorator.class);
                    assertThat(ctx.getBean(NON_TRANSACTIONAL_CONSUMER)).isInstanceOf(IdempotentConsumerMetricsDecorator.class);

                    assertThat(ctx).hasBean("inboxCleanUpScheduler");
                });
    }

    public void shouldLoadFullConfiguration_whenAllFeaturesWithRabbitEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.inbox.source.type=rabbit",
                        "oncebox.inbox.metrics.enabled=true",
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.cache.cache-name=oncebox",
                        "oncebox.inbox.clean-up.enabled=true",
                        "oncebox.inbox.clean-up.interval=PT1M",
                        "oncebox.inbox.clean-up.retention=PT24H"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(InboxRepository.class);
                    assertThat(ctx).hasSingleBean(DefaultInboxManager.class);
                    assertThat(ctx).hasSingleBean(InboxManagerMetricsDecorator.class);
                    assertThat(ctx).hasSingleBean(TransactionalIdempotentConsumer.class);
                    assertThat(ctx).hasSingleBean(NonTransactionalIdempotentConsumer.class);
                    assertThat(ctx).hasSingleBean(MetricsInboxCacheListener.class);

                    assertThat(ctx).hasBean("inboxRabbitMessageConverter");
                    assertThat(ctx).hasBean("inboxRabbitListenerContainerFactory");
                    assertThat(ctx).doesNotHaveBean("inboxKafkaRecordMessageConverter");
                    assertThat(ctx).doesNotHaveBean("inboxKafkaListenerContainerFactory");

                    assertThat(ctx).hasSingleBean(IdempotentConsumerCacheDecoratorSupplier.class);
                    assertThat(ctx).hasSingleBean(IdempotentConsumerMetricsDecoratorSupplier.class);

                    InboxManager primaryInboxManager = ctx.getBean(InboxManager.class);
                    assertThat(primaryInboxManager).isInstanceOf(InboxManagerMetricsDecorator.class);

                    assertThat(ctx.getBean(TRANSACTIONAL_CONSUMER)).isInstanceOf(IdempotentConsumerMetricsDecorator.class);
                    assertThat(ctx.getBean(NON_TRANSACTIONAL_CONSUMER)).isInstanceOf(IdempotentConsumerMetricsDecorator.class);

                    assertThat(ctx).hasBean("inboxCleanUpScheduler");
                });
    }

    public void shouldLoadMinimalConfiguration_whenOnlyRequiredPropertiesSet() {
        getBaseContextRunner()
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(InboxRepository.class);
                    assertThat(ctx).hasBean("inboxManager");
                    assertThat(ctx).hasBean(TRANSACTIONAL_CONSUMER);
                    assertThat(ctx).hasBean(NON_TRANSACTIONAL_CONSUMER);

                    InboxManager primaryInboxManager = ctx.getBean(InboxManager.class);
                    assertThat(primaryInboxManager).isInstanceOf(DefaultInboxManager.class);

                    assertThat(ctx).doesNotHaveBean(IdempotentConsumerMetricsDecoratorSupplier.class);
                    assertThat(ctx).doesNotHaveBean(InboxManagerMetricsDecorator.class);
                    assertThat(ctx).doesNotHaveBean(IdempotentConsumerMetricsDecorator.class);
                    assertThat(ctx).doesNotHaveBean("inboxCleanUpScheduler");
                });
    }

    public void shouldNotLoad_whenDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class,
                        TransactionAutoConfiguration.class,
                        CacheAutoConfiguration.class,
                        OnceboxAutoConfiguration.class,
                        InboxAutoConfiguration.class
                ))
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword,
                        "oncebox.tables.auto-create=false",
                        "oncebox.inbox.enabled=false"
                )
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(TransactionalIdempotentConsumer.class);
                    assertThat(ctx).doesNotHaveBean(DefaultInboxManager.class);
                    assertThat(ctx).doesNotHaveBean(InboxRepository.class);
                });
    }

    public void shouldRegisterInboxRepository() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(InboxRepository.class)
        );
    }

    public void shouldRegisterInboxManager() {
        getBaseContextRunner().run(ctx -> {
            assertThat(ctx).doesNotHaveBean(IdempotentConsumerDecoratorSupplier.class);
            assertThat(ctx).hasBean("inboxManager");
            assertThat(ctx).hasBean(TRANSACTIONAL_CONSUMER);
            assertThat(ctx).hasBean(NON_TRANSACTIONAL_CONSUMER);

            InboxManager primaryInboxManager = ctx.getBean(InboxManager.class);
            assertThat(primaryInboxManager).isInstanceOf(DefaultInboxManager.class);
        });
    }

    public void shouldNotRegisterCleanUpScheduler_whenDisabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.inbox.clean-up.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean("inboxCleanUpScheduler");
                    assertThat(ctx).doesNotHaveBean("inboxCleanUpJobCreateCommand");
                });
    }

    public void shouldNotRegisterCleanUpScheduler_whenEnabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.inbox.clean-up.enabled=true")
                .run(ctx -> {
                            assertThat(ctx).hasBean("inboxCleanUpScheduler");
                            assertThat(ctx).hasBean("inboxCleanUpJobCreateCommand");
                });
    }

    public void shouldNotRegisterDuplicateInboxManager_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean(
                        "inboxManager",
                        InboxManager.class,
                        () -> org.mockito.Mockito.mock(InboxManager.class)
                )
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(IdempotentConsumerDecoratorSupplier.class);
                    assertThat(ctx).hasBean("inboxManager");
                    assertThat(ctx).hasBean(TRANSACTIONAL_CONSUMER);
                    assertThat(ctx).hasBean(NON_TRANSACTIONAL_CONSUMER);
                });
    }

    public void shouldCreateTables_whenAutoCreateTrue() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    JdbcTemplate jdbcTemplate = ctx.getBean("onceboxJdbcTemplate", JdbcTemplate.class);
                    jdbcTemplate.execute("SELECT 1 FROM inbox_events WHERE 1=0");
                    jdbcTemplate.execute("SELECT 1 FROM oncebox_jobs WHERE 1=0");
                });
    }

    public void shouldNotRegisteredMetricsRelatedBeans_whenMetricsDisabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.inbox.metrics.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(IdempotentConsumerMetricsDecoratorSupplier.class);
                    assertThat(ctx).doesNotHaveBean(InboxManagerMetricsDecorator.class);
                });
    }

    public void shouldRegisteredMetricsRelatedBeans_whenMetricsEnabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.inbox.metrics.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(InboxManagerMetricsDecorator.class);
                });
    }

    public void shouldNotRegisterDuplicateInboxRepository_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customInboxRepository", InboxRepository.class,
                        () -> org.mockito.Mockito.mock(InboxRepository.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(InboxRepository.class);
                    assertThat(ctx).hasBean("customInboxRepository");
                });
    }

    public void shouldNotRegisterDuplicateIdempotentConsumer_whenCustomBeanProvided() {
        IdempotentConsumer customConsumer = org.mockito.Mockito.mock(IdempotentConsumer.class);
        getBaseContextRunner()
                .withBean("inboxUndecoratedTransactionalIdempotentConsumer", IdempotentConsumer.class, () -> customConsumer)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).doesNotHaveBean(TransactionalIdempotentConsumer.class);
                    assertThat(ctx.getBean(TRANSACTIONAL_CONSUMER)).isSameAs(customConsumer);
                    assertThat(ctx.getBean(NON_TRANSACTIONAL_CONSUMER)).isInstanceOf(NonTransactionalIdempotentConsumer.class);
                });
    }

    public void shouldRegisterInboxManagerMetricsDecorator_asPrimary_whenMetricsEnabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.inbox.metrics.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasBean("inboxManager");
                    assertThat(ctx).hasBean(TRANSACTIONAL_CONSUMER);
                    assertThat(ctx).hasBean(NON_TRANSACTIONAL_CONSUMER);
                    InboxManager primary = ctx.getBean(InboxManager.class);
                    assertThat(primary).isInstanceOf(InboxManagerMetricsDecorator.class);
                });
    }

    public void shouldRegisterIdempotentConsumerMetricsDecorator_whenMetricsEnabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.inbox.metrics.enabled=true")
                .run(ctx -> {
                    assertThat(ctx.getBean(TRANSACTIONAL_CONSUMER)).isInstanceOf(IdempotentConsumerMetricsDecorator.class);
                    assertThat(ctx.getBean(NON_TRANSACTIONAL_CONSUMER)).isInstanceOf(IdempotentConsumerMetricsDecorator.class);
                });
    }

    public void shouldNotRegisterIdempotentConsumerMetricsDecorator_whenMetricsDisabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.inbox.metrics.enabled=false")
                .run(ctx ->
                        assertThat(ctx).doesNotHaveBean(IdempotentConsumerMetricsDecorator.class)
                );
    }

    public void shouldRegisterIdempotentConsumerCacheDecorator_asPrimary_whenCacheEnableAndMetricsDisabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.inbox.metrics.enabled=false",
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.cache.cache-name=oncebox",
                        "oncebox.inbox.clean-up.enabled=true",
                        "oncebox.inbox.clean-up.interval=PT1M",
                        "oncebox.inbox.clean-up.retention=PT24H"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(InboxRepository.class);
                    assertThat(ctx).hasSingleBean(DefaultInboxManager.class);
                    assertThat(ctx).hasSingleBean(TransactionalIdempotentConsumer.class);
                    assertThat(ctx).hasSingleBean(NonTransactionalIdempotentConsumer.class);
                    assertThat(ctx).doesNotHaveBean(IdempotentConsumerMetricsDecorator.class);

                    assertThat(ctx).hasSingleBean(IdempotentConsumerCacheDecoratorSupplier.class);
                    assertThat(ctx).doesNotHaveBean(IdempotentConsumerMetricsDecoratorSupplier.class);

                    InboxManager primaryInboxManager = ctx.getBean(InboxManager.class);
                    assertThat(primaryInboxManager).isInstanceOf(DefaultInboxManager.class);

                    assertThat(ctx.getBean(TRANSACTIONAL_CONSUMER)).isInstanceOf(IdempotentConsumerCacheDecorator.class);
                    assertThat(ctx.getBean(NON_TRANSACTIONAL_CONSUMER)).isInstanceOf(IdempotentConsumerCacheDecorator.class);

                    assertThat(ctx).hasBean("inboxCleanUpScheduler");
                    assertThat(ctx).hasBean("inboxCleanUpJobCreateCommand");
                });
    }

    public void shouldRegisterIdempotentConsumerCacheDecorator_asPrimary_whenCacheEnableAndMetricsMissed() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.cache.cache-name=oncebox",
                        "oncebox.inbox.clean-up.enabled=true"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(InboxRepository.class);
                    assertThat(ctx).hasSingleBean(DefaultInboxManager.class);
                    assertThat(ctx).doesNotHaveBean(InboxManagerMetricsDecorator.class);
                    assertThat(ctx).hasSingleBean(TransactionalIdempotentConsumer.class);
                    assertThat(ctx).hasSingleBean(NonTransactionalIdempotentConsumer.class);
                    assertThat(ctx).doesNotHaveBean(IdempotentConsumerMetricsDecorator.class);

                    assertThat(ctx).hasSingleBean(IdempotentConsumerCacheDecoratorSupplier.class);
                    assertThat(ctx).doesNotHaveBean(IdempotentConsumerMetricsDecoratorSupplier.class);

                    InboxManager primaryInboxManager = ctx.getBean(InboxManager.class);
                    assertThat(primaryInboxManager).isInstanceOf(DefaultInboxManager.class);

                    assertThat(ctx.getBean(TRANSACTIONAL_CONSUMER)).isInstanceOf(IdempotentConsumerCacheDecorator.class);
                    assertThat(ctx.getBean(NON_TRANSACTIONAL_CONSUMER)).isInstanceOf(IdempotentConsumerCacheDecorator.class);

                    assertThat(ctx).hasBean("inboxCleanUpScheduler");
                    assertThat(ctx).hasBean("inboxCleanUpJobCreateCommand");
                });
    }

    public void tryConsume_whenNoActiveTransaction_shouldCommitIdAtOnce() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=true")
                .run(ctx -> {
                    InboxManager manager = ctx.getBean(InboxManager.class);
                    UUID id = UUID.randomUUID();

                    assertThat(manager.tryConsume(id)).isTrue();
                    assertThat(manager.tryConsume(id)).isFalse();
                });
    }

    public void tryConsumeAndGetDuplicates_whenNoActiveTransaction_shouldCommitIdsAtOnce() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=true")
                .run(ctx -> {
                    InboxManager manager = ctx.getBean(InboxManager.class);
                    Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());

                    assertThat(manager.tryConsumeAndGetDuplicates(ids)).isEmpty();
                    assertThat(manager.tryConsumeAndGetDuplicates(ids)).isEqualTo(ids);
                });
    }

    public void tryConsume_whenCallerTransactionRolledBack_shouldRollBackIds() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=true")
                .run(ctx -> {
                    InboxManager manager = ctx.getBean(InboxManager.class);
                    UUID id = UUID.randomUUID();
                    Set<UUID> ids = Set.of(UUID.randomUUID());

                    transactionTemplate(ctx).executeWithoutResult(status -> {
                        assertThat(manager.tryConsume(id)).isTrue();
                        assertThat(manager.tryConsumeAndGetDuplicates(ids)).isEmpty();
                        status.setRollbackOnly();
                    });

                    assertThat(manager.tryConsume(id)).isTrue();
                    assertThat(manager.tryConsumeAndGetDuplicates(ids)).isEmpty();
                });
    }

    public void transactionalConsumer_whenOperationThrows_shouldProcessRedeliveryAgain() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=true")
                .run(ctx -> {
                    IdempotentConsumer consumer = ctx.getBean(TRANSACTIONAL_CONSUMER, IdempotentConsumer.class);
                    UUID id = UUID.randomUUID();
                    AtomicInteger runs = new AtomicInteger();

                    assertThatThrownBy(() -> consumer.consume(id, () -> { throw new IllegalStateException("failed"); }))
                            .isInstanceOf(IllegalStateException.class);
                    consumer.consume(id, runs::incrementAndGet);

                    assertThat(runs).hasValue(1);
                });
    }

    public void nonTransactionalConsumer_whenOperationThrows_shouldSkipRedelivery() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=true")
                .run(ctx -> {
                    IdempotentConsumer consumer = ctx.getBean(NON_TRANSACTIONAL_CONSUMER, IdempotentConsumer.class);
                    UUID id = UUID.randomUUID();
                    AtomicInteger runs = new AtomicInteger();

                    assertThatThrownBy(() -> consumer.consume(id, () -> { throw new IllegalStateException("failed"); }))
                            .isInstanceOf(IllegalStateException.class);
                    consumer.consume(id, runs::incrementAndGet);

                    assertThat(runs).hasValue(0);
                });
    }

    public void nonTransactionalConsumer_whenBatchOperationThrows_shouldSkipRedeliveredBatch() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=true")
                .run(ctx -> {
                    IdempotentConsumer consumer = ctx.getBean(NON_TRANSACTIONAL_CONSUMER, IdempotentConsumer.class);
                    Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
                    AtomicInteger runs = new AtomicInteger();

                    assertThatThrownBy(() -> consumer.consume(ids, batch -> { throw new IllegalStateException("failed"); }))
                            .isInstanceOf(IllegalStateException.class);
                    consumer.consume(ids, batch -> runs.incrementAndGet());

                    assertThat(runs).hasValue(0);
                });
    }

    public void nonTransactionalConsumer_whenCallerTransactionRolledBack_shouldProcessRedeliveryAgain() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=true")
                .run(ctx -> {
                    IdempotentConsumer consumer = ctx.getBean(NON_TRANSACTIONAL_CONSUMER, IdempotentConsumer.class);
                    UUID id = UUID.randomUUID();
                    AtomicInteger runs = new AtomicInteger();

                    transactionTemplate(ctx).executeWithoutResult(status -> {
                        consumer.consume(id, runs::incrementAndGet);
                        status.setRollbackOnly();
                    });
                    consumer.consume(id, runs::incrementAndGet);

                    assertThat(runs).hasValue(2);
                });
    }

    private static TransactionTemplate transactionTemplate(ApplicationContext ctx) {
        return new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class));
    }
}
