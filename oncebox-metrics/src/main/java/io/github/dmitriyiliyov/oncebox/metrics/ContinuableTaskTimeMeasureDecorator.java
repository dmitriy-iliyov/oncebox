package io.github.dmitriyiliyov.oncebox.metrics;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTask;
import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ContinuableTaskTimeMeasureDecorator implements ContinuableTaskDecorator {

    private final Timer timer;

    public ContinuableTaskTimeMeasureDecorator(MeterRegistry registry, String taskType) {
        Objects.requireNonNull(registry, "registry cannot be null");
        this.timer = Timer.builder("oncebox_task_processing_duration")
                .description("Task execution time in milliseconds")
                .tag("task_type", Objects.requireNonNull(taskType, "taskType cannot be null"))
                .register(registry);
    }

    @Override
    public ContinuableTask decorate(ContinuableTask task) {
        return () -> {
            long start = System.currentTimeMillis();
            try {
                return task.run();
            } finally {
                long duration = System.currentTimeMillis() - start;
                timer.record(duration, TimeUnit.MILLISECONDS);
            }
        };
    }
}
