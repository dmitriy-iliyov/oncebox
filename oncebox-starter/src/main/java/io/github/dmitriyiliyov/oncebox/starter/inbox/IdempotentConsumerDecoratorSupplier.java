package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.core.inbox.AbstractIdempotentConsumerDecorator;
import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;

/**
 * Wraps the idempotent consumer in one more decorator, such as a cache in front of the inbox or metrics.
 * Oncebox applies every supplier in turn, so each one receives the consumer already wrapped by the previous.
 */
public interface IdempotentConsumerDecoratorSupplier {
    AbstractIdempotentConsumerDecorator supply(IdempotentConsumer consumer);
}
