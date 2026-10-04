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
     * Called when exists task in process.
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
     * Called when the scheduling delay for the next execution is changed (e.g., during adaptive polling).
     *
     * @param delay the new delay before the next execution, in milliseconds.
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
