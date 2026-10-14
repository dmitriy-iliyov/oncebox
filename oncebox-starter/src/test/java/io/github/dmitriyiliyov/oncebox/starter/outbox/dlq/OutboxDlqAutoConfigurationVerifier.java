package io.github.dmitriyiliyov.oncebox.starter.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.*;
import io.github.dmitriyiliyov.oncebox.dlq.api.*;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq.OutboxDlqManagerMetricsDecorator;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq.OutboxDlqMetrics;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq.OutboxDlqMetricsRepository;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq.OutboxDlqMetricsService;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxAutoConfiguration;
import io.github.dmitriyiliyov.oncebox.starter.outbox.OutboxAutoConfiguration;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OutboxDlqAutoConfigurationVerifier {

    private final String dbUrl;
    private final String dbDriver;
    private final String dbUsername;
    private final String dbPassword;

    public OutboxDlqAutoConfigurationVerifier(String dbUrl, String dbDriver, String dbUsername, String dbPassword) {
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
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword,
                        "oncebox.tables.auto-create=true",
                        "oncebox.outbox.enabled=true",
                        "oncebox.outbox.sender.type=kafka",
                        "oncebox.outbox.events.my-event.topic=my.topic",
                        "oncebox.outbox.dlq.enabled=true"
                );
    }

    public void shouldNotLoad_whenDisabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.outbox.dlq.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(OutboxDlqManager.class);
                    assertThat(ctx).doesNotHaveBean(OutboxDlqRepository.class);
                    assertThat(ctx).doesNotHaveBean(OutboxDlqTransfer.class);
                });
    }

    public void shouldRegisterOutboxDlqRepository() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxDlqRepository.class)
        );
    }

    public void shouldRegisterOutboxDlqManager() {
        getBaseContextRunner().run(ctx -> {
            assertThat(ctx).hasSingleBean(OutboxDlqManager.class);
            assertThat(ctx).doesNotHaveBean(OutboxDlqManagerMetricsDecorator.class);
            assertThat(ctx).doesNotHaveBean(OutboxDlqMetricsService.class);
            assertThat(ctx).doesNotHaveBean(OutboxDlqMetricsRepository.class);
            assertThat(ctx).doesNotHaveBean(OutboxDlqMetrics.class);
        });
    }

    public void shouldRegisterDefaultOutboxDlqHandler() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxDlqHandler.class)
        );
    }

    public void shouldRegisterDefaultOutboxDlqEventMapper() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(DefaultOutboxDlqEventMapper.class)
        );
    }

    public void shouldRegisterOutboxDlqTransfer() {
        getBaseContextRunner().run(ctx -> {
            assertThat(ctx).hasSingleBean(OutboxDlqTransfer.class);
        });
    }

    public void shouldRegisterOutboxDlqScheduler() {
        getBaseContextRunner().run(ctx -> {
            assertThat(ctx).hasBean("outboxDlqTransferToScheduler");
            assertThat(ctx).hasBean("outboxDlqTransferFromScheduler");
        });
    }

    public void shouldRegisteredMetricsRelatedBeans_whenAllMetricsEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.outbox.metrics.enabled=true",
                        "oncebox.outbox.metrics.gauge.enabled=true"
                )
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxDlqManagerMetricsDecorator.class);
                    assertThat(ctx).hasSingleBean(OutboxDlqMetricsService.class);
                    assertThat(ctx).hasSingleBean(OutboxDlqMetricsRepository.class);
                    assertThat(ctx).hasSingleBean(OutboxDlqMetrics.class);
                });
    }

    public void shouldRegisteredMetricsRelatedBeans_whenGaugeUnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.outbox.metrics.enabled=true",
                        "oncebox.outbox.metrics.gauge.enabled=false"
                )
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxDlqManagerMetricsDecorator.class);
                    assertThat(ctx).doesNotHaveBean(OutboxDlqMetricsService.class);
                    assertThat(ctx).doesNotHaveBean(OutboxDlqMetricsRepository.class);
                    assertThat(ctx).doesNotHaveBean(OutboxDlqMetrics.class);
                });
    }

    public void shouldRegisteredMetricsRelatedBeans_whenGaugeEnabledMissed() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.outbox.metrics.enabled=true"
                )
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxDlqManagerMetricsDecorator.class);
                    assertThat(ctx).doesNotHaveBean(OutboxDlqMetricsService.class);
                    assertThat(ctx).doesNotHaveBean(OutboxDlqMetricsRepository.class);
                    assertThat(ctx).doesNotHaveBean(OutboxDlqMetrics.class);
                });
    }

    public void shouldNotRegisterDuplicateHandler_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxDlqHandler", OutboxDlqHandler.class, () -> event -> {})
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxDlqHandler.class);
                    assertThat(ctx).hasBean("customOutboxDlqHandler");
                });
    }

    public void shouldNotRegisterDuplicateDlqManager_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("outboxDlqManager", OutboxDlqManager.class, () -> org.mockito.Mockito.mock(OutboxDlqManager.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxDlqManager.class);
                    assertThat(ctx).hasBean("outboxDlqManager");
                });
    }

    public void shouldCreateTables_whenAutoCreateTrue() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    JdbcTemplate jdbcTemplate = ctx.getBean("onceboxJdbcTemplate", JdbcTemplate.class);
                    jdbcTemplate.execute("SELECT 1 FROM outbox_dlq_events WHERE 1=0");
                    jdbcTemplate.execute("SELECT 1 FROM oncebox_jobs WHERE 1=0");
                });
    }

    public void shouldNotRegisterDuplicateDlqTransfer_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxDlqTransfer", OutboxDlqTransfer.class, () -> org.mockito.Mockito.mock(OutboxDlqTransfer.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxDlqTransfer.class);
                    assertThat(ctx).hasBean("outboxDlqTransferToScheduler");
                    assertThat(ctx).hasBean("outboxDlqTransferFromScheduler");
                    assertThat(ctx).doesNotHaveBean(DefaultOutboxDlqTransfer.class);
                });
    }

    public void shouldNotRegisterDuplicateDlqRepository_whenCustomBeanProvided() {
        getBaseContextRunner()
                .withBean("customOutboxDlqRepository", OutboxDlqRepository.class, () -> org.mockito.Mockito.mock(OutboxDlqRepository.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OutboxDlqRepository.class);
                    assertThat(ctx).hasBean("customOutboxDlqRepository");
                });
    }

    public void shouldRegisterDlqManagerDecorator_asPrimary_whenMetricsEnabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.outbox.metrics.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasBean("outboxDlqManager");
                    assertThat(ctx).hasBean("outboxDlqManagerMetricsDecorator");
                    OutboxDlqManager primary = ctx.getBean(OutboxDlqManager.class);
                    assertThat(primary).isInstanceOf(OutboxDlqManagerMetricsDecorator.class);
                });
    }

    public void shouldLoadMinimalConfiguration_whenOnlyRequiredPropertiesSet() {
        getBaseContextRunner()
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(OutboxDlqRepository.class);
                    assertThat(ctx).hasSingleBean(OutboxDlqHandler.class);
                    assertThat(ctx).hasBean("outboxDlqTransferToScheduler");
                    assertThat(ctx).hasBean("outboxDlqTransferFromScheduler");

                    assertThat(ctx).hasSingleBean(OutboxDlqManager.class);
                    OutboxDlqManager primary = ctx.getBean(OutboxDlqManager.class);
                    assertThat(primary).isInstanceOf(DefaultOutboxDlqManager.class);

                    assertThat(ctx).hasSingleBean(OutboxDlqTransfer.class);
                    OutboxDlqTransfer transfer = ctx.getBean(OutboxDlqTransfer.class);
                    assertThat(transfer).isInstanceOf(DefaultOutboxDlqTransfer.class);

                    assertThat(ctx).hasSingleBean(OutboxDlqController.class);
                    assertThat(ctx).hasSingleBean(DlqStatusQueryConverter.class);
                    assertThat(ctx).hasSingleBean(OutboxDlqControllerAdvice.class);
                    assertThat(ctx).hasSingleBean(OutboxDlqApiService.class);
                    assertThat(ctx).hasSingleBean(OutboxDlqApiRepository.class);

                    assertThat(ctx).doesNotHaveBean(OutboxDlqManagerMetricsDecorator.class);
                    assertThat(ctx).doesNotHaveBean(OutboxDlqMetricsService.class);
                    assertThat(ctx).doesNotHaveBean(OutboxDlqMetricsRepository.class);
                    assertThat(ctx).doesNotHaveBean(OutboxDlqMetrics.class);
                });
    }

    public void shouldLoadFullConfiguration_whenAllFeaturesEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.outbox.metrics.enabled=true",
                        "oncebox.outbox.metrics.gauge.enabled=true",
                        "oncebox.outbox.metrics.gauge.cache.enabled=true",
                        "oncebox.outbox.metrics.gauge.cache.ttls[0]=PT10S",
                        "oncebox.outbox.metrics.gauge.cache.ttls[1]=PT30S",
                        "oncebox.outbox.metrics.gauge.cache.ttls[2]=PT60S"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();

                    assertThat(ctx).hasSingleBean(OutboxDlqRepository.class);
                    assertThat(ctx).hasSingleBean(OutboxDlqHandler.class);
                    assertThat(ctx).hasBean("outboxDlqTransferToScheduler");
                    assertThat(ctx).hasBean("outboxDlqTransferFromScheduler");

                    assertThat(ctx).hasBean("outboxDlqManager");
                    assertThat(ctx).hasBean("outboxDlqManagerMetricsDecorator");
                    assertThat(ctx).hasBean("outboxDlqApiServiceMetricsDecorator");

                    OutboxDlqManager primaryManager = ctx.getBean(OutboxDlqManager.class);
                    assertThat(primaryManager).isInstanceOf(OutboxDlqManagerMetricsDecorator.class);

                    assertThat(ctx).hasBean("outboxDlqTransfer");

                    assertThat(ctx).hasSingleBean(OutboxDlqMetricsService.class);
                    assertThat(ctx).hasSingleBean(OutboxDlqMetricsRepository.class);
                    assertThat(ctx).hasSingleBean(OutboxDlqMetrics.class);
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

                    OutboxDlqManager primaryManager = ctx.getBean(OutboxDlqManager.class);
                    assertThat(primaryManager).isInstanceOf(OutboxDlqManagerMetricsDecorator.class);

                    assertThat(ctx).hasBean("outboxDlqApiServiceMetricsDecorator");
                    assertThat(ctx).hasSingleBean(OutboxDlqTransfer.class);

                    assertThat(ctx).doesNotHaveBean(OutboxDlqMetricsService.class);
                    assertThat(ctx).doesNotHaveBean(OutboxDlqMetricsRepository.class);
                    assertThat(ctx).doesNotHaveBean(OutboxDlqMetrics.class);
                });
    }


    public void loadAndLockBatch_whenNoActiveTransaction_shouldThrowIllegalTransactionStateException() {
        getBaseContextRunner()
                .run(ctx -> {
                    OutboxDlqManager manager = ctx.getBean(OutboxDlqManager.class);

                    assertThatThrownBy(() -> manager.loadAndLockBatch(DlqStatus.TO_RETRY, 10))
                            .isInstanceOf(IllegalTransactionStateException.class);
                });
    }


    public void loadAndLockBatch_whenInsideTransaction_shouldRun() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=true")
                .run(ctx -> {
                    OutboxDlqManager manager = ctx.getBean(OutboxDlqManager.class);

                    transactionTemplate(ctx).executeWithoutResult(status -> {
                        assertThat(manager.loadAndLockBatch(DlqStatus.TO_RETRY, 10)).isNotNull();
                        status.setRollbackOnly();
                    });
                });
    }

    private static TransactionTemplate transactionTemplate(ApplicationContext ctx) {
        return new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class));
    }
}
