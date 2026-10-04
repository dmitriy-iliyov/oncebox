package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.core.inbox.AbstractIdempotentConsumerDecorator;
import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;

public interface IdempotentConsumerDecoratorSupplier {
    AbstractIdempotentConsumerDecorator supply(IdempotentConsumer consumer);
}
