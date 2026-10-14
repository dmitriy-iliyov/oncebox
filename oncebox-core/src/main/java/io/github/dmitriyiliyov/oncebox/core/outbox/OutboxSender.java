package io.github.dmitriyiliyov.oncebox.core.outbox;

import java.util.List;

/**
 * Abstraction for sending outbox events to a message broker.
 * <p>
 * Implementations of this interface are responsible for publishing events to a specific messaging system
 * (e.g., Kafka, RabbitMQ).
 * This is a key point for supporting new message brokers.
 */
public interface OutboxSender {

    /**
     * Sends a batch of outbox events to a logical channel.
     * <p>
     * The implementation should handle the actual transmission to the broker.
     * It must return a {@link SenderResult} containing the IDs of events that were successfully sent
     * and those that failed. This allows for partial success handling.
     * <p>
     * Every event of the batch is expected in exactly one of the two sets. An event in neither stays
     * {@code IN_PROCESS} until stuck recovery returns it and it is sent again; an event in both is treated as
     * failed. Throwing instead of returning marks the whole batch as failed.
     *
     * @param topic  logical channel for events; represents a Kafka topic, RabbitMQ exchange, or any other event
     *               stream destination.
     * @param events list of events to send.
     * @return       {@link SenderResult} containing sets of processed and failed event IDs.
     */
    SenderResult sendEvents(String topic, List<OutboxEvent> events);
}
