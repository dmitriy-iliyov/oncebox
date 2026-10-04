package io.github.dmitriyiliyov.oncebox.core.inbox;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class AbstractIdempotentConsumerDecorator implements IdempotentConsumer {

    protected final IdempotentConsumer delegate;

    public AbstractIdempotentConsumerDecorator(IdempotentConsumer delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    }

    @Override
    public void consume(UUID eventId, Runnable operation) {
        delegate.consume(eventId, operation);
    }

    @Override
    public <T> void consume(T message, EventIdExtractor<T> idExtractor, Consumer<T> operation) {
        delegate.consume(message, idExtractor, operation);
    }

    @Override
    public void consume(Set<UUID> ids, Consumer<Set<UUID>> operation) {
        delegate.consume(ids, operation);
    }

    @Override
    public <T> void consume(List<T> messages, EventIdExtractor<T> idExtractor, Consumer<List<T>> operation) {
        delegate.consume(messages, idExtractor, operation);
    }
}
