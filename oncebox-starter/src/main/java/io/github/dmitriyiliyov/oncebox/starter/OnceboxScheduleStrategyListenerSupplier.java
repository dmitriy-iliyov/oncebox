package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategyListener;

/**
 * A supplier for creating {@link OnceboxScheduleStrategyListener} instances.
 */
public interface OnceboxScheduleStrategyListenerSupplier {
    
    /**
     * Supplies a configured {@link OnceboxScheduleStrategyListener} for the given task type.
     *
     * @param taskType the type of task for which to provide the listener.
     * @return a configured {@link OnceboxScheduleStrategyListener}.
     */
    OnceboxScheduleStrategyListener supply(String taskType);
}
