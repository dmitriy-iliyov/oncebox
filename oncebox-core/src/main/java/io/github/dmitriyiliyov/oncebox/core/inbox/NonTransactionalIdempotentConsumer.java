package io.github.dmitriyiliyov.oncebox.core.inbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

/**
 * {@link IdempotentConsumer} with at-most-once processing.
 * <p>
 * The ID is committed before the operation runs and outside of it, so it is not rolled back when the operation
 * throws: the exception propagates, and a redelivered event is skipped as already consumed. An operation is
 * therefore never run twice for one event, but an event whose operation failed is not processed again. Fits an
 * operation that cannot be undone and must not be repeated, such as sending an email.
 * <p>
 * Called inside a transaction of the caller, the ID joins that transaction and is rolled back with it.
 */
public class NonTransactionalIdempotentConsumer extends AbstractIdempotentConsumer {

    private static final Logger log = LoggerFactory.getLogger(NonTransactionalIdempotentConsumer.class);

    public NonTransactionalIdempotentConsumer(InboxManager manager) {
        super(manager);
    }

    @Override
    protected void consumeAndRunOperation(UUID eventId, Runnable operation) {
        super.consumeAndRunOperation(eventId, () -> runRecorded(Set.of(eventId), operation));
    }

    @Override
    protected <T> void consumeAndRunOperation(T message, UUID eventId, Consumer<T> operation) {
        super.consumeAndRunOperation(
                message,
                eventId,
                m -> runRecorded(Set.of(eventId), () -> operation.accept(m))
        );
    }

    @Override
    protected void consumeAndRunOperation(Set<UUID> ids, Consumer<Set<UUID>> operation) {
        super.consumeAndRunOperation(
                ids,
                validIds -> runRecorded(validIds, () -> operation.accept(validIds))
        );
    }

    @Override
    protected <T> void consumeAndRunOperation(List<T> messages, Map<UUID, T> messageMap, Consumer<List<T>> operation) {
        super.consumeAndRunOperation(
                messages,
                messageMap,
                validMessages -> runRecorded(messageMap.keySet(), () -> operation.accept(validMessages))
        );
    }

    private static void runRecorded(Collection<UUID> recordedIds, Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException | Error e) {
            log.error(
                    "Operation failed after {} event ids were recorded as consumed, they will not be processed again: {}",
                    recordedIds.size(), LogUtils.preview(recordedIds)
            );
            throw e;
        }
    }
}
