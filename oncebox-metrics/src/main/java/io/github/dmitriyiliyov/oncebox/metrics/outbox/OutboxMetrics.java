package io.github.dmitriyiliyov.oncebox.metrics.outbox;

/**
 * Abstraction for registering outbox-related metrics.
 * <p>
 * Implementations of this interface are responsible for setting up and exposing
 * metrics to a monitoring system.
 */
public interface OutboxMetrics {

    void register();
}
