package io.github.dmitriyiliyov.oncebox.metrics.outbox.utils;

import io.github.dmitriyiliyov.oncebox.core.outbox.domain.EventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheHelperUnitTests {

    @Mock
    OutboxCache<EventStatus> cache;

    @Mock
    Supplier<Long> countSupplier;

    @Mock
    Function<EventStatus, Long> countByStatusSupplier;

    @Mock
    BiFunction<String, EventStatus, Long> countByEventTypeAndStatusSupplier;

    @Test
    @DisplayName("UT count() should return cached value if present")
    void count_shouldReturnCachedValueIfPresent() {
        // given
        long cachedValue = 10L;
        when(cache.getCount()).thenReturn(cachedValue);

        // when
        long result = CacheHelper.count(cache, countSupplier);

        // then
        assertThat(result).isEqualTo(cachedValue);
        verifyNoInteractions(countSupplier);
    }

    @Test
    @DisplayName("UT count() should call supplier and put to cache if not present")
    void count_shouldCallSupplierAndPutToCacheIfNotPresent() {
        // given
        long newValue = 20L;
        when(cache.getCount()).thenReturn(null);
        when(countSupplier.get()).thenReturn(newValue);
        when(cache.putCount(newValue)).thenReturn(newValue);

        // when
        long result = CacheHelper.count(cache, countSupplier);

        // then
        assertThat(result).isEqualTo(newValue);
        verify(countSupplier).get();
        verify(cache).putCount(newValue);
    }

    @Test
    @DisplayName("UT countByStatus() should return cached value if present")
    void countByStatus_shouldReturnCachedValueIfPresent() {
        // given
        EventStatus status = EventStatus.PENDING;
        long cachedValue = 5L;
        when(cache.getCountByStatus(status)).thenReturn(cachedValue);

        // when
        long result = CacheHelper.countByStatus(cache, status, countByStatusSupplier);

        // then
        assertThat(result).isEqualTo(cachedValue);
        verifyNoInteractions(countByStatusSupplier);
    }

    @Test
    @DisplayName("UT countByStatus() should call supplier and put to cache if not present")
    void countByStatus_shouldCallSupplierAndPutToCacheIfNotPresent() {
        // given
        EventStatus status = EventStatus.PENDING;
        long newValue = 15L;
        when(cache.getCountByStatus(status)).thenReturn(null);
        when(countByStatusSupplier.apply(status)).thenReturn(newValue);
        when(cache.putCountByStatus(status, newValue)).thenReturn(newValue);

        // when
        long result = CacheHelper.countByStatus(cache, status, countByStatusSupplier);

        // then
        assertThat(result).isEqualTo(newValue);
        verify(countByStatusSupplier).apply(status);
        verify(cache).putCountByStatus(status, newValue);
    }

    @Test
    @DisplayName("UT countByEventTypeAndStatus() should return cached value if present")
    void countByEventTypeAndStatus_shouldReturnCachedValueIfPresent() {
        // given
        String eventType = "test-event";
        EventStatus status = EventStatus.PENDING;
        long cachedValue = 3L;
        when(cache.getCountByEventTypeAndStatus(eventType, status)).thenReturn(cachedValue);

        // when
        long result = CacheHelper.countByEventTypeAndStatus(cache, eventType, status, countByEventTypeAndStatusSupplier);

        // then
        assertThat(result).isEqualTo(cachedValue);
        verifyNoInteractions(countByEventTypeAndStatusSupplier);
    }

    @Test
    @DisplayName("UT countByEventTypeAndStatus() should call supplier and put to cache if not present")
    void countByEventTypeAndStatus_shouldCallSupplierAndPutToCacheIfNotPresent() {
        // given
        String eventType = "test-event";
        EventStatus status = EventStatus.PENDING;
        long newValue = 7L;
        when(cache.getCountByEventTypeAndStatus(eventType, status)).thenReturn(null);
        when(countByEventTypeAndStatusSupplier.apply(eventType, status)).thenReturn(newValue);
        when(cache.putCountByEventTypeAndStatus(eventType, status, newValue)).thenReturn(newValue);

        // when
        long result = CacheHelper.countByEventTypeAndStatus(cache, eventType, status, countByEventTypeAndStatusSupplier);

        // then
        assertThat(result).isEqualTo(newValue);
        verify(countByEventTypeAndStatusSupplier).apply(eventType, status);
        verify(cache).putCountByEventTypeAndStatus(eventType, status, newValue);
    }
}
