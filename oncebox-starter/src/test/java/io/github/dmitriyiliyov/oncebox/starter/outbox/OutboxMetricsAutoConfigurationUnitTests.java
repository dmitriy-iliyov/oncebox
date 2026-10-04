package io.github.dmitriyiliyov.oncebox.starter.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.NoopOutboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.OutboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class OutboxMetricsAutoConfigurationUnitTests {

    private OutboxProperties props;
    private OutboxMetricsAutoConfiguration config;

    @Mock
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        props = mock(OutboxProperties.class);
        config = new OutboxMetricsAutoConfiguration(props);
    }

    @Test
    @DisplayName("UT outboxCache returns noop when metrics null")
    void outboxCache_metricsNull_returnsNoop() {
        when(props.getMetrics()).thenReturn(null);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxCache returns noop when gauge null")
    void outboxCache_gaugeNull_returnsNoop() {
        OnceboxProperties.MetricsProperties metrics = new OnceboxProperties.MetricsProperties();
        metrics.setGauge(null);
        when(props.getMetrics()).thenReturn(metrics);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxCache returns noop when gauge disabled")
    void outboxCache_gaugeDisabled_returnsNoop() {
        OnceboxProperties.MetricsProperties metrics = new OnceboxProperties.MetricsProperties();
        OnceboxProperties.MetricsProperties.GaugeProperties gauge = new OnceboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(false);
        metrics.setGauge(gauge);
        when(props.getMetrics()).thenReturn(metrics);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxCache returns SimpleOutboxCache when valid ttls")
    void outboxCache_validTtls_returnsSimpleCache() {
        OnceboxProperties.MetricsProperties metrics = new OnceboxProperties.MetricsProperties();
        OnceboxProperties.MetricsProperties.GaugeProperties gauge = new OnceboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        OnceboxProperties.MetricsProperties.GaugeProperties.CacheProperties cacheProps =
                new OnceboxProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cacheProps.setTtls(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)));
        gauge.setCache(cacheProps);
        metrics.setGauge(gauge);
        when(props.getMetrics()).thenReturn(metrics);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(SimpleOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxCache returns noop when gauge enabled but cache disabled")
    void outboxCache_gaugeEnabledCacheDisabled_returnsNoop() {
        OnceboxProperties.MetricsProperties metrics = new OnceboxProperties.MetricsProperties();
        metrics.setEnabled(true);
        OnceboxProperties.MetricsProperties.GaugeProperties gauge = new OnceboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        OnceboxProperties.MetricsProperties.GaugeProperties.CacheProperties cacheProps =
                new OnceboxProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cacheProps.setEnabled(false);
        gauge.setCache(cacheProps);
        metrics.setGauge(gauge);
        // applyDefaults empties the ttls of a disabled cache
        metrics.applyDefaults();
        when(props.getMetrics()).thenReturn(metrics);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxCache returns noop when gauge enabled flag is null")
    void outboxCache_gaugeEnabledNull_returnsNoop() {
        OnceboxProperties.MetricsProperties metrics = new OnceboxProperties.MetricsProperties();
        OnceboxProperties.MetricsProperties.GaugeProperties gauge = new OnceboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(null);
        metrics.setGauge(gauge);
        when(props.getMetrics()).thenReturn(metrics);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxCache returns noop when gauge enabled but cache null")
    void outboxCache_gaugeEnabledCacheNull_returnsNoop() {
        OnceboxProperties.MetricsProperties metrics = new OnceboxProperties.MetricsProperties();
        OnceboxProperties.MetricsProperties.GaugeProperties gauge = new OnceboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        gauge.setCache(null);
        metrics.setGauge(gauge);
        when(props.getMetrics()).thenReturn(metrics);

        OutboxCache<?> cache = config.outboxCache();

        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }
}
