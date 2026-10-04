package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.domain.OutboxEvent;

import java.util.List;

/**
 * Handles events that have been moved to the DLQ.
 * Used by {@link OutboxDlqTransfer}.
 */
public interface OutboxDlqHandler {

    /**
     * Handle failed events.
     */
    void handle(List<OutboxEvent> events);
}
