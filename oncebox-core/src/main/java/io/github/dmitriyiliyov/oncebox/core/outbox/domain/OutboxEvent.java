package io.github.dmitriyiliyov.oncebox.core.outbox.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class OutboxEvent {

    protected UUID id;
    protected EventStatus status;
    protected String eventType;
    protected String payloadType;
    protected String payload;
    protected int retryCount;
    protected Instant nextRetryAt;
    protected Instant createdAt;
    protected Instant updatedAt;

    public OutboxEvent(UUID id, EventStatus status, String eventType, String payloadType, String payload,
                       int retryCount, Instant nextRetryAt, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.status = status;
        this.eventType = eventType;
        this.payloadType = payloadType;
        this.payload = payload;
        this.retryCount = retryCount;
        this.nextRetryAt = nextRetryAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public OutboxEvent(UUID id, String eventType, String payloadType, String payload, Instant createdAt) {
        this(id, EventStatus.PENDING, eventType, payloadType, payload, -1, createdAt, createdAt, createdAt);
    }


    public UUID getId() {
        return id;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public String getPayload() {
        return payload;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutboxEvent that = (OutboxEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
