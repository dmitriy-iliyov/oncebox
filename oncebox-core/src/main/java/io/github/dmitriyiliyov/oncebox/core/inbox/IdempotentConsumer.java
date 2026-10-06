package io.github.dmitriyiliyov.oncebox.core.inbox;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Ensures idempotent message consumption by tracking processed event IDs.
 * <p>
 * Whether a failed operation is run again on redelivery depends on the implementation: see
 * {@link TransactionalIdempotentConsumer} and {@link NonTransactionalIdempotentConsumer}.
 */
public interface IdempotentConsumer {

    /**
     * Executes the operation only if the event ID has not been processed before.
     * <p>
     * If the event ID has already been consumed, the operation is skipped.
     *
     * @param eventId   the unique identifier of the event.
     * @param operation the business logic to execute if the event is new.
     */
    void consume(UUID eventId, Runnable operation);

    /**
     * Extracts the event ID using the provided extractor and executes the operation
     * only if the message has not been processed before.
     * <p>
     * If the message has already been consumed, the operation is skipped.
     *
     * @param message     the message to consume.
     * @param idExtractor the function to extract the unique identifier from the message.
     * @param operation   the business logic to execute with the message if it is new.
     * @param <T>         the type of the message.
     */
    <T> void consume(T message, EventIdExtractor<T> idExtractor, Consumer<T> operation);

    /**
     * Executes the operation for the subset of event IDs that have not been processed before.
     * <p>
     * Already consumed event IDs are filtered out before the operation is called.
     * If all event IDs have already been consumed, the operation is not called at all.
     * Does nothing if the set is null or empty.
     *
     * @param ids       the set of event IDs to check.
     * @param operation the business logic to execute with the set of new event IDs.
     */
    void consume(Set<UUID> ids, Consumer<Set<UUID>> operation);

    /**
     * Executes the operation for the subset of messages that have not been processed before.
     * <p>
     * The event ID is extracted from each message using the provided extractor.
     * Already consumed messages are filtered out before the operation is called.
     * If all messages have already been consumed, the operation is not called at all.
     * Does nothing if the list is null or empty.
     *
     * @param messages    the list of messages to consume.
     * @param idExtractor the function to extract the unique identifier from each message.
     * @param operation   the business logic to execute with the list of new messages.
     * @param <T>         the type of the messages.
     */
    <T> void consume(List<T> messages, EventIdExtractor<T> idExtractor, Consumer<List<T>> operation);
}
