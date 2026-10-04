package io.github.dmitriyiliyov.oncebox.core.inbox;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * DAO layer for the inbox: the IDs of events that have already been consumed.
 * <p>
 * Persists them to ensure idempotent consumption.
 * Supports atomic "save if absent" operations critical for preventing duplicate processing.
 */
public interface InboxRepository {

    /**
     * Saves a single event ID to the repository if it does not already exist.
     *
     * @param id the UUID of the event to save.
     * @return   1 if the ID was inserted, 0 if it already existed.
     */
    int saveIfAbsent(UUID id);

    /**
     * Saves multiple event IDs to the repository if they do not already exist.
     * <p>
     * Returns an empty set if {@code ids} is null or empty.
     * Only IDs that were not previously present are inserted and returned.
     * <p>
     * Implementations may throw {@link ConcurrentInsertException} if a race condition
     * is detected between the existence check and the insert.
     *
     * @param ids a set of UUIDs to save.
     * @return    a set of UUIDs that were successfully inserted.
     * @throws ConcurrentInsertException if a concurrent insert is detected.
     */
    Set<UUID> saveIfAbsent(Set<UUID> ids);

    /**
     * Deletes consumed event records with {@code consumed_at} at or before the given threshold.
     * <p>
     * At most {@code batchSize} records are deleted per call.
     * Returns 0 if no records match the threshold.
     *
     * @param threshold records consumed at or before this timestamp will be deleted.
     * @param batchSize the maximum number of records to delete in one operation.
     * @return          the number of deleted records.
     */
    int deleteBatchByThreshold(Instant threshold, int batchSize);
}