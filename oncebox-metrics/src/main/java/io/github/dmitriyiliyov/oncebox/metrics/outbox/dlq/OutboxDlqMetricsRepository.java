package io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;

/**
 * DAO for retrieving DLQ event metrics.
 */
public interface OutboxDlqMetricsRepository {

    /**
     * Counts the total number of DLQ events in the repository.
     */
    long count();

    /**
     * Counts the number of DLQ events with a specific status.
     */
    long countByStatus(DlqStatus status);

    /**
     * Counts the number of DLQ events with a specific event type and status.
     *
     * @param eventType type of events to count.
     * @param status    status of the DLQ events to count.
     * @return          count of DLQ events matching the criteria.
     */
    long countByEventTypeAndStatus(String eventType, DlqStatus status);
}
