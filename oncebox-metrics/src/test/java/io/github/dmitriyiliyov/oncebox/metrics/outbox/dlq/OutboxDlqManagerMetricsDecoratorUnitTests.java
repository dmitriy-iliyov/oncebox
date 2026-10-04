package io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class OutboxDlqManagerMetricsDecoratorUnitTests {

    @Mock
    OutboxDlqManager delegate;

    SimpleMeterRegistry registry;

    OutboxDlqManagerMetricsDecorator tested;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        tested = new OutboxDlqManagerMetricsDecorator(registry, delegate);
    }

    @Test
    @DisplayName("UT constructor should throw NPE when registry is null")
    void constructor_shouldThrowNPE_whenRegistryIsNull() {
        assertThrows(NullPointerException.class, () -> new OutboxDlqManagerMetricsDecorator(null, delegate));
    }

    @Test
    @DisplayName("UT constructor should throw NPE when delegate is null")
    void constructor_shouldThrowNPE_whenDelegateIsNull() {
        assertThrows(NullPointerException.class, () -> new OutboxDlqManagerMetricsDecorator(registry, null));
    }

    @Test
    @DisplayName("UT saveBatch() when events null should delegate and not increment counters")
    void saveBatch_whenEventsNull_shouldDelegateAndNotIncrement() {
        // when
        tested.saveBatch(null);

        // then
        Mockito.verify(delegate).saveBatch(null);
        Assertions.assertEquals(
                0,
                registry.getMeters().stream()
                        .filter(m -> m instanceof Counter)
                        .mapToDouble(m -> ((Counter) m).count())
                        .sum()
        );
    }

    @Test
    @DisplayName("UT saveBatch() when events empty should delegate and not increment counters")
    void saveBatch_whenEventsEmpty_shouldDelegateAndNotIncrement() {
        // when
        tested.saveBatch(List.of());

        // then
        Mockito.verify(delegate).saveBatch(List.of());
        Assertions.assertEquals(
                0,
                registry.getMeters().stream()
                        .filter(m -> m instanceof Counter)
                        .mapToDouble(m -> ((Counter) m).count())
                        .sum()
        );
    }

    @Test
    @DisplayName("UT saveBatch() when events present should delegate and increment counters")
    void saveBatch_whenEventsPresent_shouldDelegateAndIncrement() {
        // given
        OutboxDlqEvent event1 = Mockito.mock(OutboxDlqEvent.class);

        OutboxDlqEvent event2 = Mockito.mock(OutboxDlqEvent.class);

        List<OutboxDlqEvent> events = List.of(event1, event2);

        // when
        tested.saveBatch(events);

        // then
        Mockito.verify(delegate).saveBatch(events);
    }

    @Test
    @DisplayName("UT saveBatch() when event type unknown should delegate and not increment counters")
    void saveBatch_whenEventTypeUnknown_shouldDelegateAndNotIncrement() {
        // given
        OutboxDlqEvent event = Mockito.mock(OutboxDlqEvent.class);
        List<OutboxDlqEvent> events = List.of(event);

        // when
        tested.saveBatch(events);

        // then
        Mockito.verify(delegate).saveBatch(events);
    }

    @Test
    @DisplayName("UT loadAndLockBatch() when events returned should increment attempt counter")
    void loadAndLockBatch_whenEventsReturned_shouldIncrementAttemptCounter() {
        // given
        List<OutboxDlqEvent> events = List.of(Mockito.mock(OutboxDlqEvent.class), Mockito.mock(OutboxDlqEvent.class));
        Mockito.when(delegate.loadAndLockBatch(DlqStatus.MOVED, 10)).thenReturn(events);

        // when
        tested.loadAndLockBatch(DlqStatus.MOVED, 10);

        // then
        Mockito.verify(delegate).loadAndLockBatch(DlqStatus.MOVED, 10);
        Counter attemptCounter = registry.get("outbox_dlq_events_by_action_type_rate_total")
                .tag("action_type", "attempt_move_to_outbox")
                .counter();
        Assertions.assertEquals(2.0, attemptCounter.count());
    }

    @Test
    @DisplayName("UT loadAndLockBatch() when null returned should not increment attempt counter")
    void loadAndLockBatch_whenNullReturned_shouldNotIncrement() {
        // given
        Mockito.when(delegate.loadAndLockBatch(DlqStatus.MOVED, 10)).thenReturn(null);

        // when
        tested.loadAndLockBatch(DlqStatus.MOVED, 10);

        // then
        Mockito.verify(delegate).loadAndLockBatch(DlqStatus.MOVED, 10);
        Counter attemptCounter = registry.get("outbox_dlq_events_by_action_type_rate_total")
                .tag("action_type", "attempt_move_to_outbox")
                .counter();
        Assertions.assertEquals(0.0, attemptCounter.count());
    }

    @Test
    @DisplayName("UT loadAndLockBatch() when empty returned should not increment attempt counter")
    void loadAndLockBatch_whenEmptyReturned_shouldNotIncrement() {
        // given
        Mockito.when(delegate.loadAndLockBatch(DlqStatus.MOVED, 10)).thenReturn(List.of());

        // when
        tested.loadAndLockBatch(DlqStatus.MOVED, 10);

        // then
        Mockito.verify(delegate).loadAndLockBatch(DlqStatus.MOVED, 10);
        Counter attemptCounter = registry.get("outbox_dlq_events_by_action_type_rate_total")
                .tag("action_type", "attempt_move_to_outbox")
                .counter();
        Assertions.assertEquals(0.0, attemptCounter.count());
    }

    @Test
    @DisplayName("UT deleteBatch() should increment success moved counter")
    void deleteBatch_shouldIncrementSuccessMovedCounter() {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
        Mockito.when(delegate.deleteBatch(ids)).thenReturn(2);

        // when
        tested.deleteBatch(ids);

        // then
        Mockito.verify(delegate).deleteBatch(ids);
        Counter successMovedCounter = registry.get("outbox_dlq_events_by_action_type_rate_total")
                .tag("action_type", "success_moved_to_outbox")
                .counter();
        Assertions.assertEquals(2.0, successMovedCounter.count());
    }

    @Test
    @DisplayName("UT deleteResolvedBatch() should increment cleaned counter")
    void deleteResolvedBatch_shouldIncrementCleanedCounter() {
        // given
        Duration ttl = Duration.ZERO;
        int batchSize = 100;
        int deletedCount = 15;
        Mockito.when(delegate.deleteResolvedBatch(ttl, batchSize)).thenReturn(deletedCount);

        // when
        tested.deleteResolvedBatch(ttl, batchSize);

        // then
        Mockito.verify(delegate).deleteResolvedBatch(ttl, batchSize);
        Counter cleanedCounter = registry.get("outbox_dlq_events_by_action_type_rate_total")
                .tag("action_type", "cleaned")
                .counter();
        Assertions.assertEquals((double) deletedCount, cleanedCounter.count());
    }
}
