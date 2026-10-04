package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.domain.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultOutboxDlqEventMapperUnitTests {

    private static final Instant FIXED_NOW = Instant.parse("2024-01-01T12:00:00Z");

    private Clock clock;
    private DefaultOutboxDlqEventMapper tested;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));
        tested = new DefaultOutboxDlqEventMapper(clock);
    }

    @Test
    @DisplayName("UT constructor when clock is null should throw NullPointerException")
    void constructor_whenClockIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultOutboxDlqEventMapper(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("clock cannot be null");
    }

    @Test
    @DisplayName("UT toDlqEvent() should preserve id from outbox event")
    void toDlqEvent_shouldPreserveId() {
        // given
        UUID id = UUID.randomUUID();
        OutboxEvent event = outboxEvent(id, 2);

        // when
        OutboxDlqEvent result = tested.toDlqEvent(event);

        // then
        assertEquals(id, result.getId());
    }

    @Test
    @DisplayName("UT toDlqEvent() should always set status to FAILED")
    void toDlqEvent_shouldSetStatusToFailed() {
        // given
        OutboxEvent event = outboxEvent(UUID.randomUUID(), 1);

        // when
        OutboxDlqEvent result = tested.toDlqEvent(event);

        // then
        assertEquals(EventStatus.FAILED, result.getStatus());
    }

    @Test
    @DisplayName("UT toDlqEvent() should always set dlq status to MOVED")
    void toDlqEvent_shouldSetDlqStatusToMoved() {
        // given
        OutboxEvent event = outboxEvent(UUID.randomUUID(), 1);

        // when
        OutboxDlqEvent result = tested.toDlqEvent(event);

        // then
        assertEquals(DlqStatus.MOVED, result.getDlqStatus());
    }

    @Test
    @DisplayName("UT toDlqEvent() should preserve eventType and payloadType")
    void toDlqEvent_shouldPreserveEventTypeAndPayloadType() {
        // given
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), EventStatus.FAILED, "OrderCreated",
                "com.example.OrderCreatedEvent", "{}", 1,
                Instant.now(), Instant.now(), Instant.now()
        );

        // when
        OutboxDlqEvent result = tested.toDlqEvent(event);

        // then
        assertEquals("OrderCreated", result.getEventType());
        assertEquals("com.example.OrderCreatedEvent", result.getPayloadType());
    }

    @Test
    @DisplayName("UT toDlqEvent() should preserve payload")
    void toDlqEvent_shouldPreservePayload() {
        // given
        String payload = "{\"orderId\":\"123\"}";
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), EventStatus.FAILED, "t", "t", payload, 1,
                Instant.now(), Instant.now(), Instant.now()
        );

        // when
        OutboxDlqEvent result = tested.toDlqEvent(event);

        // then
        assertEquals(payload, result.getPayload());
    }

    @Test
    @DisplayName("UT toDlqEvent() should preserve retryCount")
    void toDlqEvent_shouldPreserveRetryCount() {
        // given
        OutboxEvent event = outboxEvent(UUID.randomUUID(), 7);

        // when
        OutboxDlqEvent result = tested.toDlqEvent(event);

        // then
        assertEquals(7, result.getRetryCount());
    }

    @Test
    @DisplayName("UT toDlqEvent() should preserve nextRetryAt, createdAt, updatedAt")
    void toDlqEvent_shouldPreserveTimestamps() {
        // given
        Instant nextRetryAt = Instant.parse("2024-01-01T10:00:00Z");
        Instant createdAt  = Instant.parse("2023-12-01T08:00:00Z");
        Instant updatedAt  = Instant.parse("2023-12-31T22:00:00Z");

        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), EventStatus.FAILED, "t", "t", "{}", 3,
                nextRetryAt, createdAt, updatedAt
        );

        // when
        OutboxDlqEvent result = tested.toDlqEvent(event);

        // then
        assertEquals(nextRetryAt, result.getNextRetryAt());
        assertEquals(createdAt,   result.getCreatedAt());
        assertEquals(updatedAt,   result.getUpdatedAt());
    }

    @Test
    @DisplayName("UT toDlqEvent() should set movedAt to clock instant")
    void toDlqEvent_shouldSetMovedAtToClockInstant() {
        // given
        OutboxEvent event = outboxEvent(UUID.randomUUID(), 1);

        // when
        OutboxDlqEvent result = tested.toDlqEvent(event);

        // then
        assertEquals(FIXED_NOW, result.getMovedAt());
    }

    @Test
    @DisplayName("UT toDlqEvents() should map all events in list")
    void toDlqEvents_shouldMapAllEvents() {
        // given
        List<OutboxEvent> events = List.of(
                outboxEvent(UUID.randomUUID(), 1),
                outboxEvent(UUID.randomUUID(), 2)
        );

        // when
        List<OutboxDlqEvent> result = tested.toDlqEvents(events);

        // then
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("UT toDlqEvents() should return empty list when input is empty")
    void toDlqEvents_whenEmpty_shouldReturnEmptyList() {
        // when
        List<OutboxDlqEvent> result = tested.toDlqEvents(List.of());

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("UT toDlqEvents() should preserve ids of all events")
    void toDlqEvents_shouldPreserveIdsOfAllEvents() {
        // given
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        List<OutboxEvent> events = List.of(outboxEvent(id1, 1), outboxEvent(id2, 3));

        // when
        List<OutboxDlqEvent> result = tested.toDlqEvents(events);

        // then
        assertEquals(id1, result.get(0).getId());
        assertEquals(id2, result.get(1).getId());
    }

    @Test
    @DisplayName("UT toOutboxEvent() should preserve id from dlq event")
    void toOutboxEvent_shouldPreserveId() {
        // given
        UUID id = UUID.randomUUID();
        OutboxDlqEvent dlqEvent = dlqEvent(id, Instant.now());

        // when
        OutboxEvent result = tested.toOutboxEvent(dlqEvent);

        // then
        assertEquals(id, result.getId());
    }

    @Test
    @DisplayName("UT toOutboxEvent() should always set status to PENDING")
    void toOutboxEvent_shouldSetStatusToPending() {
        // given
        OutboxDlqEvent dlqEvent = dlqEvent(UUID.randomUUID(), Instant.now());

        // when
        OutboxEvent result = tested.toOutboxEvent(dlqEvent);

        // then
        assertEquals(EventStatus.PENDING, result.getStatus());
    }

    @Test
    @DisplayName("UT toOutboxEvent() should always reset retryCount to -1")
    void toOutboxEvent_shouldResetRetryCountToMinusOne() {
        // given
        OutboxDlqEvent dlqEvent = dlqEvent(UUID.randomUUID(), Instant.now());

        // when
        OutboxEvent result = tested.toOutboxEvent(dlqEvent);

        // then
        assertEquals(-1, result.getRetryCount());
    }

    @Test
    @DisplayName("UT toOutboxEvent() should preserve original createdAt")
    void toOutboxEvent_shouldPreserveCreatedAt() {
        // given
        Instant originalCreatedAt = Instant.parse("2023-06-15T09:00:00Z");
        OutboxDlqEvent dlqEvent = dlqEvent(UUID.randomUUID(), originalCreatedAt);

        // when
        OutboxEvent result = tested.toOutboxEvent(dlqEvent);

        // then
        assertEquals(originalCreatedAt, result.getCreatedAt());
    }

    @Test
    @DisplayName("UT toOutboxEvent() should set nextRetryAt and updatedAt to clock instant")
    void toOutboxEvent_shouldSetNextRetryAtAndUpdatedAtToClockInstant() {
        // given
        OutboxDlqEvent dlqEvent = dlqEvent(UUID.randomUUID(), Instant.now());

        // when
        OutboxEvent result = tested.toOutboxEvent(dlqEvent);

        // then
        assertEquals(FIXED_NOW, result.getNextRetryAt());
        assertEquals(FIXED_NOW, result.getUpdatedAt());
    }

    @Test
    @DisplayName("UT toOutboxEvent() should preserve eventType and payloadType")
    void toOutboxEvent_shouldPreserveEventTypeAndPayloadType() {
        // given
        OutboxDlqEvent dlqEvent = new OutboxDlqEvent(
                UUID.randomUUID(), EventStatus.FAILED, "PaymentProcessed",
                "com.example.PaymentEvent", "{}", 2,
                Instant.now(), Instant.now(), Instant.now(), DlqStatus.TO_RETRY, Instant.now()
        );

        // when
        OutboxEvent result = tested.toOutboxEvent(dlqEvent);

        // then
        assertEquals("PaymentProcessed", result.getEventType());
        assertEquals("com.example.PaymentEvent", result.getPayloadType());
    }

    @Test
    @DisplayName("UT toOutboxEvent() should preserve payload")
    void toOutboxEvent_shouldPreservePayload() {
        // given
        String payload = "{\"amount\":100}";
        OutboxDlqEvent dlqEvent = new OutboxDlqEvent(
                UUID.randomUUID(), EventStatus.FAILED, "t", "t", payload, 1,
                Instant.now(), Instant.now(), Instant.now(), DlqStatus.TO_RETRY, Instant.now()
        );

        // when
        OutboxEvent result = tested.toOutboxEvent(dlqEvent);

        // then
        assertEquals(payload, result.getPayload());
    }

    @Test
    @DisplayName("UT toOutboxEvents() should map all events in list")
    void toOutboxEvents_shouldMapAllEvents() {
        // given
        List<OutboxDlqEvent> dlqEvents = List.of(
                dlqEvent(UUID.randomUUID(), Instant.now()),
                dlqEvent(UUID.randomUUID(), Instant.now())
        );

        // when
        List<OutboxEvent> result = tested.toOutboxEvents(dlqEvents);

        // then
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("UT toOutboxEvents() should return empty list when input is empty")
    void toOutboxEvents_whenEmpty_shouldReturnEmptyList() {
        // when
        List<OutboxEvent> result = tested.toOutboxEvents(List.of());

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("UT toOutboxEvents() should set PENDING status on all events")
    void toOutboxEvents_shouldSetPendingOnAllEvents() {
        // given
        List<OutboxDlqEvent> dlqEvents = List.of(
                dlqEvent(UUID.randomUUID(), Instant.now()),
                dlqEvent(UUID.randomUUID(), Instant.now()),
                dlqEvent(UUID.randomUUID(), Instant.now())
        );

        // when
        List<OutboxEvent> result = tested.toOutboxEvents(dlqEvents);

        // then
        assertTrue(result.stream().allMatch(e -> e.getStatus() == EventStatus.PENDING));
    }

    @Test
    @DisplayName("UT toOutboxEvents() should reset retryCount to -1 on all events")
    void toOutboxEvents_shouldResetRetryCountOnAllEvents() {
        // given
        List<OutboxDlqEvent> dlqEvents = List.of(
                dlqEvent(UUID.randomUUID(), Instant.now()),
                dlqEvent(UUID.randomUUID(), Instant.now())
        );

        // when
        List<OutboxEvent> result = tested.toOutboxEvents(dlqEvents);

        // then
        assertTrue(result.stream().allMatch(e -> e.getRetryCount() == -1));
    }

    @Test
    @DisplayName("UT toDlqEvent() should return null when input is null")
    void toDlqEvent_whenNull_shouldReturnNull() {
        // when
        OutboxDlqEvent result = tested.toDlqEvent(null);

        // then
        assertNull(result);
    }

    @Test
    @DisplayName("UT toDlqEvents() should return empty list when input is null")
    void toDlqEvents_whenNull_shouldReturnEmptyList() {
        // when
        List<OutboxDlqEvent> result = tested.toDlqEvents(null);

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("UT toDlqEvents() should filter out null elements from list")
    void toDlqEvents_whenContainsNullElements_shouldFilterThemOut() {
        // given
        List<OutboxEvent> events = Arrays.asList(
                outboxEvent(UUID.randomUUID(), 1),
                null,
                outboxEvent(UUID.randomUUID(), 2)
        );

        // when
        List<OutboxDlqEvent> result = tested.toDlqEvents(events);

        // then
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("UT toOutboxEvent() should return null when input is null")
    void toOutboxEvent_whenNull_shouldReturnNull() {
        // when
        OutboxEvent result = tested.toOutboxEvent(null);

        // then
        assertNull(result);
    }

    @Test
    @DisplayName("UT toOutboxEvents() should return empty list when input is null")
    void toOutboxEvents_whenNull_shouldReturnEmptyList() {
        // when
        List<OutboxEvent> result = tested.toOutboxEvents(null);

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("UT toOutboxEvents() should filter out null elements from list")
    void toOutboxEvents_whenContainsNullElements_shouldFilterThemOut() {
        // given
        List<OutboxDlqEvent> dlqEvents = Arrays.asList(
                dlqEvent(UUID.randomUUID(), Instant.now()),
                null,
                dlqEvent(UUID.randomUUID(), Instant.now())
        );

        // when
        List<OutboxEvent> result = tested.toOutboxEvents(dlqEvents);

        // then
        assertEquals(2, result.size());
    }

    private OutboxEvent outboxEvent(UUID id, int retryCount) {
        return new OutboxEvent(
                id, EventStatus.FAILED, "type", "type", "{}", retryCount,
                Instant.now(), Instant.now(), Instant.now()
        );
    }

    private OutboxDlqEvent dlqEvent(UUID id, Instant createdAt) {
        return new OutboxDlqEvent(
                id, EventStatus.FAILED, "type", "type", "{}", 5,
                Instant.now(), createdAt, Instant.now(), DlqStatus.TO_RETRY, Instant.now()
        );
    }
}