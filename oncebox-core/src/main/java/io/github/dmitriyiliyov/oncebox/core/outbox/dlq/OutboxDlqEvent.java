package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An event of the DLQ table.
 * <p>
 * Unlike in the outbox table, {@code nextRetryAt}, {@code createdAt} and {@code updatedAt} are written on insert:
 * they are copied from the {@link OutboxEvent} the event was moved from. {@code movedAt} is set by the database
 * clock and is {@code null} on an event that has not been read from the database, such as one made by
 * {@link #moved}.
 */
public class OutboxDlqEvent extends OutboxEvent {

    private DlqStatus dlqStatus;
    private Instant movedAt;

    public OutboxDlqEvent(UUID id, EventStatus status, String eventType, String payloadType, String payload,
                          int retryCount, Instant nextRetryAt, Instant createdAt, Instant updatedAt, DlqStatus dlqStatus,
                          Instant movedAt) {
        super(id, status, eventType, payloadType, payload, retryCount, nextRetryAt, createdAt, updatedAt);
        this.dlqStatus = dlqStatus;
        this.movedAt = movedAt;
    }

    public static OutboxDlqEvent moved(OutboxEvent event) {
        return new OutboxDlqEvent(
                event.getId(),
                EventStatus.FAILED,
                event.getEventType(),
                event.getPayloadType(),
                event.getPayload(),
                event.getRetryCount(),
                event.getNextRetryAt(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                DlqStatus.MOVED,
                null
        );
    }

    @Override
    public OutboxDlqEvent copy() {
        OutboxDlqEvent copy = new OutboxDlqEvent(
                id,
                status,
                eventType,
                payloadType,
                payload,
                retryCount,
                nextRetryAt,
                createdAt,
                updatedAt,
                dlqStatus,
                movedAt
        );
        copy.retryDelay = retryDelay;
        return copy;
    }

    public DlqStatus getDlqStatus() {
        return dlqStatus;
    }

    public void setDlqStatus(DlqStatus dlqStatus) {
        this.dlqStatus = dlqStatus;
    }

    public Instant getMovedAt() {
        return movedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutboxDlqEvent that = (OutboxDlqEvent) o;
        return Objects.equals(id, that.getId());
    }
}