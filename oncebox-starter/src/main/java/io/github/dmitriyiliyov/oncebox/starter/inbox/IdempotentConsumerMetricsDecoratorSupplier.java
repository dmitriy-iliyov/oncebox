package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.core.inbox.AbstractIdempotentConsumerDecorator;
import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.metrics.inbox.IdempotentConsumerMetricsDecorator;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Objects;

public class IdempotentConsumerMetricsDecoratorSupplier implements IdempotentConsumerDecoratorSupplier {

    private final MeterRegistry registry;

    public IdempotentConsumerMetricsDecoratorSupplier(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
    }

    @Override
    public AbstractIdempotentConsumerDecorator supply(IdempotentConsumer consumer) {
        return new IdempotentConsumerMetricsDecorator(
                registry,
                Objects.requireNonNull(consumer, "consumer cannot be null")
        );
    }
}
