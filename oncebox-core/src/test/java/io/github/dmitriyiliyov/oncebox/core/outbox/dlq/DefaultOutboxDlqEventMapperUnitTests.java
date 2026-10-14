package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


public class DefaultOutboxDlqEventMapperUnitTests {

    private DefaultOutboxDlqEventMapper tested;

    @BeforeEach
    void setUp() {
        tested = new DefaultOutboxDlqEventMapper();
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
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("UT toDlqEvent() should always set status to FAILED")
    void toDlqEvent_shouldSetStatusToFailed() {
        // given
        OutboxEvent event = outboxEvent(UUID.randomUUID(), 1);

        // when
        OutboxDlqEvent result = tested.toDlqEvent(event);

        // then
        assertThat(result.getStatus()).isEqualTo(EventStatus.FAILED);
    }

    @Test
    @DisplayName("UT toDlqEvent() should always set dlq status to MOVED")
    void toDlqEvent_shouldSetDlqStatusToMoved() {
        // given
        OutboxEvent event = outboxEvent(UUID.randomUUID(), 1);

        // when
        OutboxDlqEvent result = tested.toDlqEvent(event);

        // then
        assertThat(result.getDlqStatus()).isEqualTo(DlqStatus.MOVED);
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
        assertThat(result.getEventType()).isEqualTo("OrderCreated");
        assertThat(result.getPayloadType()).isEqualTo("com.example.OrderCreatedEvent");
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
        assertThat(result.getPayload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("UT toDlqEvent() should preserve retryCount")
    void toDlqEvent_shouldPreserveRetryCount() {
        // given
        OutboxEvent event = outboxEvent(UUID.randomUUID(), 7);

        // when
        OutboxDlqEvent result = tested.toDlqEvent(event);

        // then
        assertThat(result.getRetryCount()).isEqualTo(7);
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
        assertThat(result.getNextRetryAt()).isEqualTo(nextRetryAt);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("UT toDlqEvent() should leave movedAt to the database")
    void toDlqEvent_whenMapped_shouldLeaveMovedAtToDatabase() {
        // given
        OutboxEvent event = outboxEvent(UUID.randomUUID(), 1);

        // when
        OutboxDlqEvent result = tested.toDlqEvent(event);

        // then
        assertThat(result.getMovedAt()).isNull();
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
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("UT toDlqEvents() should return empty list when input is empty")
    void toDlqEvents_whenEmpty_shouldReturnEmptyList() {
        // when
        List<OutboxDlqEvent> result = tested.toDlqEvents(List.of());

        // then
        assertThat(result.isEmpty()).isTrue();
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
        assertThat(result.get(0).getId()).isEqualTo(id1);
        assertThat(result.get(1).getId()).isEqualTo(id2);
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
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("UT toOutboxEvent() should always set status to PENDING")
    void toOutboxEvent_shouldSetStatusToPending() {
        // given
        OutboxDlqEvent dlqEvent = dlqEvent(UUID.randomUUID(), Instant.now());

        // when
        OutboxEvent result = tested.toOutboxEvent(dlqEvent);

        // then
        assertThat(result.getStatus()).isEqualTo(EventStatus.PENDING);
    }

    @Test
    @DisplayName("UT toOutboxEvent() should always reset retryCount to -1")
    void toOutboxEvent_shouldResetRetryCountToMinusOne() {
        // given
        OutboxDlqEvent dlqEvent = dlqEvent(UUID.randomUUID(), Instant.now());

        // when
        OutboxEvent result = tested.toOutboxEvent(dlqEvent);

        // then
        assertThat(result.getRetryCount()).isEqualTo(-1);
    }

    @Test
    @DisplayName("UT toOutboxEvent() should keep the creation time and leave the next retry and update times to the database")
    void toOutboxEvent_whenMapped_shouldKeepCreatedAtAndLeaveOtherTimesToDatabase() {
        // given
        Instant createdAt = Instant.parse("2023-06-15T09:00:00Z");
        OutboxDlqEvent dlqEvent = dlqEvent(UUID.randomUUID(), createdAt);

        // when
        OutboxEvent result = tested.toOutboxEvent(dlqEvent);

        // then
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getNextRetryAt()).isNull();
        assertThat(result.getUpdatedAt()).isNull();
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
        assertThat(result.getEventType()).isEqualTo("PaymentProcessed");
        assertThat(result.getPayloadType()).isEqualTo("com.example.PaymentEvent");
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
        assertThat(result.getPayload()).isEqualTo(payload);
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
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("UT toOutboxEvents() should return empty list when input is empty")
    void toOutboxEvents_whenEmpty_shouldReturnEmptyList() {
        // when
        List<OutboxEvent> result = tested.toOutboxEvents(List.of());

        // then
        assertThat(result.isEmpty()).isTrue();
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
        assertThat(result.stream().allMatch(e -> e.getStatus() == EventStatus.PENDING)).isTrue();
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
        assertThat(result.stream().allMatch(e -> e.getRetryCount() == -1)).isTrue();
    }

    @Test
    @DisplayName("UT toDlqEvent() should return null when input is null")
    void toDlqEvent_whenNull_shouldReturnNull() {
        // when
        OutboxDlqEvent result = tested.toDlqEvent(null);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("UT toDlqEvents() should return empty list when input is null")
    void toDlqEvents_whenNull_shouldReturnEmptyList() {
        // when
        List<OutboxDlqEvent> result = tested.toDlqEvents(null);

        // then
        assertThat(result.isEmpty()).isTrue();
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
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("UT toOutboxEvent() should return null when input is null")
    void toOutboxEvent_whenNull_shouldReturnNull() {
        // when
        OutboxEvent result = tested.toOutboxEvent(null);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("UT toOutboxEvents() should return empty list when input is null")
    void toOutboxEvents_whenNull_shouldReturnEmptyList() {
        // when
        List<OutboxEvent> result = tested.toOutboxEvents(null);

        // then
        assertThat(result.isEmpty()).isTrue();
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
        assertThat(result.size()).isEqualTo(2);
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