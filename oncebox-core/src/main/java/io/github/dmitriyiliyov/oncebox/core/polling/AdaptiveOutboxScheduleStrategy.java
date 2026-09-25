package io.github.dmitriyiliyov.oncebox.core.polling;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class AdaptiveOutboxScheduleStrategy implements OutboxScheduleStrategy {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveOutboxScheduleStrategy.class);

    private final AdaptivePollingPropertiesHolder properties;
    private final ScheduledExecutorService executor;
    private final OutboxScheduleStrategyListener listener;
    private final long minFixedDelay;
    private final long maxFixedDelay;
    private final double multiplier;
    private final AtomicLong currentDelay;
    private final AtomicBoolean taskInProcess;

    public AdaptiveOutboxScheduleStrategy(AdaptivePollingPropertiesHolder properties,
                                          ScheduledExecutorService executor,
                                          OutboxScheduleStrategyListener listener) {
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
        this.executor = Objects.requireNonNull(executor, "executor cannot be null");
        this.listener = new SafeOutboxScheduleStrategyListenerDecorator(
                Objects.requireNonNull(listener, "listener cannot be null")
        );
        this.minFixedDelay = properties.getMinFixedDelay().toMillis();
        this.maxFixedDelay = properties.getMaxFixedDelay().toMillis();
        this.multiplier = properties.getMultiplier();
        this.currentDelay = new AtomicLong(minFixedDelay);
        this.taskInProcess = new AtomicBoolean(false);
    }

    @Override
    public void scheduleExecution(ContinuableTask task) {
        scheduleNext(task, properties.getInitialDelay().toMillis());
    }

    private void scheduleNext(ContinuableTask task, long delay) {
        if (executor.isShutdown() || !taskInProcess.compareAndSet(false, true)) {
            listener.onExecutionSkipped();
            return;
        }
        try {
            executor.schedule(
                    () -> {
                        try {
                            executeTask(task);
                        } catch (Throwable t) {
                            log.error("Exception in scheduled execution", t);
                        }
                    },
                    delay,
                    TimeUnit.MILLISECONDS
            );
        } catch (RejectedExecutionException e) {
            taskInProcess.set(false);
            log.warn("Executor rejected scheduling, likely shutting down", e);
        }
    }

    private void executeTask(ContinuableTask task) {
        boolean shouldContinue = false;
        listener.onExecutionStarted();
        try {
            shouldContinue = task.run();
            listener.onExecutionSucceeded();
        } catch (Throwable t) {
            log.error("Exception while executing task", t);
            listener.onExecutionFailed();
        } finally {
            if (shouldContinue) {
                currentDelay.set(minFixedDelay);
                listener.onDelayChanged(minFixedDelay);
            } else {
                long delay = currentDelay.updateAndGet(d -> Math.min((long) (multiplier * d), maxFixedDelay));
                listener.onDelayChanged(delay);
                log.debug("Schedule strategy adapted, current delay is %dms".formatted(delay));
            }
            taskInProcess.set(false);
            if (!executor.isShutdown()) {
                scheduleNext(task, currentDelay.get());
            }
        }
    }
}
