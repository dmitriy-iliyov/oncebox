package io.github.dmitriyiliyov.oncebox.metrics.publisher.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SimpleOutboxCacheUnitTests {

    @Test
    @DisplayName("UT constructor should throw IAE when ttls has less than 3 values")
    void constructor_shouldThrowIAE_whenTtlsHasLessThan3Values() {
        assertThrows(IllegalArgumentException.class, () -> new SimpleOutboxCache<>(1, 2));
    }

    @Test
    @DisplayName("UT constructor should throw IAE when ttls has more than 3 values")
    void constructor_shouldThrowIAE_whenTtlsHasMoreThan3Values() {
        assertThrows(IllegalArgumentException.class, () -> new SimpleOutboxCache<>(1, 2, 3, 4));
    }

    @Test
    @DisplayName("UT constructor should throw NPE when ttls is null")
    void constructor_shouldThrowNPE_whenTtlsIsNull() {
        assertThrows(NullPointerException.class, () -> new SimpleOutboxCache<>(null));
    }

    @Test
    @DisplayName("UT getCount() should return null when cache is empty")
    void getCount_shouldReturnNull_whenCacheIsEmpty() {
        SimpleOutboxCache<TestStatus> cache = new SimpleOutboxCache<>(1_000L, 2_000L, 2_000L);

        assertNull(cache.getCount());
    }

    @Test
    @DisplayName("UT getCount() should return value when cache is not empty")
    void getCount_shouldReturnValue_whenCacheIsNotEmpty() {
        SimpleOutboxCache<TestStatus> cache = new SimpleOutboxCache<>(1_000L, 2_000L, 2_000L);

        cache.putCount(10L);

        assertEquals(10L, cache.getCount());
    }

    @Test
    @DisplayName("UT getCount() should return null when ttl expired")
    void getCount_shouldReturnNull_whenTtlExpired() throws InterruptedException {
        SimpleOutboxCache<TestStatus> cache = new SimpleOutboxCache<>(1_000L, 2_000L, 2_000L);

        cache.putCount(10L);
        TimeUnit.SECONDS.sleep(2);

        assertNull(cache.getCount());
    }

    @Test
    @DisplayName("UT getCountByStatus() should return null when cache is empty")
    void getCountByStatus_shouldReturnNull_whenCacheIsEmpty() {
        SimpleOutboxCache<TestStatus> cache = new SimpleOutboxCache<>(1_000L, 2_000L, 2_000L);

        assertNull(cache.getCountByStatus(TestStatus.STATUS_A));
    }

    @Test
    @DisplayName("UT getCountByStatus() should return value when cache is not empty")
    void getCountByStatus_shouldReturnValue_whenCacheIsNotEmpty() {
        SimpleOutboxCache<TestStatus> cache = new SimpleOutboxCache<>(1_000L, 1_000L, 2_000L);

        cache.putCountByStatus(TestStatus.STATUS_A, 5L);

        assertEquals(5L, cache.getCountByStatus(TestStatus.STATUS_A));
        assertNull(cache.getCountByStatus(TestStatus.STATUS_B));
    }

    @Test
    @DisplayName("UT getCountByStatus() should return null when ttl expired")
    void getCountByStatus_shouldReturnNull_whenTtlExpired() throws InterruptedException {
        SimpleOutboxCache<TestStatus> cache = new SimpleOutboxCache<>(1_000L, 2_000L, 2_000L);

        cache.putCountByStatus(TestStatus.STATUS_A, 5L);
        TimeUnit.SECONDS.sleep(3);

        assertNull(cache.getCountByStatus(TestStatus.STATUS_A));
    }

    @Test
    @DisplayName("UT getCountByEventTypeAndStatus() should return null when cache is empty")
    void getCountByEventTypeAndStatus_shouldReturnNull_whenCacheIsEmpty() {
        SimpleOutboxCache<TestStatus> cache = new SimpleOutboxCache<>(1_000L, 2_000L, 2_000L);

        assertNull(cache.getCountByEventTypeAndStatus("typeA", TestStatus.STATUS_A));
    }

    @Test
    @DisplayName("UT getCountByEventTypeAndStatus() should return value when cache is not empty")
    void getCountByEventTypeAndStatus_shouldReturnValue_whenCacheIsNotEmpty() {
        SimpleOutboxCache<TestStatus> cache = new SimpleOutboxCache<>(1_000L, 2_000L, 1_000L);

        cache.putCountByEventTypeAndStatus("typeA", TestStatus.STATUS_A, 3L);

        assertEquals(3L, cache.getCountByEventTypeAndStatus("typeA", TestStatus.STATUS_A));
        assertNull(cache.getCountByEventTypeAndStatus("typeA", TestStatus.STATUS_B));
        assertNull(cache.getCountByEventTypeAndStatus("typeB", TestStatus.STATUS_A));
    }

    @Test
    @DisplayName("UT getCountByEventTypeAndStatus() should return null when ttl expired")
    void getCountByEventTypeAndStatus_shouldReturnNull_whenTtlExpired() throws InterruptedException {
        SimpleOutboxCache<TestStatus> cache = new SimpleOutboxCache<>(1_000L, 1_000L, 2_000L);

        cache.putCountByEventTypeAndStatus("typeA", TestStatus.STATUS_A, 3L);
        TimeUnit.SECONDS.sleep(3);

        assertNull(cache.getCountByEventTypeAndStatus("typeA", TestStatus.STATUS_A));
    }

    @Test
    @DisplayName("UT getCount() when ttl is below a second should keep the value until it expires")
    void getCount_whenTtlIsBelowSecond_shouldKeepValueUntilItExpires() throws InterruptedException {
        SimpleOutboxCache<TestStatus> cache = new SimpleOutboxCache<>(500L, 500L, 500L);

        cache.putCount(10L);
        assertEquals(10L, cache.getCount());

        TimeUnit.MILLISECONDS.sleep(700);
        assertNull(cache.getCount());
    }

    private enum TestStatus {
        STATUS_A,
        STATUS_B
    }
}
