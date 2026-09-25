package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.starter.consumer.OutboxConsumerProperties;
import io.github.dmitriyiliyov.oncebox.starter.publisher.OutboxPublisherProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static io.github.dmitriyiliyov.oncebox.starter.OutboxProperties.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxPropertiesUnitTests {

    @Test
    @DisplayName("UT applyDefaults() should initialize defaults when all nested props are null")
    void applyDefaults_whenAllNestedNull_shouldInitializeDefaults() {
        // given
        OutboxProperties props = new OutboxProperties();
        props.setThreadPoolSize(null);
        props.setPublisher(null);
        props.setConsumer(null);
        props.setTables(null);

        // when
        props.applyDefaults();

        // then
        assertThat(props.getThreadPoolSize()).isNotNull();
        assertThat(props.getPublisher()).isNotNull();
        assertThat(props.getPublisher().isEnabled()).isFalse();
        assertThat(props.getConsumer()).isNotNull();
        assertThat(props.getConsumer().isEnabled()).isFalse();
        assertThat(props.getTables()).isNotNull();
        assertThat(props.getTables().isAutoCreate()).isTrue();
    }

    @Test
    @DisplayName("UT init() should preserve threadPoolSize if already set")
    void applyDefaults_shouldPreserveValue() {
        // given
        OutboxProperties props = new OutboxProperties();
        props.setThreadPoolSize(10);

        // when
        props.applyDefaults();

        // then
        assertThat(props.getThreadPoolSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("UT init() when publisher provided should call publisher.applyDefaults")
    void init_whenPublisherProvided_shouldPreserveAndApplyDefaults() {
        // given
        OutboxProperties props = new OutboxProperties();
        OutboxPublisherProperties.SenderProperties sender = new OutboxPublisherProperties.SenderProperties();
        sender.setType(TransportType.KAFKA);
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setTopic("test-topic");
        OutboxPublisherProperties publisher = new OutboxPublisherProperties();
        publisher.setEnabled(true);
        publisher.setSender(sender);
        publisher.setEvents(Map.of("test-event", event));
        props.setPublisher(publisher);

        // when
        props.applyDefaults();

        // then
        assertThat(props.getPublisher()).isNotNull();
        assertThat(props.getPublisher().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("UT init() when publisher not provided should set enabled = false")
    void applyDefaults_whenPublisherNotProvided_shouldUnable() {
        // given
        OutboxProperties props = new OutboxProperties();
        props.setPublisher(null);

        // when
        props.applyDefaults();

        // then
        assertThat(props.getPublisher()).isNotNull();
        assertThat(props.getPublisher().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT init() when consumer provided and enabled true should initialize nested")
    void init_whenConsumerProvidedEnabledTrue_shouldApplyDefaultsNested() {
        // given
        OutboxProperties props = new OutboxProperties();
        String cacheName = "cache";
        OutboxConsumerProperties.CacheProperties cache = new OutboxConsumerProperties.CacheProperties();
        cache.setCacheName(cacheName);
        OutboxConsumerProperties consumer = new OutboxConsumerProperties();
        consumer.setEnabled(true);

        OutboxConsumerProperties.SourceProperties source = new OutboxConsumerProperties.SourceProperties();
        source.setType(TransportType.KAFKA);

        consumer.setSource(source);
        consumer.setCache(cache);
        props.setConsumer(consumer);

        // when
        props.applyDefaults();

        // then
        assertThat(props.getConsumer().isEnabled()).isTrue();
        assertThat(props.getConsumer().getCleanUp()).isNotNull();
        assertThat(props.getConsumer().getCache()).isNotNull();
    }

    @Test
    @DisplayName("UT init() when consumer not provided should set enabled=false for consumer")
    void applyDefaults_whenConsumerNotProvided_shouldUnable() {
        // given
        OutboxProperties props = new OutboxProperties();
        props.setConsumer(null);

        // when
        props.applyDefaults();

        // then
        assertThat(props.getConsumer().isEnabled()).isFalse();
        assertThat(props.getConsumer().getCleanUp()).isNotNull();
        assertThat(props.getConsumer().getCleanUp().isEnabled()).isFalse();
        assertThat(props.getConsumer().getCache()).isNotNull();
        assertThat(props.getConsumer().getCache().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT init() when tables provided should preserve instance")
    void applyDefaults_whenTablesProvided_shouldPreserve() {
        // given
        OutboxProperties props = new OutboxProperties();
        TablesProperties tables = new TablesProperties();
        tables.setAutoCreate(false);
        props.setTables(tables);

        // when
        props.applyDefaults();

        // then
        assertThat(props.getTables()).isSameAs(tables);
        assertThat(props.getTables().isAutoCreate()).isFalse();
    }

    @Test
    @DisplayName("UT init() when tables not provided should set autoCreate = true")
    void applyDefaultsAutoCreateTrue() {
        // given
        OutboxProperties props = new OutboxProperties();
        props.setTables(null);

        // when
        props.applyDefaults();

        // then
        assertThat(props.getTables()).isNotNull();
        assertThat(props.getTables().isAutoCreate()).isTrue();
    }

    @Test
    @DisplayName("UT CleanUpProperties applyDefaults() should initialize defaults when enabled true or null")
    void cleanUp_init_whenEnabledNullOrTrue_shouldApplyDefaultsDefaults() {
        // given
        CleanUpProperties cleanUp = new CleanUpProperties();
        cleanUp.setEnabled(true);
        cleanUp.setBatchSize(null);
        cleanUp.setTtl(null);
        cleanUp.setPolling(null);

        // when
        cleanUp.applyDefaults();

        // then
        assertThat(cleanUp.isEnabled()).isTrue();
        assertThat(cleanUp.getBatchSize()).isEqualTo(500);
        assertThat(cleanUp.getTtl()).isEqualTo(Duration.ofHours(24));
        assertThat(cleanUp.getPolling().getType()).isEqualTo(PollingType.ADAPTIVE);
        assertThat(cleanUp.getInitialDelay()).isEqualTo(Duration.ofMinutes(5));
        assertThat(cleanUp.getFixedDelay()).isEqualTo(Duration.ZERO);
        assertThat(cleanUp.getMinFixedDelay()).isEqualTo(Duration.ofSeconds(5));
        assertThat(cleanUp.getMaxFixedDelay()).isEqualTo(Duration.ofMinutes(1));
        assertThat(cleanUp.getMultiplier()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("UT CleanUpProperties applyDefaults() when enabled false should reset values")
    void cleanUp_applyDefaults_whenDisabled_shouldResetValues() {
        // given
        CleanUpProperties cleanUp = new CleanUpProperties();
        cleanUp.setEnabled(false);
        cleanUp.setBatchSize(50);
        cleanUp.setTtl(Duration.ofMinutes(5));

        PollingProperties polling = new PollingProperties();
        polling.setType(PollingType.FIXED);
        polling.setInitialDelay(Duration.ofSeconds(1));
        polling.setFixedDelay(Duration.ofSeconds(1));
        cleanUp.setPolling(polling);

        // when
        cleanUp.applyDefaults();

        // then
        assertThat(cleanUp.isEnabled()).isFalse();
        assertThat(cleanUp.getBatchSize()).isZero();
        assertThat(cleanUp.getTtl()).isNull();
        assertThat(cleanUp.getPolling().getType()).isNull();
        assertThat(cleanUp.getInitialDelay()).isNull();
        assertThat(cleanUp.getFixedDelay()).isNull();
    }

    @Test
    @DisplayName("UT CleanUpProperties applyDefaults() when enabled null should default enabled true")
    void cleanUp_applyDefaults_whenEnabledNull_shouldEnable() {
        // given
        CleanUpProperties cleanUp = new CleanUpProperties();
        cleanUp.setEnabled(null);

        // when
        cleanUp.applyDefaults();

        // then
        assertThat(cleanUp.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("UT CleanUpProperties applyDefaults() when batchSize negative should reset to default")
    void cleanUp_applyDefaults_whenBatchSizeNegative_shouldReset() {
        // given
        CleanUpProperties cleanUp = new CleanUpProperties();
        cleanUp.setEnabled(true);
        cleanUp.setBatchSize(-10);

        // when
        cleanUp.applyDefaults();

        // then
        assertThat(cleanUp.getBatchSize()).isEqualTo(500);
    }

    @Test
    @DisplayName("UT TablesProperties applyDefaults() when autoCreate null should default true")
    void tables_applyDefaults_whenAutoCreateNull_shouldDefaultTrue() {
        // given
        TablesProperties tables = new TablesProperties();
        tables.setAutoCreate(null);

        // when
        tables.applyDefaults();

        // then
        assertThat(tables.isAutoCreate()).isTrue();
    }

    @Test
    @DisplayName("UT TablesProperties applyDefaults() when autoCreate false should preserve false")
    void tables_applyDefaults_whenAutoCreateFalse_shouldPreserve() {
        // given
        TablesProperties tables = new TablesProperties();
        tables.setAutoCreate(false);

        // when
        tables.applyDefaults();

        // then
        assertThat(tables.isAutoCreate()).isFalse();
    }

    @Test
    @DisplayName("UT CleanUpProperties applyDefaults() when batchSize <= 0 should reset to default")
    void cleanUp_applyDefaults_whenBatchSizeZero_shouldDefaultTo200() {
        // given
        CleanUpProperties cleanUp = new CleanUpProperties();
        cleanUp.setEnabled(true);
        cleanUp.setBatchSize(0);

        // when
        cleanUp.applyDefaults();

        // then
        assertThat(cleanUp.getBatchSize()).isEqualTo(500);
    }

    @Test
    @DisplayName("UT init() when threadPoolSize null should default to available processors capped at 5")
    void applyDefaults_whenThreadPoolSizeNull_shouldDefaultToProcessorCappedAt5() {
        OutboxProperties props = new OutboxProperties();
        props.setThreadPoolSize(null);

        props.applyDefaults();

        int expected = Math.min(Runtime.getRuntime().availableProcessors(), 5);
        assertThat(props.getThreadPoolSize()).isEqualTo(expected);
    }

    @Test
    @DisplayName("UT MetricsProperties applyDefaults() when enabled true should enable gauge with defaults")
    void metrics_applyDefaults_whenEnabledTrue_shouldEnableGaugeWithDefaults() {
        MetricsProperties metrics = new MetricsProperties();
        metrics.setEnabled(true);
        metrics.setGauge(null);

        metrics.applyDefaults();

        assertThat(metrics.isEnabled()).isTrue();
        assertThat(metrics.getGauge()).isNotNull();
        assertThat(metrics.getGauge().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT MetricsProperties applyDefaults() when enabled false should disable gauge")
    void metrics_applyDefaults_whenEnabledFalse_shouldDisableGauge() {
        MetricsProperties metrics = new MetricsProperties();
        metrics.setEnabled(false);

        metrics.applyDefaults();

        assertThat(metrics.isEnabled()).isFalse();
        assertThat(metrics.getGauge()).isNotNull();
        assertThat(metrics.getGauge().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT MetricsProperties applyDefaults() when enabled null should disable metrics")
    void metrics_applyDefaults_whenEnabledNull_shouldDisable() {
        MetricsProperties metrics = new MetricsProperties();
        metrics.setEnabled(null);

        metrics.applyDefaults();

        assertThat(metrics.isEnabled()).isFalse();
        assertThat(metrics.getGauge().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT MetricsProperties applyDefaults() when enabled true and gauge provided should preserve gauge")
    void metrics_applyDefaults_whenEnabledTrueAndGaugeProvided_shouldPreserveGauge() {
        MetricsProperties metrics = new MetricsProperties();
        metrics.setEnabled(true);
        MetricsProperties.GaugeProperties gauge = new MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        metrics.setGauge(gauge);

        metrics.applyDefaults();

        assertThat(metrics.isEnabled()).isTrue();
        assertThat(metrics.getGauge()).isSameAs(gauge);
        assertThat(metrics.getGauge().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("UT GaugeProperties applyDefaults() when enabled true and cache null should init cache with defaults")
    void gauge_init_whenEnabledTrueAndCacheNull_shouldApplyDefaultsCacheDefaults() {
        MetricsProperties.GaugeProperties gauge = new MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        gauge.setCache(null);

        gauge.applyDefaults();

        assertThat(gauge.isEnabled()).isTrue();
        assertThat(gauge.getCache()).isNotNull();
        assertThat(gauge.getCache().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("UT GaugeProperties applyDefaults() when enabled false should disable cache")
    void gauge_applyDefaults_whenEnabledFalse_shouldDisableCache() {
        MetricsProperties.GaugeProperties gauge = new MetricsProperties.GaugeProperties();
        gauge.setEnabled(false);

        gauge.applyDefaults();

        assertThat(gauge.isEnabled()).isFalse();
        assertThat(gauge.getCache()).isNotNull();
        assertThat(gauge.getCache().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT GaugeProperties applyDefaults() when enabled null should disable gauge and cache")
    void gauge_applyDefaults_whenEnabledNull_shouldDisable() {
        MetricsProperties.GaugeProperties gauge = new MetricsProperties.GaugeProperties();
        gauge.setEnabled(null);

        gauge.applyDefaults();

        assertThat(gauge.isEnabled()).isFalse();
        assertThat(gauge.getCache().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT GaugeProperties applyDefaults() when enabled true and cache provided should preserve cache")
    void gauge_applyDefaults_whenEnabledTrueAndCacheProvided_shouldPreserveCache() {
        MetricsProperties.GaugeProperties gauge = new MetricsProperties.GaugeProperties();
        gauge.setEnabled(true);
        MetricsProperties.GaugeProperties.CacheProperties cache =
                new MetricsProperties.GaugeProperties.CacheProperties();
        cache.setEnabled(true);
        gauge.setCache(cache);

        gauge.applyDefaults();

        assertThat(gauge.getCache()).isSameAs(cache);
        assertThat(gauge.getCache().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("UT CacheProperties applyDefaults() when enabled true and ttls null should use default ttls")
    void cache_applyDefaults_whenEnabledTrueAndTtlsNull_shouldUseDefaults() {
        MetricsProperties.GaugeProperties.CacheProperties cache =
                new MetricsProperties.GaugeProperties.CacheProperties();
        cache.setEnabled(true);
        cache.setTtls(null);

        cache.applyDefaults();

        assertThat(cache.isEnabled()).isTrue();
        assertThat(cache.getTtls()).hasSize(3);
        assertThat(cache.getTtls()).containsOnly(Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("UT CacheProperties applyDefaults() when enabled true and ttls empty should use default ttls")
    void cache_applyDefaults_whenEnabledTrueAndTtlsEmpty_shouldUseDefaults() {
        MetricsProperties.GaugeProperties.CacheProperties cache =
                new MetricsProperties.GaugeProperties.CacheProperties();
        cache.setEnabled(true);
        cache.setTtls(List.of());

        cache.applyDefaults();

        assertThat(cache.getTtls()).hasSize(3);
    }

    @Test
    @DisplayName("UT CacheProperties applyDefaults() when enabled true and ttls wrong size should use default ttls")
    void cache_applyDefaults_whenEnabledTrueAndTtlsWrongSize_shouldUseDefaults() {
        MetricsProperties.GaugeProperties.CacheProperties cache =
                new MetricsProperties.GaugeProperties.CacheProperties();
        cache.setEnabled(true);
        cache.setTtls(List.of(Duration.ofSeconds(10)));

        cache.applyDefaults();

        assertThat(cache.getTtls()).hasSize(3);
        assertThat(cache.getTtls()).containsOnly(Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("UT CacheProperties applyDefaults() when enabled true and ttls correct size should preserve ttls")
    void cache_applyDefaults_whenEnabledTrueAndTtlsCorrectSize_shouldPreserveTtls() {
        MetricsProperties.GaugeProperties.CacheProperties cache =
                new MetricsProperties.GaugeProperties.CacheProperties();
        cache.setEnabled(true);
        List<Duration> custom = List.of(Duration.ofSeconds(10), Duration.ofSeconds(20), Duration.ofSeconds(30));
        cache.setTtls(custom);

        cache.applyDefaults();

        assertThat(cache.getTtls()).isEqualTo(custom);
    }

    @Test
    @DisplayName("UT CacheProperties applyDefaults() when enabled false should return empty ttls")
    void cache_applyDefaults_whenEnabledFalse_shouldReturnEmptyTtls() {
        MetricsProperties.GaugeProperties.CacheProperties cache =
                new MetricsProperties.GaugeProperties.CacheProperties();
        cache.setEnabled(false);
        cache.setTtls(List.of(Duration.ofSeconds(10), Duration.ofSeconds(20), Duration.ofSeconds(30)));

        cache.applyDefaults();

        assertThat(cache.isEnabled()).isFalse();
        assertThat(cache.getTtls()).isEmpty();
    }

    @Test
    @DisplayName("UT CacheProperties applyDefaults() when enabled null should enable and use default ttls")
    void cache_applyDefaults_whenEnabledNull_shouldEnableAndUseDefaults() {
        MetricsProperties.GaugeProperties.CacheProperties cache =
                new MetricsProperties.GaugeProperties.CacheProperties();
        cache.setEnabled(null);

        cache.applyDefaults();

        assertThat(cache.isEnabled()).isTrue();
        assertThat(cache.getTtls()).hasSize(3);
    }

    @Test
    @DisplayName("UT CleanUpProperties applyDefaults() when custom valid values provided should preserve them")
    void cleanUp_applyDefaults_whenCustomValuesProvided_shouldPreserve() {
        CleanUpProperties cleanUp = new CleanUpProperties();
        cleanUp.setEnabled(true);
        cleanUp.setBatchSize(50);
        cleanUp.setTtl(Duration.ofMinutes(30));

        PollingProperties polling = new PollingProperties();
        polling.setType(PollingType.FIXED);
        polling.setInitialDelay(Duration.ofSeconds(10));
        polling.setFixedDelay(Duration.ofSeconds(3));
        cleanUp.setPolling(polling);

        cleanUp.applyDefaults();

        assertThat(cleanUp.getBatchSize()).isEqualTo(50);
        assertThat(cleanUp.getTtl()).isEqualTo(Duration.ofMinutes(30));
        assertThat(cleanUp.getPolling().getType()).isEqualTo(PollingType.FIXED);
        assertThat(cleanUp.getInitialDelay()).isEqualTo(Duration.ofSeconds(10));
        assertThat(cleanUp.getFixedDelay()).isEqualTo(Duration.ofSeconds(3));
        assertThat(cleanUp.getMinFixedDelay()).isEqualTo(Duration.ZERO);
        assertThat(cleanUp.getMaxFixedDelay()).isEqualTo(Duration.ZERO);
        assertThat(cleanUp.getMultiplier()).isNaN();
    }

    @Test
    @DisplayName("UT should initialize with default values")
    void shouldInitializeWithDefaultValues() {
        DistributedLockProperties properties = new DistributedLockProperties();

        assertThat(properties.getLockAtLeastFor()).isEqualTo(Duration.ofSeconds(1));
        assertThat(properties.getLockAtMostFor()).isEqualTo(Duration.ofMinutes(1));
        assertThat(properties.isResolveByPollingProperties()).isTrue();
    }

    @Test
    @DisplayName("UT applyDefaults should set durations to ZERO when resolveByPollingProperties is true")
    void applyDefaults_whenResolveByPollingPropertiesIsTrue_shouldSetDurationsToZero() {
        DistributedLockProperties properties = new DistributedLockProperties();
        properties.setLockAtLeastFor(Duration.ofSeconds(10));
        properties.setLockAtMostFor(Duration.ofSeconds(20));
        properties.setResolveByPollingProperties(true);

        properties.applyDefaults();

        assertThat(properties.getLockAtLeastFor()).isEqualTo(Duration.ZERO);
        assertThat(properties.getLockAtMostFor()).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("UT applyDefaults should keep durations when resolveByPollingProperties is false")
    void applyDefaults_whenResolveByPollingPropertiesIsFalse_shouldKeepDurations() {
        DistributedLockProperties properties = new DistributedLockProperties();
        properties.setLockAtLeastFor(Duration.ofSeconds(10));
        properties.setLockAtMostFor(Duration.ofSeconds(20));
        properties.setResolveByPollingProperties(false);

        properties.applyDefaults();

        assertThat(properties.getLockAtLeastFor()).isEqualTo(Duration.ofSeconds(10));
        assertThat(properties.getLockAtMostFor()).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    @DisplayName("UT applyDefaults should throw NPE when lockAtLeastFor is null")
    void applyDefaults_whenLockAtLeastForIsNull_shouldThrowNullPointerException() {
        DistributedLockProperties properties = new DistributedLockProperties();
        properties.setLockAtLeastFor(null);

        assertThatThrownBy(properties::applyDefaults)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("'lock-at-least-for' cannot be null");
    }

    @Test
    @DisplayName("UT applyDefaults should throw NPE when lockAtMostFor is null")
    void applyDefaults_whenLockAtMostForIsNull_shouldThrowNullPointerException() {
        DistributedLockProperties properties = new DistributedLockProperties();
        properties.setLockAtMostFor(null);

        assertThatThrownBy(properties::applyDefaults)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("'lock-at-most-for' cannot be null");
    }

    @Test
    @DisplayName("UT applyDefaults should throw NPE when resolveByPollingProperties is null due to unboxing")
    void applyDefaults_whenResolveByPollingPropertiesIsNull_shouldThrowNullPointerException() {
        DistributedLockProperties properties = new DistributedLockProperties();
        properties.setResolveByPollingProperties(null);

        assertThatThrownBy(properties::applyDefaults)
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("UT setters and getters should work correctly")
    void settersAndGettersShouldWork() {
        DistributedLockProperties properties = new DistributedLockProperties();

        properties.setLockAtLeastFor(Duration.ofSeconds(5));
        properties.setLockAtMostFor(Duration.ofSeconds(15));
        properties.setResolveByPollingProperties(false);

        assertThat(properties.getLockAtLeastFor()).isEqualTo(Duration.ofSeconds(5));
        assertThat(properties.getLockAtMostFor()).isEqualTo(Duration.ofSeconds(15));
        assertThat(properties.isResolveByPollingProperties()).isFalse();
    }

    @Test
    @DisplayName("UT toString should return formatted string")
    void toStringShouldReturnFormattedString() {
        DistributedLockProperties properties = new DistributedLockProperties();
        properties.setLockAtLeastFor(Duration.ofSeconds(2));
        properties.setLockAtMostFor(Duration.ofSeconds(5));
        properties.setResolveByPollingProperties(false);

        String result = properties.toString();

        assertThat(result).contains("lockAtLeastFor=PT2S");
        assertThat(result).contains("lockAtMostFor=PT5S");
        assertThat(result).contains("resolveByPollingProperties=false");
    }
}