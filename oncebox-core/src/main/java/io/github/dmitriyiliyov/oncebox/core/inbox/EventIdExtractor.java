package io.github.dmitriyiliyov.oncebox.core.inbox;

import java.util.UUID;

/**
 * Strategy interface for extracting the unique ID of an event from a raw message.
 * <p>
 * The ID is the one the publisher assigned when it saved the event to its outbox.
 * {@link DefaultIdempotentConsumer} uses this to read it without knowing the specific message broker implementation.
 *
 * @param <T> The type of the raw message from which to resolve the event ID.
 */
@FunctionalInterface
public interface EventIdExtractor<T> {

    /**
     * Extracts the event ID from the given raw message.
     *
     * @param message the raw message containing the event.
     * @return        the UUID of the event; never {@code null}.
     */
    UUID extract(T message);
}
