package io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;

/**
 * A service for retrieving metrics about DLQ events.
 */
public interface OutboxDlqMetricsService {

    /**
     * Counts the total number of DLQ events.
     */
    long count();

    /**
     * Counts the number of DLQ events with a specific status.
     */
    long countByStatus(DlqStatus status);

    /**
     * Counts the number of DLQ events with a specific event type and status.
     *
     * @param eventType type of the DLQ events to count.
     * @param status    status of the DLQ events to count.
     * @return          the count of DLQ events matching the criteria.
     */
    long countByEventTypeAndStatus(String eventType, DlqStatus status);
}
