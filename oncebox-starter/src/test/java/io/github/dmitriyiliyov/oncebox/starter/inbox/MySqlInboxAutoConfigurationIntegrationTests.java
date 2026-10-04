package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.tests.fixtures.MySqlTestContainerSingleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MySqlInboxAutoConfigurationIntegrationTests {

    private final InboxAutoConfigurationVerifier verifier =
            new InboxAutoConfigurationVerifier(
                    MySqlTestContainerSingleton.INSTANCE.getJdbcUrl(),
                    "com.mysql.cj.jdbc.Driver",
                    MySqlTestContainerSingleton.INSTANCE.getUsername(),
                    MySqlTestContainerSingleton.INSTANCE.getPassword()
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
    @DisplayName("IT should not load inbox config when enabled=false")
    void shouldNotLoad_whenDisabled() {
        verifier.shouldNotLoad_whenDisabled();
    }

    @Test
    @DisplayName("IT should register InboxRepository bean")
    void shouldRegisterInboxRepository() {
        verifier.shouldRegisterInboxRepository();
    }

    @Test
    @DisplayName("IT should register InboxManager bean")
    void shouldRegisterInboxManager() {
        verifier.shouldRegisterInboxManager();
    }

    @Test
    @DisplayName("IT should not register InboxCleanUpScheduler when clean-up disabled")
    void shouldNotRegisterCleanUpScheduler_whenDisabled() {
        verifier.shouldNotRegisterCleanUpScheduler_whenDisabled();
    }

    @Test
    @DisplayName("IT should not register duplicate InboxManager when custom bean provided")
    void shouldNotRegisterDuplicateInboxManager_whenCustomBeanProvided() {
        verifier.shouldNotRegisterDuplicateInboxManager_whenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should create inbox_events table when auto-create is true")
    void shouldCreateTables_whenAutoCreateTrue() {
        verifier.shouldCreateTables_whenAutoCreateTrue();
    }

    @Test
    @DisplayName("IT should register InboxCleanUpScheduler when clean-up enabled")
    void shouldRegisterCleanUpScheduler_whenEnabled() {
        verifier.shouldNotRegisterCleanUpScheduler_whenEnabled();
    }

    @Test
    @DisplayName("IT should not register InboxManagerMetricsDecorator when metrics disabled")
    void shouldNotRegisteredMetricsRelatedBeans_whenMetricsDisabled() {
        verifier.shouldNotRegisteredMetricsRelatedBeans_whenMetricsDisabled();
    }

    @Test
    @DisplayName("IT should register InboxManagerMetricsDecorator when metrics enabled")
    void shouldRegisteredMetricsRelatedBeans_whenMetricsEnabled() {
        verifier.shouldRegisteredMetricsRelatedBeans_whenMetricsEnabled();
    }

    @Test
    @DisplayName("IT should not register duplicate InboxRepository when custom bean provided")
    void shouldNotRegisterDuplicateInboxRepository_whenCustomBeanProvided() {
        verifier.shouldNotRegisterDuplicateInboxRepository_whenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should not register duplicate IdempotentConsumer when custom bean provided")
    void shouldNotRegisterDuplicateIdempotentConsumer_whenCustomBeanProvided() {
        verifier.shouldNotRegisterDuplicateIdempotentConsumer_whenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should register InboxManagerMetricsDecorator as primary when metrics enabled")
    void shouldRegisterInboxManagerMetricsDecorator_asPrimary_whenMetricsEnabled() {
        verifier.shouldRegisterInboxManagerMetricsDecorator_asPrimary_whenMetricsEnabled();
    }

    @Test
    @DisplayName("IT should register IdempotentConsumerMetricsDecorator when metrics enabled")
    void shouldRegisterIdempotentConsumerMetricsDecorator_whenMetricsEnabled() {
        verifier.shouldRegisterIdempotentConsumerMetricsDecorator_whenMetricsEnabled();
    }

    @Test
    @DisplayName("IT should not register IdempotentConsumerMetricsDecorator when metrics disabled")
    void shouldNotRegisterIdempotentConsumerMetricsDecorator_whenMetricsDisabled() {
        verifier.shouldNotRegisterIdempotentConsumerMetricsDecorator_whenMetricsDisabled();
    }

    @Test
    @DisplayName("IT should register IdempotentConsumerCacheDecorator as primary when cache enabled and metrics disabled")
    void shouldRegisterIdempotentConsumerCacheDecorator_asPrimary_whenCacheEnableAndMetricsDisabled() {
        verifier.shouldRegisterIdempotentConsumerCacheDecorator_asPrimary_whenCacheEnableAndMetricsDisabled();
    }

    @Test
    @DisplayName("IT should register IdempotentConsumerCacheDecorator as primary when cache enabled and metrics missed")
    void shouldRegisterIdempotentConsumerCacheDecorator_asPrimary_whenCacheEnableAndMetricsMissed() {
        verifier.shouldRegisterIdempotentConsumerCacheDecorator_asPrimary_whenCacheEnableAndMetricsMissed();
    }

    @Test
    @DisplayName("IT InboxManager tryConsume(UUID) when no active transaction should throw IllegalTransactionStateException")
    void tryConsume_whenNoActiveTransaction_shouldThrowIllegalTransactionStateException() {
        verifier.tryConsume_whenNoActiveTransaction_shouldThrowIllegalTransactionStateException();
    }

    @Test
    @DisplayName("IT InboxManager tryConsumeAndGetDuplicates(Set) when no active transaction should throw IllegalTransactionStateException")
    void tryConsumeAndGetDuplicates_whenNoActiveTransaction_shouldThrowIllegalTransactionStateException() {
        verifier.tryConsumeAndGetDuplicates_whenNoActiveTransaction_shouldThrowIllegalTransactionStateException();
    }

    @Test
    @DisplayName("IT InboxManager tryConsume and tryConsumeAndGetDuplicates when inside a transaction should run")
    void inboxManagerMandatoryMethods_whenInsideTransaction_shouldRun() {
        verifier.inboxManagerMandatoryMethods_whenInsideTransaction_shouldRun();
    }
}
