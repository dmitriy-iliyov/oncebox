package io.github.dmitriyiliyov.oncebox.core.polling;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FixedOutboxScheduleStrategy implements OutboxScheduleStrategy {

    private static final Logger log = LoggerFactory.getLogger(FixedOutboxScheduleStrategy.class);

    private final FixedPollingPropertiesHolder properties;
    private final ScheduledExecutorService executor;
    private final OutboxScheduleStrategyListener listener;

    public FixedOutboxScheduleStrategy(FixedPollingPropertiesHolder properties,
                                       ScheduledExecutorService executor,
                                       OutboxScheduleStrategyListener listener) {
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
        this.executor = Objects.requireNonNull(executor, "executor cannot be null");
        this.listener = new SafeOutboxScheduleStrategyListenerDecorator(
                Objects.requireNonNull(listener, "listener cannot be null")
        );
    }

    @Override
    public void scheduleExecution(ContinuableTask task) {
        listener.onDelayChanged(properties.getFixedDelay().toMillis());
        executor.scheduleWithFixedDelay(
                () -> {
                    listener.onExecutionStarted();
                    try {
                        task.run();
                        listener.onExecutionSucceeded();
                    } catch (Throwable t) {
                        log.error("Exception in scheduled execution", t);
                        listener.onExecutionFailed();
                    }
                },
                properties.getInitialDelay().toMillis(),
                properties.getFixedDelay().toMillis(),
                TimeUnit.MILLISECONDS
        );
    }
}
