package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.oncebox.metrics.ContinuableTaskTimeMeasureDecorator;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Objects;

public class ContinuableTaskTimeMeasureDecoratorSupplier implements ContinuableTaskDecoratorSupplier {

    private final MeterRegistry registry;

    public ContinuableTaskTimeMeasureDecoratorSupplier(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
    }

    @Override
    public ContinuableTaskDecorator supply(String taskType) {
        return new ContinuableTaskTimeMeasureDecorator(registry, taskType);
    }
}
