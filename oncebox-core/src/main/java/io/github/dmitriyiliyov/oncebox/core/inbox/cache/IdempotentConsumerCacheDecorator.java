package io.github.dmitriyiliyov.oncebox.core.inbox.cache;

import io.github.dmitriyiliyov.oncebox.core.inbox.AbstractIdempotentConsumerDecorator;
import io.github.dmitriyiliyov.oncebox.core.inbox.EventIdExtractor;
import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class IdempotentConsumerCacheDecorator extends AbstractIdempotentConsumerDecorator {

    private final InboxCache cache;

    public IdempotentConsumerCacheDecorator(IdempotentConsumer delegate, InboxCache cache) {
        super(delegate);
        this.cache = Objects.requireNonNull(cache, "cache cannot be null");
    }

    @Override
    public void consume(UUID eventId, Runnable operation) {
        if (cache.isConsumed(eventId)) {
            return;
        }
        super.consume(eventId, operation);
        cache.consume(eventId);
    }

    @Override
    public <T> void consume(T message, EventIdExtractor<T> idExtractor, Consumer<T> operation) {
        UUID eventId = idExtractor.extract(message);
        if (cache.isConsumed(eventId)) {
            return;
        }
        super.consume(message, idExtractor, operation);
        cache.consume(eventId);
    }
}
