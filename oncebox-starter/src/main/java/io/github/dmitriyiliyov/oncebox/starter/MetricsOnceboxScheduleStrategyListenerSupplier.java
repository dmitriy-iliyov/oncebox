package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategyListener;
import io.github.dmitriyiliyov.oncebox.metrics.MetricsOnceboxScheduleStrategyListener;
import io.micrometer.core.instrument.MeterRegistry;

public class MetricsOnceboxScheduleStrategyListenerSupplier implements OnceboxScheduleStrategyListenerSupplier {

    private final MeterRegistry registry;

    public MetricsOnceboxScheduleStrategyListenerSupplier(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public OnceboxScheduleStrategyListener supply(String taskType) {
        return new MetricsOnceboxScheduleStrategyListener(taskType, registry);
    }
}
