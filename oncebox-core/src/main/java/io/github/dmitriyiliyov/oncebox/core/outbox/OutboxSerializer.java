package io.github.dmitriyiliyov.oncebox.core.outbox;

import java.util.List;

/**
 * Abstraction for serializing application-specific events into a generic OutboxEvent format.
 * <p>
 * Implementations are responsible for converting event payloads
 * into a storable string representation and populating metadata
 * such as event type and payload type.
 */
public interface OutboxSerializer {

    /**
     * Serializes a single application-specific event into an OutboxEvent.
     *
     * @param eventType the type of the event.
     * @param event     the application-specific event object.
     * @param <T>       the type of the application-specific event object.
     * @return          the serialized OutboxEvent.
     * @throws OutboxSerializationException if the event cannot be serialized.
     */
    <T> OutboxEvent serialize(String eventType, T event);

    /**
     * Serializes a list of application-specific events into a list of OutboxEvents.
     *
     * @param eventType the type of the events.
     * @param rowEvents a list of application-specific event objects.
     * @param <T>       the type of the application-specific event objects.
     * @return          a list of serialized OutboxEvents.
     * @throws OutboxSerializationException if any of the events cannot be serialized.
     */
    <T> List<OutboxEvent> serialize(String eventType, List<T> rowEvents);
}
