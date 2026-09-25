package io.github.dmitriyiliyov.oncebox.metrics.publisher;

import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
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
public class DefaultOutboxMetricsServiceIntegrationTests {

    @Mock
    private OutboxMetricsRepository repository;

    private DefaultOutboxMetricsService service;

    @BeforeEach
    void setUp() {
        service = new DefaultOutboxMetricsService(
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
        service = new DefaultOutboxMetricsService(
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
        when(repository.countByStatus(EventStatus.PENDING)).thenReturn(3L);

        assertThat(service.countByStatus(EventStatus.PENDING)).isEqualTo(3L);
        verify(repository, times(1)).countByStatus(EventStatus.PENDING);
    }

    @Test
    @DisplayName("countByStatus() cache hit — does not call repository again")
    void countByStatus_cacheHit_doesNotCallRepository() {
        when(repository.countByStatus(EventStatus.PENDING)).thenReturn(3L);
        service.countByStatus(EventStatus.PENDING);

        service.countByStatus(EventStatus.PENDING);

        verify(repository, times(1)).countByStatus(EventStatus.PENDING);
    }

    @Test
    @DisplayName("countByStatus() different statuses cached independently")
    void countByStatus_differentStatuses_eachCachedIndependently() {
        when(repository.countByStatus(EventStatus.PENDING)).thenReturn(2L);
        when(repository.countByStatus(EventStatus.IN_PROCESS)).thenReturn(4L);
        when(repository.countByStatus(EventStatus.PROCESSED)).thenReturn(1L);

        service.countByStatus(EventStatus.PENDING);
        service.countByStatus(EventStatus.IN_PROCESS);
        service.countByStatus(EventStatus.PROCESSED);
        service.countByStatus(EventStatus.PENDING);
        service.countByStatus(EventStatus.IN_PROCESS);
        service.countByStatus(EventStatus.PROCESSED);

        verify(repository, times(1)).countByStatus(EventStatus.PENDING);
        verify(repository, times(1)).countByStatus(EventStatus.IN_PROCESS);
        verify(repository, times(1)).countByStatus(EventStatus.PROCESSED);
    }

    @Test
    @DisplayName("countByStatus() cache expired — calls repository again")
    void countByStatus_cacheExpired_callsRepositoryAgain() throws InterruptedException {
        service = new DefaultOutboxMetricsService(
                repository,
                new SimpleOutboxCache<>(60_000L, 0L, 60_000L)
        );
        when(repository.countByStatus(EventStatus.PENDING)).thenReturn(1L, 9L);

        service.countByStatus(EventStatus.PENDING);
        Thread.sleep(10);
        long result = service.countByStatus(EventStatus.PENDING);

        assertThat(result).isEqualTo(9L);
        verify(repository, times(2)).countByStatus(EventStatus.PENDING);
    }

    @Test
    @DisplayName("countByStatus() cache miss for one status does not affect other")
    void countByStatus_cacheMissForOneStatus_doesNotAffectOther() {
        when(repository.countByStatus(EventStatus.PENDING)).thenReturn(5L);
        when(repository.countByStatus(EventStatus.PROCESSED)).thenReturn(2L);

        service.countByStatus(EventStatus.PENDING);
        service.countByStatus(EventStatus.PROCESSED);

        verify(repository).countByStatus(EventStatus.PENDING);
        verify(repository).countByStatus(EventStatus.PROCESSED);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("countByEventTypeAndStatus() cache miss — calls repository and returns value")
    void countByEventTypeAndStatus_cacheMiss_callsRepositoryAndReturnsValue() {
        when(repository.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING)).thenReturn(7L);

        assertThat(service.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING)).isEqualTo(7L);
        verify(repository, times(1)).countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING);
    }

    @Test
    @DisplayName("countByEventTypeAndStatus() cache hit — does not call repository again")
    void countByEventTypeAndStatus_cacheHit_doesNotCallRepository() {
        when(repository.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING)).thenReturn(7L);
        service.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING);

        service.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING);

        verify(repository, times(1)).countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING);
    }

    @Test
    @DisplayName("countByEventTypeAndStatus() different event types cached independently")
    void countByEventTypeAndStatus_differentEventTypes_eachCachedIndependently() {
        when(repository.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING)).thenReturn(3L);
        when(repository.countByEventTypeAndStatus("PAYMENT_CREATED", EventStatus.PENDING)).thenReturn(6L);

        service.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING);
        service.countByEventTypeAndStatus("PAYMENT_CREATED", EventStatus.PENDING);
        service.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING);
        service.countByEventTypeAndStatus("PAYMENT_CREATED", EventStatus.PENDING);

        verify(repository, times(1)).countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING);
        verify(repository, times(1)).countByEventTypeAndStatus("PAYMENT_CREATED", EventStatus.PENDING);
    }

    @Test
    @DisplayName("countByEventTypeAndStatus() same event type different statuses cached independently")
    void countByEventTypeAndStatus_sameEventTypeDifferentStatuses_cachedIndependently() {
        when(repository.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING)).thenReturn(2L);
        when(repository.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PROCESSED)).thenReturn(4L);

        service.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING);
        service.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PROCESSED);
        service.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING);
        service.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PROCESSED);

        verify(repository, times(1)).countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING);
        verify(repository, times(1)).countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PROCESSED);
    }

    @Test
    @DisplayName("countByEventTypeAndStatus() cache expired — calls repository again")
    void countByEventTypeAndStatus_cacheExpired_callsRepositoryAgain() throws InterruptedException {
        service = new DefaultOutboxMetricsService(
                repository,
                new SimpleOutboxCache<>(60_000L, 60_000L, 0L)
        );
        when(repository.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING)).thenReturn(1L, 8L);

        service.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING);
        Thread.sleep(10);
        long result = service.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING);

        assertThat(result).isEqualTo(8L);
        verify(repository, times(2)).countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING);
    }

    @Test
    @DisplayName("all three caches are isolated from each other")
    void caches_areIsolatedFromEachOther() {
        when(repository.count()).thenReturn(10L);
        when(repository.countByStatus(EventStatus.PENDING)).thenReturn(5L);
        when(repository.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING)).thenReturn(3L);

        service.count();
        service.countByStatus(EventStatus.PENDING);
        service.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING);

        service.count();
        service.countByStatus(EventStatus.PENDING);
        service.countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING);

        verify(repository, times(1)).count();
        verify(repository, times(1)).countByStatus(EventStatus.PENDING);
        verify(repository, times(1)).countByEventTypeAndStatus("ORDER_CREATED", EventStatus.PENDING);
        verifyNoMoreInteractions(repository);
    }
}