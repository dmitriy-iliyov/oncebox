package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.starter.inbox.InboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.outbox.OutboxProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class OnceboxAutoConfigurationVerifier {

    private final String dbUrl;
    private final String dbDriver;
    private final String dbUsername;
    private final String dbPassword;

    public OnceboxAutoConfigurationVerifier(String dbUrl, String dbDriver, String dbUsername, String dbPassword) {
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
                        OnceboxAutoConfiguration.class
                ))
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword
                );
    }

    public void shouldRegisterOutboxPropertiesByDefault() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).hasSingleBean(OutboxProperties.class)
        );
    }

    public void shouldRegisterOutboxPropertiesWhenEnabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.outbox.enabled=true",
                        "oncebox.outbox.sender.type=kafka",
                        "oncebox.outbox.events.my-event.topic=my.topic")
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(OutboxProperties.class)
                );
    }

    public void shouldNotRegisterOutboxPropertiesWhenDisabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.outbox.enabled=false")
                .run(ctx ->
                        assertThat(ctx).doesNotHaveBean(OutboxProperties.class)
                );
    }

    public void shouldNotRegisterInboxPropertiesByDefault() {
        getBaseContextRunner().run(ctx ->
                assertThat(ctx).doesNotHaveBean(InboxProperties.class)
        );
    }

    public void shouldRegisterInboxPropertiesWhenEnabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.inbox.enabled=true", "oncebox.inbox.source.type=kafka")
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(InboxProperties.class)
                );
    }

    public void shouldNotRegisterInboxPropertiesWhenDisabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.inbox.enabled=false")
                .run(ctx ->
                        assertThat(ctx).doesNotHaveBean(InboxProperties.class)
                );
    }

    public void shouldRegisterJdbcTemplate() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=false")
                .run(ctx ->
                        assertThat(ctx).hasBean("onceboxJdbcTemplate")
                );
    }

    public void shouldNotRegisterJdbcTemplateWhenCustomBeanProvided() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=false")
                .withBean("onceboxJdbcTemplate", JdbcTemplate.class, () -> mock(JdbcTemplate.class))
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(JdbcTemplate.class)
                );
    }

    public void shouldRegisterDataSourceInitializerByDefault() {
        getBaseContextRunner()
                .run(ctx ->
                        assertThat(ctx).hasBean("onceboxDataSourceInitializer")
                );
    }

    public void shouldRegisterDataSourceInitializerWhenAutoCreateTrue() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasBean("onceboxDataSourceInitializer");
                    JdbcTemplate jdbcTemplate = ctx.getBean("onceboxJdbcTemplate", JdbcTemplate.class);
                    jdbcTemplate.execute("SELECT 1 FROM outbox_events WHERE 1=0");
                    jdbcTemplate.execute("SELECT 1 FROM oncebox_jobs WHERE 1=0");
                });
    }

    public void shouldNotRegisterDataSourceInitializerWhenAutoCreateFalse() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=false")
                .run(ctx ->
                        assertThat(ctx).doesNotHaveBean("onceboxDataSourceInitializer")
                );
    }

    public void shouldRegisterDistributedLockRepositoryWhenCleanUpEnabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=false",
                        "oncebox.outbox.clean-up.enabled=true",
                        "oncebox.outbox.sender.type=kafka",
                        "oncebox.outbox.events.my-event.topic=my.topic"
                )
                .withBean(OnceboxRepositoryFactory.class, () -> mock(OnceboxRepositoryFactory.class))
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(DistributedLockRepository.class)
                );
    }

    public void shouldNotRegisterDistributedLockRepositoryWhenCleanUpDisabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.tables.auto-create=false",
                        "oncebox.outbox.clean-up.enabled=false",
                        "oncebox.outbox.sender.type=kafka",
                        "oncebox.outbox.events.my-event.topic=my.topic"
                )
                .run(ctx ->
                        assertThat(ctx).doesNotHaveBean(DistributedLockRepository.class)
                );
    }

    public void shouldNotRegisterDistributedLockRepositoryWhenCustomBeanProvided() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.tables.auto-create=false",
                        "oncebox.outbox.clean-up.enabled=true",
                        "oncebox.outbox.sender.type=kafka",
                        "oncebox.outbox.events.my-event.topic=my.topic")
                .withBean(OnceboxRepositoryFactory.class, () -> mock(OnceboxRepositoryFactory.class))
                .withBean(DistributedLockRepository.class, () -> mock(DistributedLockRepository.class))
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(DistributedLockRepository.class)
                );
    }

    public void shouldRegisterNoopScheduleStrategyListenerSupplierByDefault() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=false")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OnceboxScheduleStrategyListenerSupplier.class);
                    assertThat(ctx.getBean(OnceboxScheduleStrategyListenerSupplier.class))
                            .isInstanceOf(NoopOnceboxScheduleStrategyListenerSupplier.class);
                });
    }

    public void shouldRegisterMetricsScheduleStrategyListenerSupplierWhenMetricsEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.tables.auto-create=false",
                        "oncebox.outbox.metrics.enabled=true",
                        "oncebox.outbox.sender.type=kafka",
                        "oncebox.outbox.events.my-event.topic=my.topic"
                )
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(ctx -> {
                    assertThat(ctx).hasBean("metricsOnceboxScheduleStrategyListenerSupplier");
                    OnceboxScheduleStrategyListenerSupplier primary = ctx.getBean(OnceboxScheduleStrategyListenerSupplier.class);
                    assertThat(primary).isInstanceOf(MetricsOnceboxScheduleStrategyListenerSupplier.class);
                });
    }

    public void shouldNotRegisterScheduleStrategyListenerSupplierWhenCustomBeanProvided() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=false")
                .withBean("customScheduleStrategyListenerSupplier", OnceboxScheduleStrategyListenerSupplier.class, () -> mock(OnceboxScheduleStrategyListenerSupplier.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OnceboxScheduleStrategyListenerSupplier.class);
                    assertThat(ctx).hasBean("customScheduleStrategyListenerSupplier");
                });
    }

    public void shouldRegisterNoopContinuableTaskDecoratorSupplierByDefault() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=false")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ContinuableTaskDecoratorSupplier.class);
                    assertThat(ctx.getBean(ContinuableTaskDecoratorSupplier.class))
                            .isInstanceOf(NoopContinuableTaskDecoratorSupplier.class);
                });
    }

    public void shouldRegisterMetricsContinuableTaskDecoratorSupplierWhenMetricsEnabled() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.tables.auto-create=false",
                        "oncebox.outbox.metrics.enabled=true",
                        "oncebox.outbox.sender.type=kafka",
                        "oncebox.outbox.events.my-event.topic=my.topic"
                )
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues(
                        )
                .run(ctx -> {
                    assertThat(ctx).hasBean("metricsOnceboxContinuableTaskDecoratorSupplier");
                    ContinuableTaskDecoratorSupplier primary = ctx.getBean(ContinuableTaskDecoratorSupplier.class);
                    assertThat(primary).isInstanceOf(ContinuableTaskTimeMeasureDecoratorSupplier.class);
                });
    }

    public void shouldNotRegisterContinuableTaskDecoratorSupplierWhenCustomBeanProvided() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=false")
                .withBean("customContinuableTaskDecoratorSupplier", ContinuableTaskDecoratorSupplier.class, () -> mock(ContinuableTaskDecoratorSupplier.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ContinuableTaskDecoratorSupplier.class);
                    assertThat(ctx).hasBean("customContinuableTaskDecoratorSupplier");
                });
    }

    public void shouldRegisterScheduledExecutorServiceByDefault() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=false")
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(ScheduledExecutorService.class)
                );
    }

    public void shouldRegisterScheduledExecutorServiceWithCustomPoolSize() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=false", "oncebox.thread-pool-size=5")
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(ScheduledExecutorService.class)
                );
    }

    public void shouldRegisterApplicationRunnerForJobs() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=false")
                .run(ctx ->
                        assertThat(ctx).hasBean("onceboxJobsInitializer")
                );
    }

    public void shouldRegisterOnceboxInitializerWhenMissing() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.tables.auto-create=false")
                .run(ctx ->
                        assertThat(ctx).hasSingleBean(SchedulersPostApplicationReadyOnceboxInitializer.class)
                );
    }

    public void shouldFailWhenDataSourceNotAvailable() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OnceboxAutoConfiguration.class))
                .run(ctx ->
                        assertThat(ctx).hasFailed()
                );
    }
}