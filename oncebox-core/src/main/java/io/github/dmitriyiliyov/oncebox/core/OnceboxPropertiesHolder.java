package io.github.dmitriyiliyov.oncebox.core;

import io.github.dmitriyiliyov.oncebox.core.polling.PollingPropertiesHolder;

import java.time.Duration;

/**
 * Holds configuration properties shared by the outbox and the inbox.
 * <p>
 * This interface provides access to general settings that apply to both mechanisms.
 */
public interface OnceboxPropertiesHolder {

    /**
     * Holds properties of a clean-up job: processed outbox events, resolved DLQ events or inbox records.
     */
    interface CleanUpPropertiesHolder extends PollingPropertiesHolder {

        /**
         * The TTL for data before it is eligible for cleanup.
         * <p>
         * Events older than this duration will be deleted by the cleanup job.
         */
        Duration getTtl();

        /**
         * The number of records to clean up in a single batch operation.
         * <p>
         * This controls the transaction size during cleanup to avoid locking the database for too long.
         */
        Integer getBatchSize();
    }
}
