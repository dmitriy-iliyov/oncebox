package io.github.dmitriyiliyov.oncebox.core.inbox;

import java.time.Duration;
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
     * Returns an empty set if {@code ids} is empty.
     * Only IDs that were not previously present are inserted and returned.
     * <p>
     * Implementations may throw {@link ConcurrentInsertException} if a race condition
     * is detected between the existence check and the insert.
     *
     * @param ids a set of UUIDs to save.
     * @return    a set of UUIDs that were successfully inserted.
     * @throws ConcurrentInsertException if a concurrent insert is detected.
     * @throws NullPointerException      if {@code ids} is null.
     */
    Set<UUID> saveIfAbsent(Set<UUID> ids);

    /**
     * Deletes consumed event records consumed at least {@code age} ago by the database clock.
     * <p>
     * At most {@code batchSize} records are deleted per call.
     * Returns 0 if no records are that old.
     *
     * @param age       how long ago a record has to have been consumed to be deleted.
     * @param batchSize the maximum number of records to delete in one operation.
     * @return          the number of deleted records.
     */
    int deleteBatchOlderThan(Duration age, int batchSize);
}