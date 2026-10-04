package io.github.dmitriyiliyov.oncebox.core.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.domain.OutboxEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class OutboxEventUnitTests {

    @Test
    @DisplayName("UT Constructor (New Event) should initialize default fields correctly")
    void constructorNewEvent_initializesCorrectly() {
        UUID id = UUID.randomUUID();
        String eventType = "USER_CREATED";
        String payloadType = "application/json";
        String payload = "{\"userId\": 1}";
        Instant now = Instant.now();

        OutboxEvent event = new OutboxEvent(id, eventType, payloadType, payload, now);

        assertEquals(id, event.getId());
        assertEquals(eventType, event.getEventType());
        assertEquals(payloadType, event.getPayloadType());
        assertEquals(payload, event.getPayload());
        assertEquals(now, event.getCreatedAt());

        assertEquals(EventStatus.PENDING, event.getStatus());
        assertEquals(-1, event.getRetryCount());
        assertEquals(now, event.getNextRetryAt());
        assertEquals(now, event.getUpdatedAt());
    }

    @Test
    @DisplayName("UT Constructor (Full) should map all fields correctly")
    void constructorFull_mapsAllFields() {
        UUID id = UUID.randomUUID();
        EventStatus status = EventStatus.FAILED;
        String eventType = "USER_UPDATED";
        String payloadType = "application/xml";
        String payload = "<user><id>1</id></user>";
        int retryCount = 5;
        Instant nextRetryAt = Instant.now().plusSeconds(60);
        Instant createdAt = Instant.now().minusSeconds(120);
        Instant updatedAt = Instant.now();

        OutboxEvent event = new OutboxEvent(id, status, eventType, payloadType, payload, retryCount, nextRetryAt, createdAt, updatedAt);

        assertEquals(id, event.getId());
        assertEquals(status, event.getStatus());
        assertEquals(eventType, event.getEventType());
        assertEquals(payloadType, event.getPayloadType());
        assertEquals(payload, event.getPayload());
        assertEquals(retryCount, event.getRetryCount());
        assertEquals(nextRetryAt, event.getNextRetryAt());
        assertEquals(createdAt, event.getCreatedAt());
        assertEquals(updatedAt, event.getUpdatedAt());
    }

    @Test
    @DisplayName("UT Constructor should throw when id is null")
    void constructorFull_whenIdIsNull_shouldThrows() {
        UUID id = null;
        EventStatus status = EventStatus.FAILED;
        String eventType = "USER_UPDATED";
        String payloadType = "application/xml";
        String payload = "<user><id>1</id></user>";
        int retryCount = 5;
        Instant nextRetryAt = Instant.now().plusSeconds(60);
        Instant createdAt = Instant.now().minusSeconds(120);
        Instant updatedAt = Instant.now();

        assertThrows(
                NullPointerException.class,
                () -> new OutboxEvent(id, status, eventType, payloadType, payload, retryCount, nextRetryAt, createdAt, updatedAt)
        );
    }

    @Test
    @DisplayName("UT Setters should update mutable fields")
    void setters_updateFields() {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), "TYPE", "JSON", "{}", Instant.now());

        Instant newUpdatedAt = Instant.now().plusSeconds(10);

        event.setStatus(EventStatus.PROCESSED);
        event.setUpdatedAt(newUpdatedAt);

        assertEquals(EventStatus.PROCESSED, event.getStatus());
        assertEquals(newUpdatedAt, event.getUpdatedAt());
    }

    @Test
    @DisplayName("UT equals() should return true for the exact same instance")
    void equals_sameInstance_returnsTrue() {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), "TYPE", "JSON", "{}", Instant.now());
        assertEquals(event, event);
    }

    @Test
    @DisplayName("UT equals() should return false for null or different class")
    void equals_nullOrDifferentClass_returnsFalse() {
        OutboxEvent event = new OutboxEvent(UUID.randomUUID(), "TYPE", "JSON", "{}", Instant.now());

        assertNotEquals(null, event);
        assertNotEquals("Some String", event);
    }

    @Test
    @DisplayName("UT equals() should return true if IDs are equal, ignoring other fields")
    void equals_sameIdDifferentFields_returnsTrue() {
        UUID sharedId = UUID.randomUUID();
        Instant now = Instant.now();

        OutboxEvent event1 = new OutboxEvent(sharedId, "TYPE1", "JSON", "{}", now);
        OutboxEvent event2 = new OutboxEvent(sharedId, EventStatus.FAILED, "TYPE2", "XML", "<xml/>", 2, now, now, now);

        assertEquals(event1, event2);
    }

    @Test
    @DisplayName("UT equals() should return false if IDs are different")
    void equals_differentId_returnsFalse() {
        Instant now = Instant.now();
        OutboxEvent event1 = new OutboxEvent(UUID.randomUUID(), "TYPE", "JSON", "{}", now);
        OutboxEvent event2 = new OutboxEvent(UUID.randomUUID(), "TYPE", "JSON", "{}", now);

        assertNotEquals(event1, event2);
    }

    @Test
    @DisplayName("UT hashCode() should be equal if IDs are equal")
    void hashCode_sameId_returnsSameHash() {
        UUID sharedId = UUID.randomUUID();
        Instant now = Instant.now();

        OutboxEvent event1 = new OutboxEvent(sharedId, "TYPE1", "JSON", "{}", now);
        OutboxEvent event2 = new OutboxEvent(sharedId, EventStatus.FAILED, "TYPE2", "XML", "<xml/>", 2, now, now, now);

        assertEquals(event1.hashCode(), event2.hashCode());
    }
}
