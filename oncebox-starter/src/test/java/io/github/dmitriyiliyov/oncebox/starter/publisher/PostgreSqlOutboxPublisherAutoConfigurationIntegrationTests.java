package io.github.dmitriyiliyov.oncebox.starter.publisher;

import io.github.dmitriyiliyov.oncebox.tests.fixtures.PostgresTestContainerSingleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostgreSqlOutboxPublisherAutoConfigurationIntegrationTests {

    private final OutboxPublisherAutoConfigurationVerifier verifier =
            new OutboxPublisherAutoConfigurationVerifier(
                    PostgresTestContainerSingleton.INSTANCE.getJdbcUrl(),
                    "org.postgresql.Driver",
                    PostgresTestContainerSingleton.INSTANCE.getUsername(),
                    PostgresTestContainerSingleton.INSTANCE.getPassword()
            );

    @Test
    @DisplayName("IT should load minimal configuration when only required properties set")
    void shouldLoadMinimalConfiguration_whenOnlyRequiredPropertiesSet() {
        verifier.shouldLoadMinimalConfiguration_whenOnlyRequiredPropertiesSet();
    }

    @Test
    @DisplayName("IT should load full configuration when all features enabled")
    void shouldLoadFullConfiguration_whenAllFeaturesEnabled() {
        verifier.shouldLoadFullConfiguration_whenAllFeaturesEnabled();
    }

    @Test
    @DisplayName("IT should load configuration when metrics enabled but gauge disabled")
    void shouldLoadConfiguration_whenMetricsEnabledButGaugeDisabled() {
        verifier.shouldLoadConfiguration_whenMetricsEnabledButGaugeDisabled();
    }

    @Test
    @DisplayName("IT should load configuration when clean-up enabled but metrics disabled")
    void shouldLoadConfiguration_whenCleanUpEnabledButMetricsDisabled() {
        verifier.shouldLoadConfiguration_whenCleanUpEnabledButMetricsDisabled();
    }

    @Test
    @DisplayName("IT should not load publisher config when enabled=false")
    void shouldNotLoad_whenDisabled() {
        verifier.shouldNotLoad_whenDisabled();
    }

    @Test
    @DisplayName("IT should register OutboxRepository bean")
    void shouldRegisterOutboxRepository() {
        verifier.shouldRegisterOutboxRepository();
    }

    @Test
    @DisplayName("IT should register OutboxManager bean")
    void shouldRegisterOutboxManager() {
        verifier.shouldRegisterOutboxManager();
    }

    @Test
    @DisplayName("IT should register OutboxProcessor bean")
    void shouldRegisterOutboxProcessor() {
        verifier.shouldRegisterOutboxProcessor();
    }

    @Test
    @DisplayName("IT should register OutboxSerializer bean")
    void shouldRegisterOutboxSerializer() {
        verifier.shouldRegisterOutboxSerializer();
    }

    @Test
    @DisplayName("IT should register OutboxPublisher bean")
    void shouldRegisterOutboxPublisher() {
        verifier.shouldRegisterOutboxPublisher();
    }

    @Test
    @DisplayName("IT should register UuidGenerator bean")
    void shouldRegisterUuidGenerator() {
        verifier.shouldRegisterUuidGenerator();
    }

    @Test
    @DisplayName("IT should register OutboxPublishAspect bean")
    void shouldRegisterOutboxPublishAspect() {
        verifier.shouldRegisterOutboxPublishAspect();
    }

    @Test
    @DisplayName("IT should registered metrics related beans when all metrics enabled")
    void shouldRegisteredMetricsRelatedBeans_whenAllMetricsEnabled() {
        verifier.shouldRegisteredMetricsRelatedBeans_whenAllMetricsEnabled();
    }

    @Test
    @DisplayName("IT should registered only OutboxManagerMetricsDecorator when gauge unabled")
    void shouldRegisteredMetricsRelatedBeans_whenGaugeUnabled() {
        verifier.shouldRegisteredMetricsRelatedBeans_whenGaugeUnabled();
    }

    @Test
    @DisplayName("IT should registered only OutboxManagerMetricsDecorator when gauge is missed")
    void shouldRegisteredMetricsRelatedBeans_whenGaugeEnabledMissed() {
        verifier.shouldRegisteredMetricsRelatedBeans_whenGaugeEnabledMissed();
    }

    @Test
    @DisplayName("IT should not register duplicate OutboxRepository when custom bean provided")
    void shouldNotRegisterOutboxRepository_whenCustomBeanProvided() {
        verifier.shouldNotRegisterOutboxRepository_whenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should not register duplicate OutboxProcessor when custom bean provided")
    void shouldNotRegisterOutboxProcessor_whenCustomBeanProvided() {
        verifier.shouldNotRegisterOutboxProcessor_whenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should not register duplicate OutboxManager when custom bean provided")
    void shouldNotRegisterOutboxManager_whenCustomBeanProvided() {
        verifier.shouldNotRegisterOutboxManager_whenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should not register duplicate OutboxSender when custom bean provided")
    void shouldNotRegisterOutboxSender_whenCustomBeanProvided() {
        verifier.shouldNotRegisterOutboxSender_whenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should not register duplicate OutboxSerializer when custom bean provided")
    void shouldNotRegisterOutboxSerializer_whenCustomBeanProvided() {
        verifier.shouldNotRegisterOutboxSerializer_whenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should not register duplicate UuidGenerator when custom bean provided")
    void shouldNotRegisterUuidGenerator_whenCustomBeanProvided() {
        verifier.shouldNotRegisterUuidGenerator_whenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should register OutboxManagerMetricsDecorator as primary when metrics enabled")
    void shouldRegisterOutboxManager_whenMetricsEnabled_hasDecoratorAsPrimary() {
        verifier.shouldRegisterOutboxManager_whenMetricsEnabled_hasDecoratorAsPrimary();
    }

    @Test
    @DisplayName("IT should register OutboxPublisherScheduler per each event type")
    void shouldRegisterPublisherScheduler_perEventType() {
        verifier.shouldRegisterPublisherScheduler_perEventType();
    }

    @Test
    @DisplayName("IT should always register OutboxRecoveryScheduler")
    void shouldRegisterRecoveryScheduler() {
        verifier.shouldRegisterRecoveryScheduler();
    }

    @Test
    @DisplayName("IT should register OutboxCleanUpScheduler when clean-up enabled")
    void shouldRegisterCleanUpScheduler_whenEnabled() {
        verifier.shouldRegisterCleanUpScheduler_whenEnabled();
    }

    @Test
    @DisplayName("IT should not register OutboxCleanUpScheduler when clean-up disabled")
    void shouldNotRegisterCleanUpScheduler_whenDisabled() {
        verifier.shouldNotRegisterCleanUpScheduler_whenDisabled();
    }

    @Test
    @DisplayName("IT should inject metrics decorator into schedulers when metrics enabled")
    void shouldRegisterPublisherScheduler_withMetricsDecoratorAsManager() {
        verifier.shouldRegisterPublisherScheduler_withMetricsDecoratorAsManager();
    }

    @Test
    @DisplayName("IT should not register duplicate OutboxCache when custom bean provided")
    void shouldNotRegisterOutboxCache_whenCustomBeanProvided() {
        verifier.shouldNotRegisterOutboxCache_whenCustomBeanProvided();
    }

    @Test
    @DisplayName("IT should register OutboxJobCreateCommand when clean-up enabled")
    void shouldRegisterCleanUpJobCreateCommand_whenEnabled() {
        verifier.shouldRegisterCleanUpJobCreateCommand_whenEnabled();
    }

    @Test
    @DisplayName("IT should not register OutboxJobCreateCommand when clean-up disabled")
    void shouldNotRegisterCleanUpJobCreateCommand_whenDisabled() {
        verifier.shouldNotRegisterCleanUpJobCreateCommand_whenDisabled();
    }
}