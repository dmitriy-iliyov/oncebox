package io.github.dmitriyiliyov.oncebox.metrics.outbox.utils;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoopOutboxCacheUnitTests {

    NoopOutboxCache<EventStatus> cache;

    @BeforeEach
    void setUp() {
        cache = new NoopOutboxCache<>();
    }

    @Test
    @DisplayName("UT getCount() should return null")
    void getCount_shouldReturnNull() {
        assertThat(cache.getCount()).isNull();
    }

    @Test
    @DisplayName("UT putCount() should return value")
    void putCount_shouldReturnValue() {
        long count = 10L;
        assertThat(cache.putCount(count)).isEqualTo(count);
    }

    @Test
    @DisplayName("UT getCountByStatus() should return null")
    void getCountByStatus_shouldReturnNull() {
        assertThat(cache.getCountByStatus(EventStatus.PENDING)).isNull();
    }

    @Test
    @DisplayName("UT putCountByStatus() should return value")
    void putCountByStatus_shouldReturnValue() {
        long count = 5L;
        assertThat(cache.putCountByStatus(EventStatus.PENDING, count)).isEqualTo(count);
    }

    @Test
    @DisplayName("UT getCountByEventTypeAndStatus() should return null")
    void getCountByEventTypeAndStatus_shouldReturnNull() {
        assertThat(cache.getCountByEventTypeAndStatus("test", EventStatus.PENDING)).isNull();
    }

    @Test
    @DisplayName("UT putCountByEventTypeAndStatus() should return value")
    void putCountByEventTypeAndStatus_shouldReturnValue() {
        long count = 3L;
        assertThat(cache.putCountByEventTypeAndStatus("test", EventStatus.PENDING, count)).isEqualTo(count);
    }
}
