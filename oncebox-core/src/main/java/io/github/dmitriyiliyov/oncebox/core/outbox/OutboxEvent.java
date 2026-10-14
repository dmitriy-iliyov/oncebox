package io.github.dmitriyiliyov.oncebox.core.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An event of the outbox table.
 * <p>
 * {@code nextRetryAt}, {@code createdAt} and {@code updatedAt} are set by the database clock, and inserts and
 * updates of the outbox table do not write the values an event carries: they hold what was last read from the
 * event's row, and are {@code null} on an event that has not been read from the database, such as one made by
 * {@link #OutboxEvent(UUID, String, String, String)}. The one exception is
 * {@link OutboxRepository#saveBatchWithCreatedAt}, which writes the {@code createdAt} of each event.
 * <p>
 * {@code retryDelay} goes the other way: it is never read from the row, and on a finalize the database counts the
 * next retry time from it; a {@code null} delay leaves the next retry time as it is.
 */
public class OutboxEvent {

    protected UUID id;
    protected EventStatus status;
    protected String eventType;
    protected String payloadType;
    protected String payload;
    protected int retryCount;
    protected Instant nextRetryAt;
    protected Duration retryDelay;
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

    public OutboxEvent(UUID id, String eventType, String payloadType, String payload) {
        this(id, EventStatus.PENDING, eventType, payloadType, payload, -1, null, null, null);
    }

    public OutboxEvent copy() {
        OutboxEvent copy = new OutboxEvent(
                id,
                status,
                eventType,
                payloadType,
                payload,
                retryCount,
                nextRetryAt,
                createdAt,
                updatedAt
        );
        copy.retryDelay = retryDelay;
        return copy;
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

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = retryDelay;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
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
