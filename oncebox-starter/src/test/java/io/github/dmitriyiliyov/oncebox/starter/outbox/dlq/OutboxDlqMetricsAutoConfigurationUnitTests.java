package io.github.dmitriyiliyov.oncebox.starter.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.NoopOutboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.OutboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.outbox.OutboxProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxDlqMetricsAutoConfigurationUnitTests {

    @Test
    @DisplayName("UT outboxDlqCache returns noop cache when metrics null")
    void outboxDlqCache_metricsNull_returnsPassthrough() {
        // given
        OutboxProperties props = new OutboxProperties();
        props.setDlq(new OutboxProperties.DlqProperties());
        OutboxDlqMetricsAutoConfiguration config = new OutboxDlqMetricsAutoConfiguration(props);

        // when
        OutboxCache<?> cache = config.outboxDlqCache();

        // then
        assertThat(cache).isInstanceOf(NoopOutboxCache.class).isNotNull();
    }

    @Test
    @DisplayName("UT outboxDlqCache returns noop cache when gaugeProperties null")
    void outboxDlqCache_gaugeNull_returnsPassthrough() {
        // given
        OutboxProperties props = new OutboxProperties();
        OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties();
        OnceboxProperties.MetricsProperties metrics = new OnceboxProperties.MetricsProperties();
        metrics.setEnabled(true);
        metrics.setGauge(null);
        props.setMetrics(metrics);
        props.setDlq(dlq);

        OutboxDlqMetricsAutoConfiguration config = new OutboxDlqMetricsAutoConfiguration(props);

        // when
        OutboxCache<?> cache = config.outboxDlqCache();

        // then
        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxDlqCache returns Passthrough cache when gauge disabled")
    void outboxDlqCache_gaugeDisabled_returnsPassthrough() {
        // given
        OutboxProperties props = new OutboxProperties();
        OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties();
        OnceboxProperties.MetricsProperties metrics = new OnceboxProperties.MetricsProperties();
        metrics.setEnabled(true);
        OnceboxProperties.MetricsProperties.GaugeProperties gauge = new OnceboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(false);
        metrics.setGauge(gauge);
        props.setMetrics(metrics);
        props.setDlq(dlq);

        OutboxDlqMetricsAutoConfiguration config = new OutboxDlqMetricsAutoConfiguration(props);

        // when
        OutboxCache<?> cache = config.outboxDlqCache();

        // then
        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxDlqCache returns SimpleOutboxCache when ttls size == 3 and enabled")
    void outboxDlqCache_validTtls_returnsSimpleCache() {
        // given
        OutboxProperties props = new OutboxProperties();
        OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties();
        OnceboxProperties.MetricsProperties metrics = new OnceboxProperties.MetricsProperties();
        metrics.setEnabled(true);
        OnceboxProperties.MetricsProperties.GaugeProperties gauge = new OnceboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);

        OnceboxProperties.MetricsProperties.GaugeProperties.CacheProperties cacheProps =
                new OnceboxProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cacheProps.setEnabled(true);
        cacheProps.setTtls(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)));
        gauge.setCache(cacheProps);
        metrics.setGauge(gauge);
        props.setMetrics(metrics);
        props.setDlq(dlq);

        OutboxDlqMetricsAutoConfiguration config = new OutboxDlqMetricsAutoConfiguration(props);

        // when
        OutboxCache<?> cache = config.outboxDlqCache();

        // then
        assertThat(cache).isInstanceOf(SimpleOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxDlqCache returns noop cache when gauge enabled but cache disabled")
    void outboxDlqCache_gaugeEnabledCacheDisabled_returnsPassthrough() {
        // given
        OutboxProperties props = new OutboxProperties();
        props.setDlq(new OutboxProperties.DlqProperties());
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
        props.setMetrics(metrics);

        OutboxDlqMetricsAutoConfiguration config = new OutboxDlqMetricsAutoConfiguration(props);

        // when
        OutboxCache<?> cache = config.outboxDlqCache();

        // then
        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxDlqCache returns noop cache when gauge enabled flag is null")
    void outboxDlqCache_gaugeEnabledNull_returnsPassthrough() {
        // given
        OutboxProperties props = new OutboxProperties();
        props.setDlq(new OutboxProperties.DlqProperties());
        OnceboxProperties.MetricsProperties metrics = new OnceboxProperties.MetricsProperties();
        OnceboxProperties.MetricsProperties.GaugeProperties gauge = new OnceboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(null);
        metrics.setGauge(gauge);
        props.setMetrics(metrics);

        OutboxDlqMetricsAutoConfiguration config = new OutboxDlqMetricsAutoConfiguration(props);

        // when
        OutboxCache<?> cache = config.outboxDlqCache();

        // then
        assertThat(cache).isInstanceOf(NoopOutboxCache.class);
    }

    @Test
    @DisplayName("UT outboxDlqCache returns SimpleOutboxCache when cache enabled flag is null")
    void outboxDlqCache_cacheEnabledNull_returnsSimpleCache() {
        // given
        OutboxProperties props = new OutboxProperties();
        props.setDlq(new OutboxProperties.DlqProperties());
        OnceboxProperties.MetricsProperties metrics = new OnceboxProperties.MetricsProperties();
        metrics.setEnabled(true);
        OnceboxProperties.MetricsProperties.GaugeProperties gauge = new OnceboxProperties.MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        OnceboxProperties.MetricsProperties.GaugeProperties.CacheProperties cacheProps =
                new OnceboxProperties.MetricsProperties.GaugeProperties.CacheProperties();
        cacheProps.setEnabled(null);
        cacheProps.setTtls(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)));
        gauge.setCache(cacheProps);
        metrics.setGauge(gauge);
        props.setMetrics(metrics);

        OutboxDlqMetricsAutoConfiguration config = new OutboxDlqMetricsAutoConfiguration(props);

        // when
        OutboxCache<?> cache = config.outboxDlqCache();

        // then
        assertThat(cache).isInstanceOf(SimpleOutboxCache.class);
    }
}
