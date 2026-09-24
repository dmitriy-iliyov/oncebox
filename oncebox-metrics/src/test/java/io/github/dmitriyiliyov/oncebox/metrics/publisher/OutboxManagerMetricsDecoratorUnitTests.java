package io.github.dmitriyiliyov.oncebox.metrics.publisher;

import io.github.dmitriyiliyov.oncebox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxManager;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class OutboxManagerMetricsDecoratorUnitTests {

    private static final UUID LOCK_TOKEN = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    OutboxManager outboxManager;
    OutboxPublisherPropertiesHolder properties;
    OutboxManagerMetricsDecorator tested;
    SimpleMeterRegistry registry;
    Counter processedCounter;

    @BeforeEach
    public void initContext() {
        outboxManager = mock(OutboxManager.class);
        registry = new SimpleMeterRegistry();
        Map<String, OutboxPublisherPropertiesHolder.EventPropertiesHolder> eventProps = Map.of(
                "test-event-type", mock(OutboxPublisherPropertiesHolder.EventPropertiesHolder.class),
                "test-event-type-2", mock(OutboxPublisherPropertiesHolder.EventPropertiesHolder.class)
        );
        properties = mock(OutboxPublisherPropertiesHolder.class);
        when(properties.getEventHolders()).thenReturn(eventProps);
        tested = new OutboxManagerMetricsDecorator(properties, registry, outboxManager);
        processedCounter = registry.get("outbox_events_rate_total")
                .tag("event_type", "test-event-type")
                .tag("status", "processed")
                .counter();
    }

    @Test
    @DisplayName("UT constructor should throw NPE when properties is null")
    void constructor_shouldThrowNPE_whenPropertiesIsNull() {
        assertThrows(NullPointerException.class, () -> new OutboxManagerMetricsDecorator(null, registry, tested));
    }

    @Test
    @DisplayName("UT constructor should throw NPE when registry is null")
    void constructor_shouldThrowNPE_whenRegistryIsNull() {
        assertThrows(NullPointerException.class, () -> new OutboxManagerMetricsDecorator(properties, null, tested));
    }

    @Test
    @DisplayName("UT constructor should throw NPE when delegate is null")
    void constructor_shouldThrowNPE_whenDelegateIsNull() {
        assertThrows(NullPointerException.class, () -> new OutboxManagerMetricsDecorator(properties, registry, null));
    }

    @Test
    @DisplayName("UT finalizeBatch(): when event isn't empty and eventType != null, should delegate and involve counters")
    public void finalizeBatch_whenArgumentsValid_shouldDelegateAndGetCount() {
        // given
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), "test-event-type", "payloadType", "{}", Instant.now());
        List<OutboxEvent> events = List.of(event);
        Set<UUID> processedIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
        Set<UUID> failedIds = Set.of(UUID.randomUUID());

        // when
        tested.finalizeBatch(events, processedIds, failedIds, 5, retry -> Instant.now(), LOCK_TOKEN);

        // then
        verify(outboxManager).finalizeBatch(eq(events), eq(processedIds), eq(failedIds), eq(5), any(), eq(LOCK_TOKEN));
        assertEquals(2.0, processedCounter.count(), "Processed counter should be incremented by 2");
    }

    @Test
    @DisplayName("UT finalizeBatch(): when event isn empty, should delegate and not involve counters")
    public void finalizeBatch_whenEventsIsEmpty_shouldDelegateAndGetCount() {
        // given
        List<OutboxEvent> events = List.of();
        Set<UUID> processedIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
        Set<UUID> failedIds = Set.of(UUID.randomUUID());

        // when
        tested.finalizeBatch(events, processedIds, failedIds, 5, retry -> Instant.now(), LOCK_TOKEN);

        // then
        verify(outboxManager).finalizeBatch(eq(events), eq(processedIds), eq(failedIds), eq(5), any(), eq(LOCK_TOKEN));
        assertEquals(0, processedCounter.count(), "Processed counter should not be incremented");
    }

    @Test
    @DisplayName("UT finalizeBatch(): when event eventType == null, should delegate and not involve counters")
    public void finalizeBatch_whenEventTypeIsNull_shouldDelegateAndGetCount() {
        // given
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), null, "payloadType", "{}", Instant.now());
        List<OutboxEvent> events = List.of(event);
        Set<UUID> processedIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
        Set<UUID> failedIds = Set.of(UUID.randomUUID());

        // when
        tested.finalizeBatch(events, processedIds, failedIds, 5, retry -> Instant.now(), LOCK_TOKEN);

        // then
        verify(outboxManager).finalizeBatch(eq(events), eq(processedIds), eq(failedIds), eq(5), any(), eq(LOCK_TOKEN));
        assertEquals(0, processedCounter.count(), "Processed counter should not be incremented");
    }

    @Test
    @DisplayName("UT loadBatch(eventType, batchSize, lockToken) should delegate with the same lock token")
    void loadBatch_shouldDelegateWithSameLockToken() {
        // given
        List<OutboxEvent> events = List.of(
                new OutboxEvent(UUID.randomUUID(), "test-event-type", "payloadType", "{}", Instant.now())
        );
        when(outboxManager.loadBatch("test-event-type", 10, LOCK_TOKEN)).thenReturn(events);

        // when
        List<OutboxEvent> result = tested.loadBatch("test-event-type", 10, LOCK_TOKEN);

        // then
        assertThat(result).isSameAs(events);
        verify(outboxManager).loadBatch("test-event-type", 10, LOCK_TOKEN);
    }

    @Test
    @DisplayName("UT loadBatch(status, batchSize) should delegate and increment attempt counter")
    void loadBatch_shouldDelegateAndIncrementAttemptCounter() {
        // given
        List<OutboxEvent> events = List.of(mock(OutboxEvent.class), mock(OutboxEvent.class));
        when(outboxManager.loadBatch(EventStatus.FAILED, 10)).thenReturn(events);

        // when
        tested.loadBatch(EventStatus.FAILED, 10);

        // then
        verify(outboxManager).loadBatch(EventStatus.FAILED, 10);
        Counter attemptCounter = registry.get("outbox_events_by_action_type_rate_total")
                .tag("action_type", "attempt_move_to_dlq")
                .counter();
        assertEquals(2.0, attemptCounter.count());
    }

    @Test
    @DisplayName("UT recoverStuckBatch() should delegate and increment recovered counter")
    void recoverStuckBatch_shouldDelegateAndIncrementRecoveredCounter() {
        // given
        Duration duration = Duration.ofMinutes(1);
        when(outboxManager.recoverStuckBatch(duration, 10)).thenReturn(5);

        // when
        tested.recoverStuckBatch(duration, 10);

        // then
        verify(outboxManager).recoverStuckBatch(duration, 10);
        Counter recoveredCounter = registry.get("outbox_events_by_action_type_rate_total")
                .tag("action_type", "recovered")
                .counter();
        assertEquals(5.0, recoveredCounter.count());
    }

    @Test
    @DisplayName("UT deleteProcessedBatch() should delegate and increment cleaned counter")
    void deleteProcessedBatch_shouldDelegateAndIncrementCleanedCounter() {
        // given
        Duration ttl = Duration.ZERO;
        when(outboxManager.deleteProcessedBatch(ttl, 20)).thenReturn(15);

        // when
        tested.deleteProcessedBatch(ttl, 20);

        // then
        verify(outboxManager).deleteProcessedBatch(ttl, 20);
        Counter cleanedCounter = registry.get("outbox_events_by_action_type_rate_total")
                .tag("action_type", "cleaned")
                .counter();
        assertEquals(15.0, cleanedCounter.count());
    }

    @Test
    @DisplayName("UT deleteBatch() should delegate and increment success moved counter")
    void deleteBatch_shouldDelegateAndIncrementSuccessMovedCounter() {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID());
        when(outboxManager.deleteBatch(ids)).thenReturn(1);

        // when
        tested.deleteBatch(ids);

        // then
        verify(outboxManager).deleteBatch(ids);
        Counter successMovedCounter = registry.get("outbox_events_by_action_type_rate_total")
                .tag("action_type", "success_moved_to_dlq")
                .counter();
        assertEquals(1.0, successMovedCounter.count());
    }
}
