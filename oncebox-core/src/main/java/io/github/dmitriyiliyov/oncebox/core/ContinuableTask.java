package io.github.dmitriyiliyov.oncebox.core;

/**
 * A {@link Runnable} that reports whether more work is likely waiting: {@code true} when the run filled its batch,
 * {@code false} otherwise. The answer never stops the task - it only lets the
 * {@link io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategy} choose the delay before the next run.
 */
@FunctionalInterface
public interface ContinuableTask {
    boolean run();
}
