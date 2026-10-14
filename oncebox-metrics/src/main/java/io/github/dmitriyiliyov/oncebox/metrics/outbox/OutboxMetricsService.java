package io.github.dmitriyiliyov.oncebox.metrics.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;

/**
 * A service for retrieving metrics about outbox events.
 */
public interface OutboxMetricsService {

    long count();

    long countByStatus(EventStatus status);

    long countByEventTypeAndStatus(String eventType, EventStatus status);
}
