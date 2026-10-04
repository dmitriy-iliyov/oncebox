package io.github.dmitriyiliyov.oncebox.core.outbox;

import java.util.List;

/**
 * Provides a high-level API for publishing events to the outbox.
 * <p>
 * This is the primary entry point for application code to manually send events.
 */
public interface OutboxPublisher {

    /**
     * Publishes a single event to the outbox.
     * <p>
     * The event is serialized and saved to the outbox table.
     * The caller must hold an active transaction: the event is committed or rolled back together with the
     * change that produced it.
     *
     * @param eventType the type of the event.
     * @param event     the event payload.
     * @param <T>       the type of the event payload.
     * @throws IllegalArgumentException if {@code eventType} is blank or not configured.
     */
    <T> void publish(String eventType, T event);

    /**
     * Publishes a list of events to the outbox.
     * <p>
     * The caller must hold an active transaction: the events are committed or rolled back together with the
     * change that produced them.
     * Does nothing if the list is empty.
     *
     * @param eventType the type of the events.
     * @param events    a list of event payloads.
     * @param <T>       the type of the event payloads.
     * @throws IllegalArgumentException if {@code eventType} is blank or not configured.
     */
    <T> void publish(String eventType, List<T> events);
}
