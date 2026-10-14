package io.github.dmitriyiliyov.oncebox.starter.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.aop.OutboxPublishAspect;
import io.github.dmitriyiliyov.oncebox.core.outbox.*;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.OutboxManagerMetricsDecorator;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.OutboxMetrics;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.OutboxMetricsRepository;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.OutboxMetricsService;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.NoopOutboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.OutboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxAutoConfiguration;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxJobCreateCommand;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OutboxAutoConfigurationVerifier {

    private final String dbUrl;
    private final String dbDriver;
    private final String dbUsername;
    private final String dbPassword;

    public OutboxAutoConfigurationVerifier(String dbUrl, String dbDriver, String dbUsername, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbDriver = dbDriver;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    private ApplicationContextRunner getBaseContextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class,
                        TransactionAutoConfiguration.class,
                        JacksonAutoConfiguration.class,
                        OnceboxAutoConfiguration.class,
                        OutboxAutoConfiguration.class,
                        KafkaAutoConfiguration.class
                ))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword,
                        "oncebox.tables.auto-create=false",
                        "oncebox.outbox.sender.type=kafka",
                        "oncebox.outbox.sender.bean-name=kafkaTemplate",
                        "oncebox.outbox.events.my-event.topic=my.topic"
                );
    }

    public void shouldLoadMinimalConfiguration_whenOnlyRequiredPropertiesSet() {
        getBaseContextRunner()
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(OutboxRepository.class);
                    assertThat(ctx).hasSingleBean(OutboxManager.class);
                    assertThat(ctx).hasSingleBean(OutboxProcessor.class);
                    assertThat(ctx).hasSingleBean(OutboxSender.class);
                    assertThat(ctx).hasSingleBean(OutboxSerializer.class);
                    assertThat(ctx).hasSingleBean(OutboxPublisher.class);
                    assertThat(ctx).hasSingleBean(UuidGenerator.class);
                    assertThat(ctx).hasSingleBean(OutboxPublishAspect.class);

                    assertThat(ctx).hasBean("outboxRecoveryScheduler");
                    assertThat(ctx).hasBean("myeventOutboxPollingScheduler");
                    assertThat(ctx).hasBean("outboxCleanUpScheduler");
                    assertThat(ctx).hasBean("outboxCleanUpJobCreateCommand");

                    assertThat(ctx).doesNotHaveBean("outboxCache");
                    assertThat(ctx).doesNotHaveBean(OutboxManagerMetricsDecorator.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsService.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsRepository.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetrics.class);
                });
    }

    public void shouldLoadFullConfiguration_whenAllFeaturesEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.outbox.metrics.enabled=true",
                        "oncebox.outbox.metrics.gauge.enabled=true",
                        "oncebox.outbox.metrics.gauge.cache.ttls[0]=PT10S",
                        "oncebox.outbox.metrics.gauge.cache.ttls[1]=PT30S",
                        "oncebox.outbox.metrics.gauge.cache.ttls[2]=PT60S",
                        "oncebox.outbox.clean-up.enabled=true",
                        "oncebox.outbox.clean-up.interval=PT1M",
                        "oncebox.outbox.clean-up.retention=PT24H",
                        "oncebox.outbox.events.my-event.topic=my.topic",
                        "oncebox.outbox.events.other-event.topic=other.topic"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(OutboxRepository.class);
                    assertThat(ctx).hasSingleBean(OutboxProcessor.class);
                    assertThat(ctx).hasSingleBean(OutboxSender.class);
                    assertThat(ctx).hasSingleBean(OutboxSerializer.class);
                    assertThat(ctx).hasSingleBean(OutboxPublisher.class);
                    assertThat(ctx).hasSingleBean(UuidGenerator.class);
                    assertThat(ctx).hasSingleBean(OutboxPublishAspect.class);
                    assertThat(ctx).hasSingleBean(OutboxCache.class);
                    assertThat(ctx).hasSingleBean(OnceboxJobCreateCommand.class);

                    assertThat(ctx.getBean(OutboxCache.class)).isInstanceOf(SimpleOutboxCache.class);

                    assertThat(ctx).hasBean("outboxManager");
                    assertThat(ctx).hasBean("outboxManagerMetricsDecorator");
                    OutboxManager primary = ctx.getBean(OutboxManager.class);
                    assertThat(primary).isInstanceOf(OutboxManagerMetricsDecorator.class);

                    assertThat(ctx).hasSingleBean(OutboxManagerMetricsDecorator.class);
                    assertThat(ctx).hasSingleBean(OutboxMetricsService.class);
                    assertThat(ctx).hasSingleBean(OutboxMetricsRepository.class);
                    assertThat(ctx).hasSingleBean(OutboxMetrics.class);

                    assertThat(ctx).hasBean("outboxRecoveryScheduler");
                    assertThat(ctx).hasBean("outboxCleanUpScheduler");
                    assertThat(ctx).hasBean("outboxCleanUpJobCreateCommand");
                    assertThat(ctx).hasBean("myeventOutboxPollingScheduler");
                    assertThat(ctx).hasBean("othereventOutboxPollingScheduler");
                });
    }

    public void shouldLoadConfiguration_whenMetricsEnabledButGaugeDisabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.outbox.metrics.enabled=true",
                        "oncebox.outbox.metrics.gauge.enabled=false"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    OutboxManager primary = ctx.getBean(OutboxManager.class);
                    assertThat(primary).isInstanceOf(OutboxManagerMetricsDecorator.class);

                    assertThat(ctx).hasSingleBean(OutboxCache.class);
                    assertThat(ctx.getBean(OutboxCache.class)).isInstanceOf(NoopOutboxCache.class);

                    assertThat(ctx).doesNotHaveBean(OutboxMetricsService.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsRepository.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetrics.class);
                });
    }

    public void shouldLoadConfiguration_whenCleanUpEnabledButMetricsDisabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.outbox.clean-up.enabled=true",
                        "oncebox.outbox.clean-up.interval=PT1M",
                        "oncebox.outbox.clean-up.retention=PT24H"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasBean("outboxCleanUpScheduler");
                    assertThat(ctx).hasBean("outboxCleanUpJobCreateCommand");
                    assertThat(ctx).hasSingleBean(OnceboxJobCreateCommand.class);

                    assertThat(ctx).doesNotHaveBean(OutboxManagerMetricsDecorator.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsService.class);

                    OutboxManager primary = ctx.getBean(OutboxManager.class);
                    assertThat(primary).isInstanceOf(DefaultOutboxManager.class);
                });
    }

    public void shouldNotLoad_whenDisabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.outbox.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(OutboxProcessor.class);
                    assertThat(ctx).doesNotHaveBean(OutboxManager.class);
                    assertThat(ctx).doesNotHaveBean(OutboxSender.class);
                    assertThat(ctx).doesNotHaveBean(OutboxManagerMetricsDecorator.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsService.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsRepository.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetrics.class);
                    assertThat(ctx).doesNotHaveBean(OutboxCache.class);
                    assertThat(ctx).doesNotHaveBean(OnceboxJobCreateCommand.class);
                });
    }

    public void shouldRegisterOutboxRepository() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxRepository.class)
        );
    }

    public void shouldRegisterOutboxManager() {
        getBaseContextRunner().run(ctx -> {
            assertThat(ctx).hasSingleBean(OutboxManager.class);
            assertThat(ctx).doesNotHaveBean(OutboxManagerMetricsDecorator.class);
            assertThat(ctx).doesNotHaveBean(OutboxMetricsService.class);
            assertThat(ctx).doesNotHaveBean(OutboxMetricsRepository.class);
            assertThat(ctx).doesNotHaveBean(OutboxMetrics.class);
        });
    }

    public void shouldRegisterOutboxProcessor() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxProcessor.class)
        );
    }

    public void shouldRegisterOutboxSerializer() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxSerializer.class)
        );
    }

    public void shouldRegisterOutboxOutbox() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxPublisher.class)
        );
    }

    public void shouldRegisterUuidGenerator() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(UuidGenerator.class)
        );
    }

    public void shouldRegisterOutboxPublishAspect() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxPublishAspect.class)
        );
    }

    public void shouldRegisteredMetricsRelatedBeans_whenAllMetricsEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.outbox.metrics.enabled=true",
                        "oncebox.outbox.metrics.gauge.enabled=true",
                        "oncebox.outbox.metrics.gauge.cache.ttls[0]=PT1S",
                        "oncebox.outbox.metrics.gauge.cache.ttls[1]=PT2S",
                        "oncebox.outbox.metrics.gauge.cache.ttls[2]=PT3S"
                )
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxManagerMetricsDecorator.class);
                    assertThat(ctx).hasSingleBean(OutboxMetricsService.class);
                    assertThat(ctx).hasSingleBean(OutboxMetricsRepository.class);
                    assertThat(ctx).hasSingleBean(OutboxMetrics.class);
                    assertThat(ctx).hasSingleBean(OutboxCache.class);
                    assertThat(ctx.getBean(OutboxCache.class)).isInstanceOf(SimpleOutboxCache.class);
                });
    }

    public void shouldRegisteredMetricsRelatedBeans_whenGaugeUnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.outbox.metrics.enabled=true",
                        "oncebox.outbox.metrics.gauge.enabled=false"
                )
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxManagerMetricsDecorator.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsService.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsRepository.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetrics.class);
                    assertThat(ctx).hasSingleBean(OutboxCache.class);
                    assertThat(ctx.getBean(OutboxCache.class)).isInstanceOf(NoopOutboxCache.class);
                });
    }

    public void shouldRegisteredMetricsRelatedBeans_whenGaugeEnabledMissed() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.outbox.metrics.enabled=true"
                )
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxManagerMetricsDecorator.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsService.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetricsRepository.class);
                    assertThat(ctx).doesNotHaveBean(OutboxMetrics.class);
                    assertThat(ctx).hasSingleBean(OutboxCache.class);
                    assertThat(ctx.getBean(OutboxCache.class)).isInstanceOf(NoopOutboxCache.class);
                });
    }

    public void shouldNotRegisterOutboxRepository_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxRepository", OutboxRepository.class, () -> org.mockito.Mockito.mock(OutboxRepository.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxRepository.class);
                    assertThat(ctx).hasBean("customOutboxRepository");
                });
    }

    public void shouldNotRegisterOutboxProcessor_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxProcessor", OutboxProcessor.class, () -> org.mockito.Mockito.mock(OutboxProcessor.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxProcessor.class);
                    assertThat(ctx).hasBean("customOutboxProcessor");
                });
    }

    public void shouldNotRegisterOutboxManager_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("outboxManager", OutboxManager.class, () -> org.mockito.Mockito.mock(OutboxManager.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxManager.class);
                    assertThat(ctx).hasBean("outboxManager");
                    assertThat(ctx).doesNotHaveBean(DefaultOutboxManager.class);
                });
    }

    public void shouldNotRegisterOutboxSender_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxSender", OutboxSender.class, () -> org.mockito.Mockito.mock(OutboxSender.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxSender.class);
                    assertThat(ctx).hasBean("customOutboxSender");
                });
    }

    public void shouldNotRegisterOutboxSerializer_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxSerializer", OutboxSerializer.class, () -> org.mockito.Mockito.mock(OutboxSerializer.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxSerializer.class);
                    assertThat(ctx).hasBean("customOutboxSerializer");
                });
    }

    public void shouldNotRegisterUuidGenerator_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customUuidGenerator", UuidGenerator.class, () -> org.mockito.Mockito.mock(UuidGenerator.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(UuidGenerator.class);
                    assertThat(ctx).hasBean("customUuidGenerator");
                });
    }

    public void shouldNotRegisterOutboxCache_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("outboxCache", OutboxCache.class, () -> org.mockito.Mockito.mock(OutboxCache.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxCache.class);
                    assertThat(ctx).hasBean("outboxCache");
                });
    }

    public void shouldRegisterOutboxManager_whenMetricsEnabled_hasDecoratorAsPrimary() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.outbox.metrics.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasBean("outboxManager");
                    assertThat(ctx).hasBean("outboxManagerMetricsDecorator");
                    OutboxManager primary = ctx.getBean(OutboxManager.class);
                    assertThat(primary).isInstanceOf(OutboxManagerMetricsDecorator.class);
                });
    }

    public void shouldRegisterPublisherScheduler_perEventType() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.outbox.events.my-event.topic=my.topic",
                        "oncebox.outbox.events.other-event.topic=other.topic"
                )
                .run(ctx -> {
                    assertThat(ctx).hasBean("myeventOutboxPollingScheduler");
                    assertThat(ctx).hasBean("othereventOutboxPollingScheduler");
                });
    }

    public void shouldRegisterRecoveryScheduler() {
        getBaseContextRunner()
                .run(ctx ->
                        assertThat(ctx).hasBean("outboxRecoveryScheduler")
                );
    }

    public void shouldRegisterCleanUpScheduler_whenEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.outbox.clean-up.enabled=true",
                        "oncebox.outbox.clean-up.interval=PT1M",
                        "oncebox.outbox.clean-up.retention=PT24H"
                )
                .run(ctx -> {
                    assertThat(ctx).hasBean("outboxCleanUpScheduler");
                    assertThat(ctx).hasBean("outboxCleanUpJobCreateCommand");
                });
    }

    public void shouldNotRegisterCleanUpScheduler_whenDisabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.outbox.clean-up.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean("outboxCleanUpScheduler");
                    assertThat(ctx).doesNotHaveBean("outboxCleanUpJobCreateCommand");
                });
    }

    public void shouldRegisterCleanUpJobCreateCommand_whenEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.outbox.clean-up.enabled=true"
                )
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(OnceboxJobCreateCommand.class)
                );
    }

    public void shouldNotRegisterCleanUpJobCreateCommand_whenDisabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.outbox.clean-up.enabled=false"
                )
                .run(ctx ->
                        assertThat(ctx).doesNotHaveBean(OnceboxJobCreateCommand.class)
                );
    }

    public void shouldRegisterPublisherScheduler_withMetricsDecoratorAsManager() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.outbox.metrics.enabled=true",
                        "oncebox.outbox.events.my-event.topic=my.topic"
                )
                .run(ctx -> {
                    assertThat(ctx).hasBean("myeventOutboxPollingScheduler");
                    assertThat(ctx).hasBean("outboxRecoveryScheduler");
                    OutboxManager primary = ctx.getBean(OutboxManager.class);
                    assertThat(primary).isInstanceOf(OutboxManagerMetricsDecorator.class);
                });
    }

    public void loadBatchByStatus_whenNoActiveTransaction_shouldThrowIllegalTransactionStateException() {
        getBaseContextRunner()
                .run(ctx -> {
                    OutboxManager manager = ctx.getBean(OutboxManager.class);

                    assertThatThrownBy(() -> manager.loadBatch(EventStatus.FAILED, 10))
                            .isInstanceOf(IllegalTransactionStateException.class);
                });
    }


    public void loadBatchByStatus_whenInsideTransaction_shouldRun() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=true")
                .run(ctx -> {
                    OutboxManager manager = ctx.getBean(OutboxManager.class);

                    transactionTemplate(ctx).executeWithoutResult(status -> {
                        assertThat(manager.loadBatch(EventStatus.FAILED, 10)).isNotNull();
                        status.setRollbackOnly();
                    });
                });
    }

    private static TransactionTemplate transactionTemplate(ApplicationContext ctx) {
        return new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class));
    }
}
