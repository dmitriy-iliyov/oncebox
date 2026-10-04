package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.polling.AdaptiveOnceboxScheduleStrategy;
import io.github.dmitriyiliyov.oncebox.core.polling.FixedOnceboxScheduleStrategy;
import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategy;
import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategyListener;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A factory for creating {@link OnceboxScheduleStrategy} instances based on the polling configuration.
 * <p>
 * Implementations should support scheduling strategies based on {@link PollingType}, such as
 * {@link PollingType#FIXED} and {@link PollingType#ADAPTIVE}.
 */
public final class OnceboxScheduleStrategyFactory {

    private OnceboxScheduleStrategyFactory() {}

    /**
     * Creates an {@link OnceboxScheduleStrategy} instance based on the provided task type and polling properties.
     *
     * @param taskType         an identifier representing the type of task to be scheduled.
     * @param properties       the polling properties determining the strategy type ({@link PollingType#FIXED} or {@link PollingType#ADAPTIVE}).
     * @param executor         the scheduled executor service used for task scheduling.
     * @param listenerSupplier a supplier for providing a {@link OnceboxScheduleStrategyListener} based on taskType.
     * @return                 a configured {@link OnceboxScheduleStrategy} instance.
     * @throws NullPointerException  if some of the passed parameters is null.
     * @throws IllegalStateException if an unknown or unsupported polling type is encountered.
     */
    public static OnceboxScheduleStrategy create(String taskType,
                                                 OnceboxProperties.PollingProperties properties,
                                                 ScheduledExecutorService executor,
                                                 OnceboxScheduleStrategyListenerSupplier listenerSupplier) {
        Objects.requireNonNull(taskType, "taskType cannot be null");
        Objects.requireNonNull(properties, "properties cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(listenerSupplier, "listenerSupplier cannot be null");

        if (PollingType.FIXED.equals(properties.getType())) {
            return new FixedOnceboxScheduleStrategy(taskType, properties, executor, listenerSupplier.supply(taskType));
        } else if (PollingType.ADAPTIVE.equals(properties.getType())) {
            return new AdaptiveOnceboxScheduleStrategy(taskType, properties, executor, listenerSupplier.supply(taskType));
        } else {
            throw new IllegalStateException("Reached unreachable branch during creating OnceboxScheduleStrategy");
        }
    }
}
