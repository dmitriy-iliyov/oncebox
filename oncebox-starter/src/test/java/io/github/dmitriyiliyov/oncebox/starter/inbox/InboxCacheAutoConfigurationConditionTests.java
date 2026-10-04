package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.core.inbox.cache.InboxCacheListener;
import io.github.dmitriyiliyov.oncebox.inbox.cache.DefaultInboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.inbox.MetricsInboxCacheListener;
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

class InboxCacheAutoConfigurationConditionTests {

    private final InboxProperties properties = mock(InboxProperties.class);
    private final CacheManager cacheManager = mock(CacheManager.class);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(InboxCacheAutoConfiguration.class))
            .withBean(InboxProperties.class, () -> properties)
            .withBean(CacheManager.class, () -> cacheManager);

    @Test
    @DisplayName("IT should load configuration when all conditions are met")
    void shouldLoadWhenAllConditionsMet() {
        InboxProperties.CacheProperties cache = mock(InboxProperties.CacheProperties.class);
        when(cache.getCacheName()).thenReturn("cache-name");
        when(properties.getCache()).thenReturn(cache);
        when(cacheManager.getCache("cache-name")).thenReturn(mock(Cache.class));
        contextRunner
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.cache.enabled=true"
                )
                .run(context -> assertThat(context).hasSingleBean(InboxCacheAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.inbox.enabled' is false")
    void shouldNotLoadWhenInboxEnabledIsFalse() {
        contextRunner
                .withPropertyValues(
                        "oncebox.inbox.enabled=false",
                        "oncebox.inbox.cache.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(InboxCacheAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.inbox.enabled' is missing")
    void shouldNotLoadWhenInboxEnabledIsMissing() {
        contextRunner
                .withPropertyValues(
                        "oncebox.inbox.cache.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(InboxCacheAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.inbox.cache.enabled' is false")
    void shouldNotLoadWhenCacheEnabledIsFalse() {
        contextRunner
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.cache.enabled=false"
                )
                .run(context -> assertThat(context).doesNotHaveBean(InboxCacheAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when 'oncebox.inbox.cache.enabled' is missing")
    void shouldNotLoadWhenCacheEnabledIsMissing() {
        contextRunner
                .withPropertyValues(
                        "oncebox.inbox.enabled=true"
                )
                .run(context -> assertThat(context).doesNotHaveBean(InboxCacheAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should NOT load configuration when DefaultInboxCache class is missing on classpath")
    void shouldNotLoadWhenRequiredClassIsMissing() {
        contextRunner
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.cache.enabled=true"
                )
                .withClassLoader(new FilteredClassLoader(DefaultInboxCache.class))
                .run(context -> assertThat(context).doesNotHaveBean(InboxCacheAutoConfiguration.class));
    }

    @Test
    @DisplayName("IT should expose NOOP cache listener when metrics are disabled")
    void shouldUseNoopCacheListenerWhenMetricsDisabled() {
        stubCache();
        contextRunner
                .withConfiguration(AutoConfigurations.of(InboxCacheMetricsAutoConfiguration.class))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.metrics.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(InboxCacheListener.class);
                    assertThat(context).hasBean("noopInboxCacheListener");
                    assertThat(context).doesNotHaveBean(MetricsInboxCacheListener.class);
                });
    }

    @Test
    @DisplayName("IT should expose metrics cache listener when metrics are enabled")
    void shouldUseMetricsCacheListenerWhenMetricsEnabled() {
        stubCache();
        contextRunner
                .withConfiguration(AutoConfigurations.of(InboxCacheMetricsAutoConfiguration.class))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasBean("metricsInboxCacheListener");
                    // the noop bean is also registered, the metrics one wins by being @Primary
                    assertThat(context.getBean(InboxCacheListener.class))
                            .isInstanceOf(MetricsInboxCacheListener.class);
                });
    }

    @Test
    @DisplayName("IT should fall back to NOOP cache listener when metrics are enabled but oncebox-metrics is missing")
    void shouldFallBackToNoopWhenMetricsEnabledButMetricsModuleMissing() {
        stubCache();
        contextRunner
                .withConfiguration(AutoConfigurations.of(InboxCacheMetricsAutoConfiguration.class))
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.metrics.enabled=true"
                )
                .withClassLoader(new FilteredClassLoader(MetricsInboxCacheListener.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(InboxCacheListener.class);
                    assertThat(context).hasBean("noopInboxCacheListener");
                });
    }

    @Test
    @DisplayName("IT should fall back to NOOP cache listener when metrics are enabled but micrometer is missing")
    void shouldFallBackToNoopWhenMetricsEnabledButMicrometerMissing() {
        stubCache();
        contextRunner
                .withConfiguration(AutoConfigurations.of(InboxCacheMetricsAutoConfiguration.class))
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.metrics.enabled=true"
                )
                .withClassLoader(new FilteredClassLoader(MeterRegistry.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(InboxCacheListener.class);
                    assertThat(context).hasBean("noopInboxCacheListener");
                });
    }

    private void stubCache() {
        InboxProperties.CacheProperties cache = mock(InboxProperties.CacheProperties.class);
        when(cache.getCacheName()).thenReturn("cache-name");
        when(properties.getCache()).thenReturn(cache);
        when(cacheManager.getCache("cache-name")).thenReturn(mock(Cache.class));
    }
}