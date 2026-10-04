package io.github.dmitriyiliyov.oncebox.core.polling;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTask;

public interface OnceboxScheduleStrategy {

    /**
     * Schedules the given task for execution.
     *
     * @param task the task to be scheduled. Its execution continues as long as it returns {@code true}.
     */
    void scheduleExecution(ContinuableTask task);
}
