package io.github.dmitriyiliyov.oncebox.starter.consumer;

import io.github.dmitriyiliyov.oncebox.consumer.cache.DefaultConsumedOutboxCache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxConsumerCacheMetricsAutoConfigurationConditionTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxConsumerCacheMetricsAutoConfiguration.class))
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @Test
    @DisplayName("IT should load configuration when all conditions are met")
    void shouldLoadWhenAllConditionsMet() {
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=true",
                        "oncebox.consumer.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OutboxConsumerCacheMetricsAutoConfiguration.class);
                    assertThat(context).hasBean("metricsConsumedOutboxCacheListener");
                });
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.consumer.enabled' is false")
    void shouldNotLoadWhenConsumerEnabledIsFalse() {
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.enabled=false",
                        "oncebox.consumer.cache.enabled=true",
                        "oncebox.consumer.metrics.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(OutboxConsumerCacheMetricsAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.consumer.cache.enabled' is false")
    void shouldNotLoadWhenCacheEnabledIsFalse() {
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=false",
                        "oncebox.consumer.metrics.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(OutboxConsumerCacheMetricsAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.consumer.cache.enabled' is missing")
    void shouldNotLoadWhenCacheEnabledIsMissing() {
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.metrics.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(OutboxConsumerCacheMetricsAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.consumer.metrics.enabled' is false")
    void shouldNotLoadWhenMetricsEnabledIsFalse() {
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=true",
                        "oncebox.consumer.metrics.enabled=false"
                )
                .run(context -> assertThat(context).doesNotHaveBean(OutboxConsumerCacheMetricsAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.consumer.metrics.enabled' is missing")
    void shouldNotLoadWhenMetricsEnabledIsMissing() {
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(OutboxConsumerCacheMetricsAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when DefaultConsumedOutboxCache class is missing on classpath")
    void shouldNotLoadWhenRequiredClassIsMissing() {
        contextRunner
                .withPropertyValues(
                        "oncebox.consumer.enabled=true",
                        "oncebox.consumer.cache.enabled=true",
                        "oncebox.consumer.metrics.enabled=true"
                )
                .withClassLoader(new FilteredClassLoader(DefaultConsumedOutboxCache.class))
                .run(context -> assertThat(context).doesNotHaveBean(OutboxConsumerCacheMetricsAutoConfiguration.class));
    }
}
