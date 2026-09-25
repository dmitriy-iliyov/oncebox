package io.github.dmitriyiliyov.oncebox.starter.consumer;

import io.github.dmitriyiliyov.oncebox.starter.OutboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.TransportType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class OutboxConsumerPropertiesUnitTests {

    @Test
    @DisplayName("UT init() when enabled null should disable consumer and initialize defaults")
    void applyDefaults_whenEnabledNull_shouldDisable() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(null);

        // when
        props.applyDefaults();

        // then
        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getSource()).isNotNull();

        assertThat(props.getCleanUp()).isNotNull();
        assertThat(props.getCleanUp().isEnabled()).isFalse();

        assertThat(props.getCache()).isNotNull();
        assertThat(props.getCache().isEnabled()).isFalse();

        assertThat(props.getMetrics()).isNotNull();
        assertThat(props.getMetrics().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT init() when enabled false should disable consumer and initialize defaults")
    void applyDefaults_whenEnabledFalse_shouldDisable() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(false);

        // when
        props.applyDefaults();

        // then
        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getSource()).isNotNull();

        assertThat(props.getCleanUp()).isNotNull();
        assertThat(props.getCleanUp().isEnabled()).isFalse();

        assertThat(props.getCache()).isNotNull();
        assertThat(props.getCache().isEnabled()).isFalse();

        assertThat(props.getMetrics()).isNotNull();
        assertThat(props.getMetrics().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT init() when enabled true and nested null should initialize defaults")
    void applyDefaults_whenEnabledTrueAndNestedNull_shouldInitializeDefaults() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(true);
        props.setSource(mock(OutboxConsumerProperties.SourceProperties.class));

        // when
        props.applyDefaults();

        // then
        assertThat(props.isEnabled()).isTrue();

        assertThat(props.getCleanUp()).isNotNull();
        assertThat(props.getCleanUp().isEnabled()).isTrue();

        assertThat(props.getCache()).isNotNull();
        assertThat(props.getCache().isEnabled()).isFalse();

        assertThat(props.getMetrics()).isNotNull();
        assertThat(props.getMetrics().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT init() when enabled true and cleanUp disabled should keep cleanUp disabled")
    void applyDefaults_whenCleanUpDisabled_shouldKeepDisabled() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(true);
        props.setSource(mock(OutboxConsumerProperties.SourceProperties.class));

        OutboxProperties.CleanUpProperties cleanUp = new OutboxProperties.CleanUpProperties();
        cleanUp.setEnabled(false);
        props.setCleanUp(cleanUp);

        OutboxConsumerProperties.CacheProperties cache = new OutboxConsumerProperties.CacheProperties();
        cache.setEnabled(false);
        props.setCache(cache);

        // when
        props.applyDefaults();

        // then
        assertThat(props.getCleanUp()).isNotNull();
        assertThat(props.getCleanUp().isEnabled()).isFalse();
    }

    @Test
    @DisplayName("UT init() when enabled true and source null should throw NullPointerException")
    void applyDefaults_whenSourceIsNullAndEnabledTrue_shouldThrowNPE() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(true);
        props.setSource(null);

        // when + then
        assertThrows(NullPointerException.class, props::applyDefaults);
    }

    @Test
    @DisplayName("UT init() when source type is null should throw IllegalArgumentException")
    void applyDefaults_whenSourceTypeIsNull_shouldThrowIAE() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(false);
        props.applyDefaults();

        props.setEnabled(true);

        // when + then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, props::applyDefaults);
        assertEquals("'source.type' cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("UT init() when source type is valid should pass")
    void applyDefaults_whenSourceTypeIsValid_shouldPass() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(false);
        props.applyDefaults();

        props.setEnabled(true);
        props.getSource().setType(mock(TransportType.class));

        // when
        props.applyDefaults();

        // then
        assertThat(props.getSource().getType()).isNotNull();
    }

    @Test
    @DisplayName("UT init() when cache enabled true and cacheName null should throw NPE")
    void applyDefaults_whenCacheEnabledTrueAndNameNull_shouldThrowNPE() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(true);
        props.setSource(mock(OutboxConsumerProperties.SourceProperties.class));

        OutboxConsumerProperties.CacheProperties cache = new OutboxConsumerProperties.CacheProperties();
        cache.setEnabled(true);
        cache.setCacheName(null);
        props.setCache(cache);

        // when + then
        assertThrows(NullPointerException.class, props::applyDefaults);
    }

    @Test
    @DisplayName("UT init() when cache enabled true and cacheName blank should throw IllegalArgumentException")
    void applyDefaults_whenCacheEnabledTrueAndNameBlank_shouldThrowIAE() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(true);
        props.setSource(mock(OutboxConsumerProperties.SourceProperties.class));

        OutboxConsumerProperties.CacheProperties cache = new OutboxConsumerProperties.CacheProperties();
        cache.setEnabled(true);
        cache.setCacheName("   ");
        props.setCache(cache);

        // when + then
        assertThrows(IllegalArgumentException.class, props::applyDefaults);
    }

    @Test
    @DisplayName("UT init() when cache enabled false should disable cache and nullify name")
    void applyDefaults_whenCacheDisabled_shouldNullifyName() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(true);
        props.setSource(mock(OutboxConsumerProperties.SourceProperties.class));

        OutboxConsumerProperties.CacheProperties cache = new OutboxConsumerProperties.CacheProperties();
        cache.setEnabled(false);
        cache.setCacheName("myCache");
        props.setCache(cache);

        // when
        props.applyDefaults();

        // then
        assertThat(props.getCache().isEnabled()).isFalse();
        assertThat(props.getCache().getCacheName()).isNull();
    }

    @Test
    @DisplayName("UT init() when CacheProperties is null should initialize to disabled")
    void applyDefaults_whenCachePropertiesIsNull_shouldPass() {
        // given
        OutboxConsumerProperties props = new OutboxConsumerProperties();
        props.setEnabled(true);
        props.setSource(mock(OutboxConsumerProperties.SourceProperties.class));
        props.setCache(null);

        // when
        props.applyDefaults();

        // then
        assertThat(props.getCache().isEnabled()).isFalse();
        assertThat(props.getCache().getCacheName()).isNull();
    }

    @Test
    @DisplayName("UT CacheProperties init() when enabled null should enable")
    void cache_applyDefaults_whenEnabledNull_shouldEnable() {
        // given
        String cacheName = "cache";
        OutboxConsumerProperties.CacheProperties cache = new OutboxConsumerProperties.CacheProperties();
        cache.setEnabled(null);
        cache.setCacheName(cacheName);

        // when
        cache.applyDefaults();

        // then
        assertThat(cache.isEnabled()).isTrue();
        assertEquals(cacheName, cache.getCacheName());
    }

    @Test
    @DisplayName("UT CacheProperties init() when enabled false should disable and nullify name")
    void cache_applyDefaults_whenEnabledFalse_shouldDisable() {
        // given
        OutboxConsumerProperties.CacheProperties cache = new OutboxConsumerProperties.CacheProperties();
        cache.setEnabled(false);
        cache.setCacheName("cache");

        // when
        cache.applyDefaults();

        // then
        assertThat(cache.isEnabled()).isFalse();
        assertThat(cache.getCacheName()).isNull();
    }

    @Test
    @DisplayName("UT CacheProperties init() when enabled true and name valid should pass")
    void cache_applyDefaults_whenEnabledTrueAndNameValid_shouldPass() {
        // given
        OutboxConsumerProperties.CacheProperties cache = new OutboxConsumerProperties.CacheProperties();
        cache.setEnabled(true);
        cache.setCacheName("validCache");

        // when
        cache.applyDefaults();

        // then
        assertThat(cache.isEnabled()).isTrue();
        assertThat(cache.getCacheName()).isEqualTo("validCache");
    }
}
