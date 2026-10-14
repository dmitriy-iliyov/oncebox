package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import java.time.Duration;
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
     * Saves a batch of DLQ events to the repository, writing {@code next_retry_at}, {@code created_at} and
     * {@code updated_at} from each event; {@code moved_at} is set by the database clock.
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

    /**
     * Deletes DLQ events that match a given status and were last updated at least {@code age} ago by the database
     * clock.
     * <p>
     * At most {@code batchSize} events are deleted per call.
     *
     * @param status    the DLQ status of events to delete.
     * @param age       how long ago an event has to have been last updated to be deleted.
     * @param batchSize the maximum number of events to delete in one call.
     * @return          the number of deleted events.
     */
    int deleteBatchByStatusOlderThan(DlqStatus status, Duration age, int batchSize);
}