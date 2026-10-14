package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;

import java.util.List;

/**
 * Converts events between the outbox and the DLQ.
 * <p>
 * A {@code null} event maps to {@code null}; a {@code null} or empty list maps to an empty one, and {@code null}
 * elements are left out.
 */
public interface OutboxDlqEventMapper {

    /**
     * Makes the DLQ event an outbox event is moved as: {@link io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus#FAILED}
     * in {@link DlqStatus#MOVED}, under the same id.
     */
    OutboxDlqEvent toDlqEvent(OutboxEvent event);

    List<OutboxDlqEvent> toDlqEvents(List<OutboxEvent> events);

    /**
     * Makes the outbox event a DLQ event is brought back as: a
     * {@link io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus#PENDING} event under the same id with its
     * retries started over. It keeps the creation time of the event, which is why it is saved by
     * {@link io.github.dmitriyiliyov.oncebox.core.outbox.OutboxManager#saveBatchWithCreatedAt}; the next retry and
     * update times are left to the database.
     */
    OutboxEvent toOutboxEvent(OutboxDlqEvent event);

    List<OutboxEvent> toOutboxEvents(List<OutboxDlqEvent> events);
}
