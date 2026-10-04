package io.github.dmitriyiliyov.oncebox.metrics.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.domain.EventStatus;

/**
 * A service for retrieving metrics about outbox events.
 */
public interface OutboxMetricsService {

    /**
     * Counts the total number of outbox events.
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
