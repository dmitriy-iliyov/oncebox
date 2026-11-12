package io.github.dmitriyiliyov.oncebox.core;

/**
 * A background job of oncebox - polling, stuck recovery, DLQ transfer or clean-up - bound to the
 * {@link io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategy} it runs on.
 */
public interface OnceboxScheduler {

    /**
     * Hands the job over to its strategy and returns at once; the job then runs until the executor is shut down.
     */
    void schedule();
}
