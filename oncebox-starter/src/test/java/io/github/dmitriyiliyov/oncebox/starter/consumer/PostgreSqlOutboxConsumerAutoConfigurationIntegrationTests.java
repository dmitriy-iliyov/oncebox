package io.github.dmitriyiliyov.oncebox.starter.consumer;

import io.github.dmitriyiliyov.oncebox.tests.fixtures.PostgresTestContainerSingleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostgreSqlOutboxConsumerAutoConfigurationIntegrationTests {

    private final OutboxConsumerAutoConfigurationVerifier verifier =
            new OutboxConsumerAutoConfigurationVerifier(
                    PostgresTestContainerSingleton.INSTANCE.getJdbcUrl(),
                    "org.postgresql.Driver",
                    PostgresTestContainerSingleton.INSTANCE.getUsername(),
                    PostgresTestContainerSingleton.INSTANCE.getPassword()
            );

    @Test
    @DisplayName("IT should load full configuration when all features enabled")
    void shouldLoadFullConfiguration_whenAllFeaturesEnabled() {
        verifier.shouldLoadFullConfiguration_whenAllFeaturesEnabled();
    }

    @Test
    @DisplayName("IT should load full configuration when all features with rabbit enabled")
    void shouldLoadFullConfiguration_whenAllFeaturesWithRabbitEnabled() {
        verifier.shouldLoadFullConfiguration_whenAllFeaturesWithRabbitEnabled();
    }

    @Test
    @DisplayName("IT should load minimal configuration when only required properties set")
    void shouldLoadMinimalConfiguration_whenOnlyRequiredPropertiesSet() {
        verifier.shouldLoadMinimalConfiguration_whenOnlyRequiredPropertiesSet();
    }

    @Test
    @DisplayName("IT should not load consumer config when enabled=false")
    void shouldNotLoad_whenDisabled() {
        verifier.shouldNotLoad_whenDisabled();
    }

    @Test
    @DisplayName("IT should register ConsumedOutboxRepository bean")
    void shouldRegisterConsumedOutboxRepository() {
        verifier.shouldRegisterConsumedOutboxRepository();
    }

    @Test
    @DisplayName("IT should register ConsumedOutboxManager bean")
    void shouldRegisterConsumedOutboxManager() {
        verifier.shouldRegisterConsumedOutboxManager();
    }

    @Test
    @DisplayName("IT should not register ConsumedOutboxCleanUpScheduler when clean-up disabled")
    void shouldNotRegisterCleanUpScheduler_whenDisabled() {
        verifier.shouldNotRegisterCleanUpScheduler_whenDisabled();
    }

    @Test
    @DisplayName("IT should not register duplicate ConsumedOutboxManager when custom bean provided")
    void shouldNotRegisterDuplicateConsumedOutboxManager_whenCustomBeanProvided() {
        verifier.shouldNotRegisterDuplicateConsumedOutboxManager_whenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should create outbox_consumed_events table when auto-create is true")
    void shouldCreateTables_whenAutoCreateTrue() {
        verifier.shouldCreateTables_whenAutoCreateTrue();
    }

    @Test
    @DisplayName("IT should register ConsumedOutboxCleanUpScheduler when clean-up enabled")
    void shouldRegisterCleanUpScheduler_whenEnabled() {
        verifier.shouldNotRegisterCleanUpScheduler_whenEnabled();
    }

    @Test
    @DisplayName("IT should not register ConsumedOutboxManagerMetricsDecorator when metrics disabled")
    void shouldNotRegisteredMetricsRelatedBeans_whenMetricsDisabled() {
        verifier.shouldNotRegisteredMetricsRelatedBeans_whenMetricsDisabled();
    }

    @Test
    @DisplayName("IT should register ConsumedOutboxManagerMetricsDecorator when metrics enabled")
    void shouldRegisteredMetricsRelatedBeans_whenMetricsEnabled() {
        verifier.shouldRegisteredMetricsRelatedBeans_whenMetricsEnabled();
    }

    @Test
    @DisplayName("IT should not register duplicate ConsumedOutboxRepository when custom bean provided")
    void shouldNotRegisterDuplicateConsumedOutboxRepository_whenCustomBeanProvided() {
        verifier.shouldNotRegisterDuplicateConsumedOutboxRepository_whenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should not register duplicate OutboxIdempotentConsumer when custom bean provided")
    void shouldNotRegisterDuplicateOutboxIdempotentConsumer_whenCustomBeanProvided() {
        verifier.shouldNotRegisterDuplicateOutboxIdempotentConsumer_whenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should register ConsumedOutboxManagerMetricsDecorator as primary when metrics enabled")
    void shouldRegisterConsumedOutboxManagerDecorator_asPrimary_whenMetricsEnabled() {
        verifier.shouldRegisterConsumedOutboxManagerDecorator_asPrimary_whenMetricsEnabled();
    }

    @Test
    @DisplayName("IT should register OutboxIdempotentConsumerMetricsDecorator when metrics enabled")
    void shouldRegisterOutboxIdempotentConsumerMetricsDecorator_whenMetricsEnabled() {
        verifier.shouldRegisterOutboxIdempotentConsumerMetricsDecorator_whenMetricsEnabled();
    }

    @Test
    @DisplayName("IT should not register OutboxIdempotentConsumerMetricsDecorator when metrics disabled")
    void shouldNotRegisterOutboxIdempotentConsumerMetricsDecorator_whenMetricsDisabled() {
        verifier.shouldNotRegisterOutboxIdempotentConsumerMetricsDecorator_whenMetricsDisabled();
    }

    @Test
    @DisplayName("IT should register ConsumedOutboxManagerCacheDecorator cache enabled and metrics disabled")
    void shouldRegisteredConsumedOutboxManagerCacheDecorator_asPrimary_whenCacheEnableAndMetricsDisabled() {
        verifier.shouldRegisteredConsumedOutboxManagerCacheDecorator_asPrimary_whenCacheEnableAndMetricsDisabled();
    }

    @Test
    @DisplayName("IT should register ConsumedOutboxManagerCacheDecorator cache enabled and metrics missed")
    void shouldRegisteredConsumedOutboxManagerCacheDecorator_asPrimary_whenCacheEnableAndMetricsMissed() {
        verifier.shouldRegisteredConsumedOutboxManagerCacheDecorator_asPrimary_whenCacheEnableAndMetricsMissed();
    }
}
