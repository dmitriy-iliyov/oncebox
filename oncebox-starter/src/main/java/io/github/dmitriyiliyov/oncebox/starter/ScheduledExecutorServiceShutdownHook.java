package io.github.dmitriyiliyov.oncebox.starter;

/**
 * Stops the {@code ScheduledExecutorService} that every oncebox job runs on, when the application context closes.
 */
public interface ScheduledExecutorServiceShutdownHook {
    void shutdown();
}
