package io.github.dmitriyiliyov.oncebox.core.outbox;

/**
 * Processes outbox events for a specific event type.
 * <p>
 * Defines the contract for the core processing loop:
 * <ol>
 *     <li>Load a batch of pending events from the database.</li>
 *     <li>Send them to the message broker via {@link OutboxSender}.</li>
 *     <li>Finalize their status based on the send result.</li>
 * </ol>
 * If sending fails entirely, all events in the batch are marked as failed
 * and will be retried according to the configured backoff strategy.
 */
public interface OutboxProcessor {

    /**
     * Processes a batch of outbox events based on the provided configuration.
     * <p>
     * Used by {@link io.github.dmitriyiliyov.oncebox.core.OnceboxScheduler}
     *
     * @param properties configuration for the event type being processed,
     *                   including batch size, retry limits, and backoff settings.
     * @return           the number of events loaded and finalized by this call, whatever their outcome;
     *                   0 if none were due.
     * @throws NullPointerException if {@code properties} is null.
     */
    int process(OutboxPropertiesHolder.EventPropertiesHolder properties);
}
