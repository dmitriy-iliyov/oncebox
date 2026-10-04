package io.github.dmitriyiliyov.oncebox.metrics.inbox;

import io.github.dmitriyiliyov.oncebox.core.inbox.AbstractIdempotentConsumerDecorator;
import io.github.dmitriyiliyov.oncebox.core.inbox.EventIdExtractor;
import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class IdempotentConsumerMetricsDecorator extends AbstractIdempotentConsumerDecorator {

    private final Counter fails;

    public IdempotentConsumerMetricsDecorator(MeterRegistry registry, IdempotentConsumer delegate) {
        super(delegate);
        Objects.requireNonNull(registry, "registry cannot be null");
        this.fails = registry.counter("inbox_events_total", "type", "failed");
    }

    @Override
    public void consume(UUID eventId, Runnable operation) {
        try {
            super.consume(eventId, operation);
        } catch (Exception e) {
            fails.increment();
            throw e;
        }
    }

    @Override
    public <T> void consume(T message, EventIdExtractor<T> idExtractor, Consumer<T> operation) {
        try {
            super.consume(message, idExtractor, operation);
        } catch (Exception e) {
            fails.increment();
            throw e;
        }
    }

    @Override
    public void consume(Set<UUID> ids, Consumer<Set<UUID>> operation) {
        try {
            super.consume(ids, operation);
        } catch (Exception e) {
            fails.increment(ids.size());
            throw e;
        }
    }

    @Override
    public <T> void consume(List<T> messages, EventIdExtractor<T> idExtractor, Consumer<List<T>> operation) {
        try {
            super.consume(messages, idExtractor, operation);
        } catch (Exception e) {
            fails.increment(messages.size());
            throw e;
        }
    }
}
