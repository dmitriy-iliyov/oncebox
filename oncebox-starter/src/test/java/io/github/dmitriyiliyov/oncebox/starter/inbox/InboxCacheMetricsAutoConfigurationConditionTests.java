package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.inbox.cache.DefaultInboxCache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class InboxCacheMetricsAutoConfigurationConditionTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(InboxCacheMetricsAutoConfiguration.class))
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @Test
    @DisplayName("IT should load configuration when all conditions are met")
    void shouldLoadWhenAllConditionsMet() {
        contextRunner
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(InboxCacheMetricsAutoConfiguration.class);
                    assertThat(context).hasBean("metricsInboxCacheListener");
                });
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.inbox.enabled' is false")
    void shouldNotLoadWhenInboxEnabledIsFalse() {
        contextRunner
                .withPropertyValues(
                        "oncebox.inbox.enabled=false",
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.metrics.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(InboxCacheMetricsAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.inbox.cache.enabled' is false")
    void shouldNotLoadWhenCacheEnabledIsFalse() {
        contextRunner
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.cache.enabled=false",
                        "oncebox.inbox.metrics.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(InboxCacheMetricsAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.inbox.cache.enabled' is missing")
    void shouldNotLoadWhenCacheEnabledIsMissing() {
        contextRunner
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.metrics.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(InboxCacheMetricsAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.inbox.metrics.enabled' is false")
    void shouldNotLoadWhenMetricsEnabledIsFalse() {
        contextRunner
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.metrics.enabled=false"
                )
                .run(context -> assertThat(context).doesNotHaveBean(InboxCacheMetricsAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.inbox.metrics.enabled' is missing")
    void shouldNotLoadWhenMetricsEnabledIsMissing() {
        contextRunner
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.cache.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(InboxCacheMetricsAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when DefaultInboxCache class is missing on classpath")
    void shouldNotLoadWhenRequiredClassIsMissing() {
        contextRunner
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.metrics.enabled=true"
                )
                .withClassLoader(new FilteredClassLoader(DefaultInboxCache.class))
                .run(context -> assertThat(context).doesNotHaveBean(InboxCacheMetricsAutoConfiguration.class));
    }
}
