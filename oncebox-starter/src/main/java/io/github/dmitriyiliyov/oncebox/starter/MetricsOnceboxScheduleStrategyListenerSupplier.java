package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategyListener;
import io.github.dmitriyiliyov.oncebox.metrics.MetricsOnceboxScheduleStrategyListener;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Objects;

public class MetricsOnceboxScheduleStrategyListenerSupplier implements OnceboxScheduleStrategyListenerSupplier {

    private final MeterRegistry registry;

    public MetricsOnceboxScheduleStrategyListenerSupplier(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
    }

    @Override
    public OnceboxScheduleStrategyListener supply(String taskType) {
        return new MetricsOnceboxScheduleStrategyListener(taskType, registry);
    }
}
