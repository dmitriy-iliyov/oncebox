package io.github.dmitriyiliyov.oncebox.metrics.publisher.dlq;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.metrics.publisher.utils.SimpleOutboxCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultOutboxDlqMetricsServiceIntegrationTests {

    @Mock
    private OutboxDlqMetricsRepository repository;

    private DefaultOutboxDlqMetricsService service;

    @BeforeEach
    void setUp() {
        service = new DefaultOutboxDlqMetricsService(
                repository,
                new SimpleOutboxCache<>(60_000L, 60_000L, 60_000L)
        );
    }

    @Test
    @DisplayName("count() cache miss — calls repository and returns value")
    void count_cacheMiss_callsRepositoryAndReturnsValue() {
        when(repository.count()).thenReturn(5L);

        assertThat(service.count()).isEqualTo(5L);
        verify(repository, times(1)).count();
    }

    @Test
    @DisplayName("count() cache hit — does not call repository again")
    void count_cacheHit_doesNotCallRepository() {
        when(repository.count()).thenReturn(5L);
        service.count();

        service.count();

        verify(repository, times(1)).count();
    }

    @Test
    @DisplayName("count() cache expired — calls repository again")
    void count_cacheExpired_callsRepositoryAgain() throws InterruptedException {
        service = new DefaultOutboxDlqMetricsService(
                repository,
                new SimpleOutboxCache<>(0L, 60_000L, 60_000L)
        );
        when(repository.count()).thenReturn(3L, 7L);

        service.count();
        Thread.sleep(10);
        long result = service.count();

        assertThat(result).isEqualTo(7L);
        verify(repository, times(2)).count();
    }

    @Test
    @DisplayName("count() returns zero from repository")
    void count_repositoryReturnsZero_returnsZero() {
        when(repository.count()).thenReturn(0L);

        assertThat(service.count()).isEqualTo(0L);
    }

    @Test
    @DisplayName("countByStatus() cache miss — calls repository and returns value")
    void countByStatus_cacheMiss_callsRepositoryAndReturnsValue() {
        when(repository.countByStatus(DlqStatus.MOVED)).thenReturn(3L);

        assertThat(service.countByStatus(DlqStatus.MOVED)).isEqualTo(3L);
        verify(repository, times(1)).countByStatus(DlqStatus.MOVED);
    }

    @Test
    @DisplayName("countByStatus() cache hit — does not call repository again")
    void countByStatus_cacheHit_doesNotCallRepository() {
        when(repository.countByStatus(DlqStatus.MOVED)).thenReturn(3L);
        service.countByStatus(DlqStatus.MOVED);

        service.countByStatus(DlqStatus.MOVED);

        verify(repository, times(1)).countByStatus(DlqStatus.MOVED);
    }

    @Test
    @DisplayName("countByStatus() different statuses cached independently")
    void countByStatus_differentStatuses_eachCachedIndependently() {
        when(repository.countByStatus(DlqStatus.MOVED)).thenReturn(2L);
        when(repository.countByStatus(DlqStatus.RESOLVED)).thenReturn(4L);
        when(repository.countByStatus(DlqStatus.TO_RETRY)).thenReturn(1L);

        service.countByStatus(DlqStatus.MOVED);
        service.countByStatus(DlqStatus.RESOLVED);
        service.countByStatus(DlqStatus.TO_RETRY);
        service.countByStatus(DlqStatus.MOVED);
        service.countByStatus(DlqStatus.RESOLVED);
        service.countByStatus(DlqStatus.TO_RETRY);

        verify(repository, times(1)).countByStatus(DlqStatus.MOVED);
        verify(repository, times(1)).countByStatus(DlqStatus.RESOLVED);
        verify(repository, times(1)).countByStatus(DlqStatus.TO_RETRY);
    }

    @Test
    @DisplayName("countByStatus() cache expired — calls repository again")
    void countByStatus_cacheExpired_callsRepositoryAgain() throws InterruptedException {
        service = new DefaultOutboxDlqMetricsService(
                repository,
                new SimpleOutboxCache<>(60_000L, 0L, 60_000L)
        );
        when(repository.countByStatus(DlqStatus.MOVED)).thenReturn(1L, 9L);

        service.countByStatus(DlqStatus.MOVED);
        Thread.sleep(10);
        long result = service.countByStatus(DlqStatus.MOVED);

        assertThat(result).isEqualTo(9L);
        verify(repository, times(2)).countByStatus(DlqStatus.MOVED);
    }

    @Test
    @DisplayName("countByEventTypeAndStatus() cache miss — calls repository and returns value")
    void countByEventTypeAndStatus_cacheMiss_callsRepositoryAndReturnsValue() {
        when(repository.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED)).thenReturn(7L);

        assertThat(service.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED)).isEqualTo(7L);
        verify(repository, times(1)).countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED);
    }

    @Test
    @DisplayName("countByEventTypeAndStatus() cache hit — does not call repository again")
    void countByEventTypeAndStatus_cacheHit_doesNotCallRepository() {
        when(repository.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED)).thenReturn(7L);
        service.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED);

        service.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED);

        verify(repository, times(1)).countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED);
    }

    @Test
    @DisplayName("countByEventTypeAndStatus() different event types cached independently")
    void countByEventTypeAndStatus_differentEventTypes_eachCachedIndependently() {
        when(repository.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED)).thenReturn(3L);
        when(repository.countByEventTypeAndStatus("PAYMENT_CREATED", DlqStatus.MOVED)).thenReturn(6L);

        service.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED);
        service.countByEventTypeAndStatus("PAYMENT_CREATED", DlqStatus.MOVED);
        service.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED);
        service.countByEventTypeAndStatus("PAYMENT_CREATED", DlqStatus.MOVED);

        verify(repository, times(1)).countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED);
        verify(repository, times(1)).countByEventTypeAndStatus("PAYMENT_CREATED", DlqStatus.MOVED);
    }

    @Test
    @DisplayName("countByEventTypeAndStatus() same event type different statuses cached independently")
    void countByEventTypeAndStatus_sameEventTypeDifferentStatuses_cachedIndependently() {
        when(repository.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED)).thenReturn(2L);
        when(repository.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.RESOLVED)).thenReturn(4L);

        service.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED);
        service.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.RESOLVED);
        service.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED);
        service.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.RESOLVED);

        verify(repository, times(1)).countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED);
        verify(repository, times(1)).countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.RESOLVED);
    }

    @Test
    @DisplayName("countByEventTypeAndStatus() cache expired — calls repository again")
    void countByEventTypeAndStatus_cacheExpired_callsRepositoryAgain() throws InterruptedException {
        service = new DefaultOutboxDlqMetricsService(
                repository,
                new SimpleOutboxCache<>(60_000L, 60_000L, 0L)
        );
        when(repository.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED)).thenReturn(1L, 8L);

        service.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED);
        Thread.sleep(10);
        long result = service.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED);

        assertThat(result).isEqualTo(8L);
        verify(repository, times(2)).countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED);
    }

    @Test
    @DisplayName("all three caches are isolated from each other")
    void caches_areIsolatedFromEachOther() {
        when(repository.count()).thenReturn(10L);
        when(repository.countByStatus(DlqStatus.MOVED)).thenReturn(5L);
        when(repository.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED)).thenReturn(3L);

        service.count();
        service.countByStatus(DlqStatus.MOVED);
        service.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED);

        service.count();
        service.countByStatus(DlqStatus.MOVED);
        service.countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED);

        verify(repository, times(1)).count();
        verify(repository, times(1)).countByStatus(DlqStatus.MOVED);
        verify(repository, times(1)).countByEventTypeAndStatus("ORDER_CREATED", DlqStatus.MOVED);
        verifyNoMoreInteractions(repository);
    }
}