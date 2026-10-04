package io.github.dmitriyiliyov.oncebox.metrics.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.OutboxCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultOutboxMetricsServiceUnitTests {

    @Mock
    OutboxMetricsRepository repository;

    @Mock
    OutboxCache<EventStatus> cache;

    DefaultOutboxMetricsService service;

    @BeforeEach
    void setUp() {
        service = new DefaultOutboxMetricsService(repository, cache);
    }

    @Test
    @DisplayName("UT constructor should throw NPE when repository is null")
    void constructor_shouldThrowNPE_whenRepositoryIsNull() {
        assertThrows(NullPointerException.class, () -> new DefaultOutboxMetricsService(null, cache));
    }

    @Test
    @DisplayName("UT constructor should throw NPE when cache is null")
    void constructor_shouldThrowNPE_whenCacheIsNull() {
        assertThrows(NullPointerException.class, () -> new DefaultOutboxMetricsService(repository, null));
    }

    @Test
    @DisplayName("UT count() should call CacheHelper.count")
    void count_shouldCallCacheHelperCount() {
        // given
        long expectedCount = 10L;
        when(cache.getCount()).thenReturn(null);
        when(repository.count()).thenReturn(expectedCount);
        when(cache.putCount(expectedCount)).thenReturn(expectedCount);

        // when
        long result = service.count();

        // then
        assertThat(result).isEqualTo(expectedCount);
        verify(repository).count();
        verify(cache).putCount(expectedCount);
    }

    @Test
    @DisplayName("UT countByStatus() should call CacheHelper.countByStatus")
    void countByStatus_shouldCallCacheHelperCountByStatus() {
        // given
        EventStatus status = EventStatus.PENDING;
        long expectedCount = 5L;
        when(cache.getCountByStatus(status)).thenReturn(null);
        when(repository.countByStatus(status)).thenReturn(expectedCount);
        when(cache.putCountByStatus(status, expectedCount)).thenReturn(expectedCount);

        // when
        long result = service.countByStatus(status);

        // then
        assertThat(result).isEqualTo(expectedCount);
        verify(repository).countByStatus(status);
        verify(cache).putCountByStatus(status, expectedCount);
    }

    @Test
    @DisplayName("UT countByEventTypeAndStatus() should call CacheHelper.countByEventTypeAndStatus")
    void countByEventTypeAndStatus_shouldCallCacheHelperCountByEventTypeAndStatus() {
        // given
        String eventType = "test-event";
        EventStatus status = EventStatus.PENDING;
        long expectedCount = 3L;
        when(cache.getCountByEventTypeAndStatus(eventType, status)).thenReturn(null);
        when(repository.countByEventTypeAndStatus(eventType, status)).thenReturn(expectedCount);
        when(cache.putCountByEventTypeAndStatus(eventType, status, expectedCount)).thenReturn(expectedCount);

        // when
        long result = service.countByEventTypeAndStatus(eventType, status);

        // then
        assertThat(result).isEqualTo(expectedCount);
        verify(repository).countByEventTypeAndStatus(eventType, status);
        verify(cache).putCountByEventTypeAndStatus(eventType, status, expectedCount);
    }
}
