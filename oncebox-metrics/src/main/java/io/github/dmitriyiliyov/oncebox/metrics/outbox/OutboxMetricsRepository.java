package io.github.dmitriyiliyov.oncebox.metrics.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.domain.EventStatus;

/**
 * DAO for retrieving outbox event metrics.
 */
public interface OutboxMetricsRepository {

    /**
     * Counts the total number of outbox events in the repository.
     */
    long count();

    /**
     * Counts the number of outbox events with a specific status.
     */
    long countByStatus(EventStatus status);

    /**
     * Counts the number of outbox events with a specific event type and status.
     *
     * @param eventType type of the events to count.
     * @param status    status of the events to count.
     * @return          the count of events matching the criteria.
     */
    long countByEventTypeAndStatus(String eventType, EventStatus status);
}
