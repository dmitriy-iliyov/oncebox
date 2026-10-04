package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * DAO layer for DLQ outbox events.
 * <p>
 * Provides a contract for storing and managing events that have failed processing.
 */
public interface OutboxDlqRepository {

    /**
     * Saves a batch of DLQ events to the repository.
     * <p>
     * Does nothing if the list is null or empty.
     *
     * @param dlqEvents the list of DLQ events to save.
     */
    void saveBatch(List<OutboxDlqEvent> dlqEvents);

    /**
     * Finds and locks a batch of DLQ events by their status.
     * <p>
     * Expected to use row-level locking (e.g. {@code SELECT ... FOR UPDATE SKIP LOCKED}).
     * May return fewer than {@code batchSize} events under contention.
     * Implementations should atomically update the status to {@code lockStatus}.
     *
     * @param status     the current status of the events to find.
     * @param batchSize  the maximum number of events to retrieve.
     * @param lockStatus the new status to set for the locked events.
     * @return           a list of locked DLQ events with status set to {@code lockStatus}.
     */
    List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus);

    /**
     * Deletes a batch of DLQ events by their IDs.
     * <p>
     * Does nothing and returns 0 if the set is null or empty.
     * IDs that do not correspond to existing events are silently ignored.
     *
     * @param ids the set of event IDs to delete.
     * @return    the number of actually deleted events.
     */
    int deleteBatch(Set<UUID> ids);

    int deleteBatchByStatusAndThreshold(DlqStatus status, Instant threshold, int batchSize);
}