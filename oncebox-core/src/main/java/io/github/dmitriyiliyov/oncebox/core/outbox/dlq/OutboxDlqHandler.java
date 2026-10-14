package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;

import java.util.List;

/**
 * Handles events that have been moved to the DLQ.
 * Used by {@link OutboxDlqTransfer}.
 */
public interface OutboxDlqHandler {

    /**
     * Called with the events of a batch once their move to the DLQ has committed. An exception thrown here is
     * logged and does not undo the move.
     */
    void handle(List<OutboxEvent> events);
}
