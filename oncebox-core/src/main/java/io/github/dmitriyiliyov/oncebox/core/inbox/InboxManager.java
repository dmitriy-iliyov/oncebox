package io.github.dmitriyiliyov.oncebox.core.inbox;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks the IDs of events that have already been consumed, for idempotent processing.
 */
public interface InboxManager {

    /**
     * Marks the event ID as consumed unless it is already recorded.
     * <p>
     * The caller must hold an active transaction, and it has to be the one the handler of the event runs in:
     * if the handler fails, the ID is rolled back with it and a redelivered event is processed again.
     *
     * @param id the ID of the event.
     * @return   {@code true} if the ID was recorded by this call, {@code false} if it had already been consumed.
     */
    boolean tryConsume(UUID id);

    /**
     * Marks the IDs that are not yet recorded as consumed and returns those that already were.
     * <p>
     * The caller must hold an active transaction, and it has to be the one the handler of the events runs in:
     * if the handler fails, the IDs are rolled back with it and redelivered events are processed again.
     *
     * @param ids the set of event IDs to filter.
     * @return    the subset of {@code ids} that had already been consumed.
     */
    Set<UUID> tryConsumeAndGetDuplicates(Set<UUID> ids);

    /**
     * Cleans up (deletes) consumed event records that have exceeded their TTL.
     *
     * @param ttl       the duration after which a consumed event record is considered expired.
     * @param batchSize the maximum number of records to delete in a single operation.
     * @return          the number of deleted records.
     */
    int cleanBatchByTtl(Duration ttl, int batchSize);
}
