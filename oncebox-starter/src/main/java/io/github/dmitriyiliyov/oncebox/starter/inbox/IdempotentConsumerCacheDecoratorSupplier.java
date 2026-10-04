package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.core.inbox.AbstractIdempotentConsumerDecorator;
import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.core.inbox.cache.IdempotentConsumerCacheDecorator;
import io.github.dmitriyiliyov.oncebox.core.inbox.cache.InboxCache;

import java.util.Objects;

public class IdempotentConsumerCacheDecoratorSupplier implements IdempotentConsumerDecoratorSupplier {

    private final InboxCache cache;

    public IdempotentConsumerCacheDecoratorSupplier(InboxCache cache) {
        this.cache = Objects.requireNonNull(cache, "cache cannot be null");
    }

    @Override
    public AbstractIdempotentConsumerDecorator supply(IdempotentConsumer consumer) {
        return new IdempotentConsumerCacheDecorator(
                Objects.requireNonNull(consumer, "consumer cannot be null"),
                cache
        );
    }
}
