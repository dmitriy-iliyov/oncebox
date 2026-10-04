package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository abstraction for accessing and modifying DLQ events.
 */
public interface OutboxDlqApiRepository {

    /**
     * Finds a DLQ event by its ID.
     *
     * @param id the ID of the event.
     * @return   an {@link Optional} containing the event if found, otherwise empty.
     */
    Optional<OutboxDlqEvent> findById(UUID id);

    /**
     * Finds and lock a DLQ event by its ID.
     *
     * @param id the ID of the event.
     * @return   an {@link Optional} containing the event if found, otherwise empty.
     */
    Optional<OutboxDlqEvent> findByIdForUpdate(UUID id);

    /**
     * Finds a batch of DLQ events by their status with pagination.
     * <p>
     * Results are ordered by {@code moved_at} and {@code id} ascending.
     *
     * @param filter      the filter criteria to apply.
     * @param batchNumber the page number (0-based).
     * @param batchSize   the number of events per page.
     * @return            a list of DLQ events for the specified page; empty list if none found.
     */
    List<OutboxDlqEvent> findBatch(DlqFilter filter, int batchNumber, int batchSize);

    /**
     * Counts events matching the given filter.
     *
     * @param filter the filter criteria to apply.
     * @return       the total number of matching DLQ events.
     */
    long count(DlqFilter filter);

    /**
     * Updates the status of a single DLQ event.
     * <p>
     * If no event with the given ID exists, the operation is a no-op.
     *
     * @param id     the ID of the event to update.
     * @param status the new status to set.
     */
    void updateStatus(UUID id, DlqStatus status);

    /**
     * Updates the status for a batch of DLQ events based on the provided filter.
     * <p>
     * Does nothing if the filter criteria result in no matches.
     * Events currently in {@code forbiddenStatus} are excluded from the update.
     *
     * @param filter          the filter criteria defining which events to update.
     * @param forbiddenStatus the status that prevents an event from being updated.
     * @return                the number of actually updated events.
     */
    int updateBatchStatus(DlqFilter filter, DlqStatus forbiddenStatus);

    /**
     * Deletes a single DLQ event by its ID.
     *
     * @param id the ID of the event to delete.
     * @return   1 if the event was deleted, 0 if no event with the given ID exists.
     */
    int deleteById(UUID id);

    /**
     * Deletes a batch of DLQ events based on the provided filter.
     * <p>
     * Does nothing and returns 0 if the filter criteria result in no matches.
     * Events currently in {@code forbiddenStatus} are excluded from deletion.
     *
     * @param filter          the filter criteria defining which events to delete.
     * @param forbiddenStatus the status that prevents an event from being deleted.
     * @return                the number of actually deleted events.
     */
    int deleteBatch(DlqFilter filter, DlqStatus forbiddenStatus);
}
