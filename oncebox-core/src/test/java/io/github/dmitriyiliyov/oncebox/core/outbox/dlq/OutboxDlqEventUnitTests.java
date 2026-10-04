package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.domain.EventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

        assertEquals(id, event.getId());
        assertEquals(status, event.getStatus());
        assertEquals(eventType, event.getEventType());
        assertEquals(payloadType, event.getPayloadType());
        assertEquals(payload, event.getPayload());
        assertEquals(retryCount, event.getRetryCount());
        assertEquals(nextRetryAt, event.getNextRetryAt());
        assertEquals(createdAt, event.getCreatedAt());
        assertEquals(updatedAt, event.getUpdatedAt());
        assertEquals(dlqStatus, event.getDlqStatus());
        assertEquals(movedAt, event.getMovedAt());
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

        assertEquals(newDlqStatus, event.getDlqStatus());
    }

    @Test
    @DisplayName("UT equals() should return true for the exact same instance")
    void equals_sameInstance_returnsTrue() {
        OutboxDlqEvent event = new OutboxDlqEvent(
                UUID.randomUUID(), EventStatus.FAILED, "TYPE", "JSON", "{}",
                0, Instant.now(), Instant.now(), Instant.now(),
                Mockito.mock(DlqStatus.class), Instant.now()
        );

        assertEquals(event, event);
    }

    @Test
    @DisplayName("UT equals() should return false for null or different class")
    void equals_nullOrDifferentClass_returnsFalse() {
        OutboxDlqEvent event = new OutboxDlqEvent(
                UUID.randomUUID(), EventStatus.FAILED, "TYPE", "JSON", "{}",
                0, Instant.now(), Instant.now(), Instant.now(),
                Mockito.mock(DlqStatus.class), Instant.now()
        );

        assertNotEquals(null, event);
        assertNotEquals("StringObject", event);
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

        assertEquals(event1, event2);
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

        assertNotEquals(event1, event2);
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

        assertEquals(event1.hashCode(), event2.hashCode());
    }
}
