package io.github.dmitriyiliyov.oncebox.core.polling;

/**
 * A listener for observing the execution lifecycle and dynamic delay adjustments of a scheduled task.
 */
public interface OnceboxScheduleStrategyListener {

    /**
     * Called immediately before a scheduled task begins execution.
     */
    void onExecutionStarted();

    /**
     * Called when a run is not scheduled because the executor is shut down or the task is already scheduled or
     * running. Only {@link AdaptiveOnceboxScheduleStrategy} reports it.
     */
    void onExecutionSkipped();

    /**
     * Called when a scheduled task completes execution successfully without throwing an exception.
     */
    void onExecutionSucceeded();

    /**
     * Called when a scheduled task terminates abruptly due to an exception.
     */
    void onExecutionFailed();

    /**
     * Reports the delay before the next run: {@link FixedOnceboxScheduleStrategy} once, when the task is scheduled,
     * {@link AdaptiveOnceboxScheduleStrategy} after every run, whether or not the delay changed.
     *
     * @param delay the delay before the next run, in milliseconds
     */
    void onDelayChanged(long delay);

    /**
     * A no-operation implementation that does nothing on any event.
     */
    OnceboxScheduleStrategyListener NOOP = new OnceboxScheduleStrategyListener() {
        @Override
        public void onExecutionStarted() { }

        @Override
        public void onExecutionSucceeded() { }

        @Override
        public void onExecutionFailed() { }

        @Override
        public void onDelayChanged(long delay) { }

        @Override
        public void onExecutionSkipped() { }
    };
}
