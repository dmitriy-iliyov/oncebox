package io.github.dmitriyiliyov.oncebox.core.polling;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTask;

/**
 * Decides when a background task of oncebox runs again: at a fixed delay or at one that adapts to the load.
 *
 * @see FixedOnceboxScheduleStrategy
 * @see AdaptiveOnceboxScheduleStrategy
 */
public interface OnceboxScheduleStrategy {

    /**
     * Hands the task over to the executor and returns at once; the task then runs repeatedly until the executor is
     * shut down. What a run returns is a hint for the delay before the next one, which a strategy is free to
     * ignore - {@link FixedOnceboxScheduleStrategy} does.
     *
     * @param task the task to run; an exception it throws is logged and does not stop later runs
     */
    void scheduleExecution(ContinuableTask task);
}
