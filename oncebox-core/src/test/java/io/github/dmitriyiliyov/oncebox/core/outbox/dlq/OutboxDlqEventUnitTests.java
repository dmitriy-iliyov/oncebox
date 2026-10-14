package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OutboxDlqEventUnitTests {

    @Test
    @DisplayName("UT Constructor should map all superclass and subclass fields correctly")
    void constructor_mapsAllFields() {
        UUID id = UUID.randomUUID();
        EventStatus status = EventStatus.FAILED;
        String eventType = "TEST_EVENT";
        String payloadType = "application/json";
        String payload = "{}";
        int retryCount = 3;
        Instant nextRetryAt = Instant.now().plusSeconds(60);
        Instant createdAt = Instant.now().minusSeconds(120);
        Instant updatedAt = Instant.now();
        DlqStatus dlqStatus = Mockito.mock(DlqStatus.class);
        Instant movedAt = Instant.now().plusMillis(100);

        OutboxDlqEvent event = new OutboxDlqEvent(
                id, status, eventType, payloadType, payload,
                retryCount, nextRetryAt, createdAt, updatedAt,
                dlqStatus, movedAt
        );

        assertThat(event.getId()).isEqualTo(id);
        assertThat(event.getStatus()).isEqualTo(status);
        assertThat(event.getEventType()).isEqualTo(eventType);
        assertThat(event.getPayloadType()).isEqualTo(payloadType);
        assertThat(event.getPayload()).isEqualTo(payload);
        assertThat(event.getRetryCount()).isEqualTo(retryCount);
        assertThat(event.getNextRetryAt()).isEqualTo(nextRetryAt);
        assertThat(event.getCreatedAt()).isEqualTo(createdAt);
        assertThat(event.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(event.getDlqStatus()).isEqualTo(dlqStatus);
        assertThat(event.getMovedAt()).isEqualTo(movedAt);
    }

    @Test
    @DisplayName("UT Setters should correctly update mutable fields")
    void setters_updateFields() {
        OutboxDlqEvent event = new OutboxDlqEvent(
                UUID.randomUUID(), EventStatus.FAILED, "TYPE", "JSON", "{}",
                0, Instant.now(), Instant.now(), Instant.now(),
                Mockito.mock(DlqStatus.class), Instant.now()
        );

        DlqStatus newDlqStatus = Mockito.mock(DlqStatus.class);
        event.setDlqStatus(newDlqStatus);

        assertThat(event.getDlqStatus()).isEqualTo(newDlqStatus);
    }

    @Test
    @DisplayName("UT equals() should return true for the exact same instance")
    void equals_sameInstance_returnsTrue() {
        OutboxDlqEvent event = new OutboxDlqEvent(
                UUID.randomUUID(), EventStatus.FAILED, "TYPE", "JSON", "{}",
                0, Instant.now(), Instant.now(), Instant.now(),
                Mockito.mock(DlqStatus.class), Instant.now()
        );

        assertThat(event).isEqualTo(event);
    }

    @Test
    @DisplayName("UT equals() should return false for null or different class")
    void equals_nullOrDifferentClass_returnsFalse() {
        OutboxDlqEvent event = new OutboxDlqEvent(
                UUID.randomUUID(), EventStatus.FAILED, "TYPE", "JSON", "{}",
                0, Instant.now(), Instant.now(), Instant.now(),
                Mockito.mock(DlqStatus.class), Instant.now()
        );

        assertThat(event).isNotEqualTo(null);
        assertThat(event).isNotEqualTo("StringObject");
    }

    @Test
    @DisplayName("UT equals() should return true when IDs are identical")
    void equals_sameId_returnsTrue() {
        UUID sharedId = UUID.randomUUID();
        Instant now = Instant.now();
        DlqStatus statusMock = Mockito.mock(DlqStatus.class);

        OutboxDlqEvent event1 = new OutboxDlqEvent(
                sharedId, EventStatus.FAILED, "TYPE1", "JSON", "{}",
                0, now, now, now, statusMock, now
        );

        OutboxDlqEvent event2 = new OutboxDlqEvent(
                sharedId, EventStatus.PENDING, "TYPE2", "XML", "<xml/>",
                5, now.plusSeconds(10), now, now, statusMock, now.plusSeconds(5)
        );

        assertThat(event2).isEqualTo(event1);
    }

    @Test
    @DisplayName("UT equals() should return false when IDs are different")
    void equals_differentId_returnsFalse() {
        Instant now = Instant.now();
        DlqStatus statusMock = Mockito.mock(DlqStatus.class);

        OutboxDlqEvent event1 = new OutboxDlqEvent(
                UUID.randomUUID(), EventStatus.FAILED, "TYPE", "JSON", "{}",
                0, now, now, now, statusMock, now
        );

        OutboxDlqEvent event2 = new OutboxDlqEvent(
                UUID.randomUUID(), EventStatus.FAILED, "TYPE", "JSON", "{}",
                0, now, now, now, statusMock, now
        );

        assertThat(event2).isNotEqualTo(event1);
    }

    @Test
    @DisplayName("UT hashCode() should return identical hash codes for objects with the same ID")
    void hashCode_sameId_returnsIdenticalHash() {
        UUID sharedId = UUID.randomUUID();
        Instant now = Instant.now();
        DlqStatus statusMock = Mockito.mock(DlqStatus.class);

        OutboxDlqEvent event1 = new OutboxDlqEvent(
                sharedId, EventStatus.FAILED, "TYPE1", "JSON", "{}",
                0, now, now, now, statusMock, now
        );

        OutboxDlqEvent event2 = new OutboxDlqEvent(
                sharedId, EventStatus.PENDING, "TYPE2", "XML", "<xml/>",
                5, now.plusSeconds(10), now, now, statusMock, now.plusSeconds(5)
        );

        assertThat(event2.hashCode()).isEqualTo(event1.hashCode());
    }

    @Test
    @DisplayName("UT moved() should make a FAILED event in MOVED that keeps the times of the outbox event")
    void moved_whenOutboxEventIsGiven_shouldKeepItsTimesAndLeaveMovedAtToDatabase() {
        // given
        Instant nextRetryAt = Instant.parse("2026-01-01T10:05:00Z");
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-01T10:01:00Z");
        OutboxEvent event = new OutboxEvent(MOVED_ID, EventStatus.IN_PROCESS, "type", "payloadType", "{}", 3,
                nextRetryAt, createdAt, updatedAt);

        // when
        OutboxDlqEvent tested = OutboxDlqEvent.moved(event);

        // then
        assertThat(tested.getId()).isEqualTo(MOVED_ID);
        assertThat(tested.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(tested.getDlqStatus()).isEqualTo(DlqStatus.MOVED);
        assertThat(tested.getRetryCount()).isEqualTo(3);
        assertThat(tested.getNextRetryAt()).isEqualTo(nextRetryAt);
        assertThat(tested.getCreatedAt()).isEqualTo(createdAt);
        assertThat(tested.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(tested.getMovedAt()).isNull();
    }

    @Test
    @DisplayName("UT copy() should stay a DLQ event and carry its DLQ status and movedAt")
    void copy_whenDlqEventIsCopied_shouldStayDlqEventWithItsOwnFields() {
        // given
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        OutboxDlqEvent tested = new OutboxDlqEvent(MOVED_ID, EventStatus.FAILED, "type", "payloadType", "{}", 3,
                now, now, now, DlqStatus.TO_RETRY, now);

        // when
        OutboxDlqEvent copy = tested.copy();

        // then
        assertThat(copy).isNotSameAs(tested).usingRecursiveComparison().isEqualTo(tested);
        assertThat(copy.getDlqStatus()).isEqualTo(DlqStatus.TO_RETRY);
        assertThat(copy.getMovedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("UT copy() when called through the outbox type should still give a DLQ event")
    void copy_whenCalledThroughOutboxType_shouldStillGiveDlqEvent() {
        // given
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        OutboxEvent tested = new OutboxDlqEvent(MOVED_ID, EventStatus.FAILED, "type", "payloadType", "{}", 3,
                now, now, now, DlqStatus.MOVED, now);

        // when
        OutboxEvent copy = tested.copy();

        // then
        assertThat(copy).isInstanceOf(OutboxDlqEvent.class);
    }

    private static final UUID MOVED_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
}
