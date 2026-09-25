package io.github.dmitriyiliyov.oncebox.core;

import io.github.dmitriyiliyov.oncebox.core.polling.PollingPropertiesHolder;

import java.time.Duration;
import java.util.Map;

/**
 * Holds outbox publisher configuration properties.
 * <p>
 * This interface provides access to various settings for event processing, recovery, and DLQ management.
 */
public interface OutboxPublisherPropertiesHolder {

    /**
     * Checks if a specific event type exists in the configuration.
     *
     * @param eventType the event type to check.
     * @return          {@code true} if the event type exists, {@code false} otherwise.
     */
    boolean existEventType(String eventType);

    /**
     * Returns a map of event type to their respective properties holders.
     *
     * @return a map of {@link EventPropertiesHolder}.
     */
    Map<String, EventPropertiesHolder> getEventHolders();

    /**
     * Holds properties for recovering stuck outbox events.
     * <p>
     * Stuck events are those that remain in the {@code IN_PROCESS} state for too long, usually due to a crash.
     */
    interface StuckRecoveryPropertiesHolder extends PollingPropertiesHolder {

        /**
         * The maximum time a batch can be in processing before being considered stuck.
         * <p>
         * Events in {@code IN_PROCESS} state longer than this duration will be recovered (moved back to {@code PENDING}).
         */
        Duration getMaxBatchProcessingTime();

        /**
         * The number of stuck events to recover in one batch.
         */
        Integer getBatchSize();
    }

    /**
     * Holds properties specific to a particular outbox event type.
     */
    interface EventPropertiesHolder extends PollingPropertiesHolder {

        /**
         * The type of the event.
         */
        String getEventType();

        /**
         * The number of events to process in one batch for this event type.
         */
        Integer getBatchSize();

        /**
         * The target topic or exchange name for this event type.
         *
         * @return the topic/exchange name.
         */
        String getTopic();

        /**
         * The maximum number of retries for a failed event of this type.
         */
        Integer getMaxRetries();

        /**
         * The multiplier for exponential backoff retry strategy.
         */
        Double backoffMultiplier();

        /**
         * The base delay for exponential backoff retry strategy, in milliseconds.
         */
        Long backoffDelay();
    }

    /**
     * Holds properties for Dead Letter Queue management.
     */
    interface DlqPropertiesHolder {

        /**
         * Generic properties for transferring events to or from the DLQ.
         */
        interface TransferPropertiesHolder extends PollingPropertiesHolder {
            
            /**
             * The number of events to process in one batch.
             */
            Integer getBatchSize();
        }

        /**
         * Properties governing the transfer of failed events to the DLQ.
         * <p>
         * Applies when an event has exceeded its maximum retry attempts.
         */
        TransferPropertiesHolder getTransferTo();

        /**
         * Properties governing the transfer of events from the DLQ back to the regular outbox.
         * <p>
         * Applies when manually triggering a retry for DLQ events.
         */
        TransferPropertiesHolder getTransferFrom();
    }
}