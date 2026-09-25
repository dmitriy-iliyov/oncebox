package io.github.dmitriyiliyov.oncebox.starter.consumer;

import io.github.dmitriyiliyov.oncebox.consumer.cache.DefaultConsumedOutboxCache;
import io.github.dmitriyiliyov.oncebox.core.consumer.cache.ConsumedOutboxCacheListener;
import io.github.dmitriyiliyov.oncebox.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.oncebox.metrics.consumer.MetricsConsumedOutboxCacheListener;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxConsumerCacheAutoConfigurationConditionTests {

    private final OutboxConsumerProperties properties = mock(OutboxConsumerProperties.class);
    private final CacheManager cacheManager = mock(CacheManager.class);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxConsumerCacheAutoConfiguration.class))
            .withBean(OutboxConsumerProperties.class, () -> properties)
            .withBean(CacheManager.class, () -> cacheManager);

    @Test
    @DisplayName("IT should load configuration when all conditions are met")
    void shouldLoadWhenAllConditionsMet() {
        OutboxConsumerProperties.CacheProperties cache = mock(OutboxConsumerProperties.CacheProperties.class);
        when(cache.getCacheName()).thenReturn("cache-name");
        when(properties.getCache()).thenReturn(cache);
        when(cacheManager.getCache("cache-name")).thenReturn(mock(Cache.class));
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=true"
                )
                .run(context -> assertThat(context).hasSingleBean(OutboxConsumerCacheAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.consumer.enabled' is false")
    void shouldNotLoadWhenConsumerEnabledIsFalse() {
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.enabled=false",
                        "oncebox.consumer.cache.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(OutboxConsumerCacheAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.consumer.enabled' is missing")
    void shouldNotLoadWhenConsumerEnabledIsMissing() {
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.cache.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(OutboxConsumerCacheAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.consumer.cache.enabled' is false")
    void shouldNotLoadWhenCacheEnabledIsFalse() {
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=false"
                )
                .run(context -> assertThat(context).doesNotHaveBean(OutboxConsumerCacheAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.consumer.cache.enabled' is missing")
    void shouldNotLoadWhenCacheEnabledIsMissing() {
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(OutboxConsumerCacheAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when DefaultConsumedOutboxCache class is missing on classpath")
    void shouldNotLoadWhenRequiredClassIsMissing() {
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=true"
                )
                .withClassLoader(new FilteredClassLoader(DefaultConsumedOutboxCache.class))
                .run(context -> assertThat(context).doesNotHaveBean(OutboxConsumerCacheAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should expose NOOP cache listener when metrics are disabled")
    void shouldUseNoopCacheListenerWhenMetricsDisabled() {
        stubCache();
        contextRunner
                .withConfiguration(AutoConfigurations.of(OutboxConsumerCacheMetricsAutoConfiguration.class))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=true",
                        "oncebox.consumer.metrics.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(ConsumedOutboxCacheListener.class);
                    assertThat(context).hasBean("noopConsumedOutboxCacheListener");
                    assertThat(context).doesNotHaveBean(MetricsConsumedOutboxCacheListener.class);
                });
    }

    @Test
    @DisplayName("IT should expose metrics cache listener when metrics are enabled")
    void shouldUseMetricsCacheListenerWhenMetricsEnabled() {
        stubCache();
        contextRunner
                .withConfiguration(AutoConfigurations.of(OutboxConsumerCacheMetricsAutoConfiguration.class))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=true",
                        "oncebox.consumer.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasBean("metricsConsumedOutboxCacheListener");
                    // the noop bean is also registered, the metrics one wins by being @Primary
                    assertThat(context.getBean(ConsumedOutboxCacheListener.class))
                            .isInstanceOf(MetricsConsumedOutboxCacheListener.class);
                });
    }

    @Test
    @DisplayName("IT should fall back to NOOP cache listener when metrics are enabled but oncebox-metrics is missing")
    void shouldFallBackToNoopWhenMetricsEnabledButMetricsModuleMissing() {
        stubCache();
        contextRunner
                .withConfiguration(AutoConfigurations.of(OutboxConsumerCacheMetricsAutoConfiguration.class))
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=true",
                        "oncebox.consumer.metrics.enabled=true"
                )
                .withClassLoader(new FilteredClassLoader(OutboxMetrics.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ConsumedOutboxCacheListener.class);
                    assertThat(context).hasBean("noopConsumedOutboxCacheListener");
                });
    }

    @Test
    @DisplayName("IT should fall back to NOOP cache listener when metrics are enabled but micrometer is missing")
    void shouldFallBackToNoopWhenMetricsEnabledButMicrometerMissing() {
        stubCache();
        contextRunner
                .withConfiguration(AutoConfigurations.of(OutboxConsumerCacheMetricsAutoConfiguration.class))
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=true",
                        "oncebox.consumer.metrics.enabled=true"
                )
                .withClassLoader(new FilteredClassLoader(MeterRegistry.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ConsumedOutboxCacheListener.class);
                    assertThat(context).hasBean("noopConsumedOutboxCacheListener");
                });
    }

    private void stubCache() {
        OutboxConsumerProperties.CacheProperties cache = mock(OutboxConsumerProperties.CacheProperties.class);
        when(cache.getCacheName()).thenReturn("cache-name");
        when(properties.getCache()).thenReturn(cache);
        when(cacheManager.getCache("cache-name")).thenReturn(mock(Cache.class));
    }
}