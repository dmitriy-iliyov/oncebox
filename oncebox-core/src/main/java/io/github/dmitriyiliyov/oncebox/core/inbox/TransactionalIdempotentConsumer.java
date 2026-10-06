package io.github.dmitriyiliyov.oncebox.core.inbox;

import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Consumer;

/**
 * {@link IdempotentConsumer} with effectively-once processing.
 * <p>
 * The ID is recorded in the same transaction as the operation: if the operation throws, the record is rolled
 * back with it and the exception propagates, so a redelivered event runs the operation again. The effect of an
 * operation is kept once only if it is written to the same database in that transaction: an external call made
 * by the operation, such as sending an email, is repeated when the transaction does not commit.
 */
public class TransactionalIdempotentConsumer extends AbstractIdempotentConsumer {

    private final TransactionTemplate transactionTemplate;

    public TransactionalIdempotentConsumer(TransactionTemplate transactionTemplate,
                                           InboxManager inboxManager) {
        super(inboxManager);
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate cannot be null");
    }

    @Override
    protected void consumeAndRunOperation(UUID eventId, Runnable operation) {
        transactionTemplate.executeWithoutResult(status -> {
            super.consumeAndRunOperation(eventId, operation);
        });
    }

    @Override
    protected <T> void consumeAndRunOperation(T message, UUID eventId, Consumer<T> operation) {
        transactionTemplate.executeWithoutResult(status -> {
            super.consumeAndRunOperation(message, eventId, operation);
        });
    }

    @Override
    protected void consumeAndRunOperation(Set<UUID> ids, Consumer<Set<UUID>> operation) {
        transactionTemplate.executeWithoutResult(status -> {
            super.consumeAndRunOperation(ids, operation);
        });
    }

    @Override
    protected <T> void consumeAndRunOperation(List<T> messages, Map<UUID, T> messageMap, Consumer<List<T>> operation) {
        transactionTemplate.executeWithoutResult(status -> {
            super.consumeAndRunOperation(messages, messageMap, operation);
        });
    }
}
