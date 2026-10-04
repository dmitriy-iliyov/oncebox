package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.tests.fixtures.PostgresTestContainerSingleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostgreSqlOnceboxAutoConfigurationIntegrationTests {

    private final OnceboxAutoConfigurationVerifier verifier =
            new OnceboxAutoConfigurationVerifier(
                    PostgresTestContainerSingleton.INSTANCE.getJdbcUrl(),
                    "org.postgresql.Driver",
                    PostgresTestContainerSingleton.INSTANCE.getUsername(),
                    PostgresTestContainerSingleton.INSTANCE.getPassword()
            );

    @Test
    @DisplayName("IT should register OutboxProperties by default")
    void shouldRegisterOutboxPropertiesByDefault() {
        verifier.shouldRegisterOutboxPropertiesByDefault();
    }

    @Test
    @DisplayName("IT should register OutboxProperties when enabled")
    void shouldRegisterOutboxPropertiesWhenEnabled() {
        verifier.shouldRegisterOutboxPropertiesWhenEnabled();
    }

    @Test
    @DisplayName("IT should not register OutboxProperties when disabled")
    void shouldNotRegisterOutboxPropertiesWhenDisabled() {
        verifier.shouldNotRegisterOutboxPropertiesWhenDisabled();
    }

    @Test
    @DisplayName("IT should not register InboxProperties by default")
    void shouldNotRegisterInboxPropertiesByDefault() {
        verifier.shouldNotRegisterInboxPropertiesByDefault();
    }

    @Test
    @DisplayName("IT should register InboxProperties when enabled")
    void shouldRegisterInboxPropertiesWhenEnabled() {
        verifier.shouldRegisterInboxPropertiesWhenEnabled();
    }

    @Test
    @DisplayName("IT should not register InboxProperties when disabled")
    void shouldNotRegisterInboxPropertiesWhenDisabled() {
        verifier.shouldNotRegisterInboxPropertiesWhenDisabled();
    }

    @Test
    @DisplayName("IT should register JdbcTemplate")
    void shouldRegisterJdbcTemplate() {
        verifier.shouldRegisterJdbcTemplate();
    }

    @Test
    @DisplayName("IT should not register JdbcTemplate when custom bean provided")
    void shouldNotRegisterJdbcTemplateWhenCustomBeanProvided() {
        verifier.shouldNotRegisterJdbcTemplateWhenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should register DataSourceInitializer by default")
    void shouldRegisterDataSourceInitializerByDefault() {
        verifier.shouldRegisterDataSourceInitializerByDefault();
    }

    @Test
    @DisplayName("IT should register DataSourceInitializer when auto-create true")
    void shouldRegisterDataSourceInitializerWhenAutoCreateTrue() {
        verifier.shouldRegisterDataSourceInitializerWhenAutoCreateTrue();
    }

    @Test
    @DisplayName("IT should not register DataSourceInitializer when auto-create false")
    void shouldNotRegisterDataSourceInitializerWhenAutoCreateFalse() {
        verifier.shouldNotRegisterDataSourceInitializerWhenAutoCreateFalse();
    }

    @Test
    @DisplayName("IT should register DistributedLockRepository when clean-up enabled")
    void shouldRegisterDistributedLockRepositoryWhenCleanUpEnabled() {
        verifier.shouldRegisterDistributedLockRepositoryWhenCleanUpEnabled();
    }

    @Test
    @DisplayName("IT should not register DistributedLockRepository when clean-up disabled")
    void shouldNotRegisterDistributedLockRepositoryWhenCleanUpDisabled() {
        verifier.shouldNotRegisterDistributedLockRepositoryWhenCleanUpDisabled();
    }

    @Test
    @DisplayName("IT should not register duplicate DistributedLockRepository when custom bean provided")
    void shouldNotRegisterDistributedLockRepositoryWhenCustomBeanProvided() {
        verifier.shouldNotRegisterDistributedLockRepositoryWhenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should register NoopScheduleStrategyListenerSupplier by default")
    void shouldRegisterNoopScheduleStrategyListenerSupplierByDefault() {
        verifier.shouldRegisterNoopScheduleStrategyListenerSupplierByDefault();
    }

    @Test
    @DisplayName("IT should register MetricsOnceboxScheduleStrategyListenerSupplier when metrics enabled")
    void shouldRegisterMetricsScheduleStrategyListenerSupplierWhenMetricsEnabled() {
        verifier.shouldRegisterMetricsScheduleStrategyListenerSupplierWhenMetricsEnabled();
    }

    @Test
    @DisplayName("IT should not register duplicate ScheduleStrategyListenerSupplier when custom bean provided")
    void shouldNotRegisterScheduleStrategyListenerSupplierWhenCustomBeanProvided() {
        verifier.shouldNotRegisterScheduleStrategyListenerSupplierWhenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should register NoopContinuableTaskDecoratorSupplier by default")
    void shouldRegisterNoopContinuableTaskDecoratorSupplierByDefault() {
        verifier.shouldRegisterNoopContinuableTaskDecoratorSupplierByDefault();
    }

    @Test
    @DisplayName("IT should register MetricsContinuableTaskDecoratorSupplier when metrics enabled")
    void shouldRegisterMetricsContinuableTaskDecoratorSupplierWhenMetricsEnabled() {
        verifier.shouldRegisterMetricsContinuableTaskDecoratorSupplierWhenMetricsEnabled();
    }

    @Test
    @DisplayName("IT should not register duplicate ContinuableTaskDecoratorSupplier when custom bean provided")
    void shouldNotRegisterContinuableTaskDecoratorSupplierWhenCustomBeanProvided() {
        verifier.shouldNotRegisterContinuableTaskDecoratorSupplierWhenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should register ScheduledExecutorService by default")
    void shouldRegisterScheduledExecutorServiceByDefault() {
        verifier.shouldRegisterScheduledExecutorServiceByDefault();
    }

    @Test
    @DisplayName("IT should register ScheduledExecutorService with custom pool size")
    void shouldRegisterScheduledExecutorServiceWithCustomPoolSize() {
        verifier.shouldRegisterScheduledExecutorServiceWithCustomPoolSize();
    }

    @Test
    @DisplayName("IT should register ApplicationRunner for jobs initializer")
    void shouldRegisterApplicationRunnerForJobs() {
        verifier.shouldRegisterApplicationRunnerForJobs();
    }

    @Test
    @DisplayName("IT should register OnceboxInitializer when missing")
    void shouldRegisterOnceboxInitializerWhenMissing() {
        verifier.shouldRegisterOnceboxInitializerWhenMissing();
    }

    @Test
    @DisplayName("IT should fail when DataSource not available")
    void shouldFailWhenDataSourceNotAvailable() {
        verifier.shouldFailWhenDataSourceNotAvailable();
    }
}