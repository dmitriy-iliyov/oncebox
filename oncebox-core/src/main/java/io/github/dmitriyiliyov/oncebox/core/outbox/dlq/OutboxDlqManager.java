package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the lifecycle of DLQ events.
 * <p>
 * Provides operations for saving, loading and deleting events
 * that have failed processing and been moved to the DLQ.
 */
public interface OutboxDlqManager {

    /**
     * Saves a batch of DLQ events.
     * <p>
     * Does nothing if the list is null or empty.
     *
     * @param events the list of DLQ events to save.
     */
    void saveBatch(List<OutboxDlqEvent> events);

    /**
     * Loads and locks a batch of DLQ events with a specific status.
     * <p>
     * Locked events have their status changed to {@link DlqStatus#IN_PROCESS},
     * preventing concurrent processing by other instances.
     * <p>
     * The caller must hold an active transaction: the rows stay locked only until it ends, so whatever is done
     * with the batch has to happen in that same transaction.
     *
     * @param status    the current status of the events to load.
     * @param batchSize the maximum number of events to load and lock.
     * @return          list of locked DLQ events with status set to {@link DlqStatus#IN_PROCESS}.
     */
    List<OutboxDlqEvent> loadAndLockBatch(DlqStatus status, int batchSize);

    /**
     * Deletes a batch of DLQ events by their IDs without any additional checks.
     * <p>
     * Does nothing and returns 0 if the set is null or empty.
     *
     * @param ids the set of event IDs to delete.
     * @return    the number of deleted events.
     */
    int deleteBatch(Set<UUID> ids);

    /**
     * Deletes resolved DLQ events whose {@code updated_at} is at or before {@code now - ttl}.
     * <p>
     * At most {@code batchSize} records are deleted per call.
     *
     * @param ttl       the duration after which a resolved DLQ event is considered expired.
     * @param batchSize the maximum number of events to delete in one call.
     * @return          the number of deleted events.
     */
    int deleteResolvedBatch(Duration ttl, int batchSize);
}