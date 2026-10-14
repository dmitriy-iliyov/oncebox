package io.github.dmitriyiliyov.oncebox.metrics.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;

/**
 * DAO for retrieving outbox event metrics.
 */
public interface OutboxMetricsRepository {

    long count();

    long countByStatus(EventStatus status);

    long countByEventTypeAndStatus(String eventType, EventStatus status);
}
