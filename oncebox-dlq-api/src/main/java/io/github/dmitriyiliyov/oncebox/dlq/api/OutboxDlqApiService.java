package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.dlq.api.exception.OutboxDlqEventInProcessException;
import io.github.dmitriyiliyov.oncebox.dlq.api.exception.OutboxDlqEventNotFoundException;

import java.util.List;
import java.util.UUID;
/**
 * Public API for managing DLQ events produced by the outbox publisher.
 * <p>
 * Provides read, update, and delete operations for individual events as well as batch processing.
 * <p>
 * <b>Important constraints:</b>
 * <ul>
 *   <li>Events with status {@link DlqStatus#IN_PROCESS} cannot be updated or deleted.</li>
 *   <li>Batch modification operations operate either by IDs or by event type.</li>
 *   <li>Batch reading operations operate by status and event type in any combination and without both params.</li>
 * </ul>
 */
public interface OutboxDlqApiService {

    /**
     * Loads a single DLQ event by its ID.
     *
     * @param id the ID of the event to load.
     * @return   the DLQ event with the given ID.
     * @throws OutboxDlqEventNotFoundException if no event with the given ID exists.
     */
    OutboxDlqEvent findById(UUID id);

    /**
     * Loads a batch of DLQ events based on pagination and optional status filtering.
     *
     * @param request the request defining pagination and filtering criteria.
     * @return        list of DLQ events matching the given criteria; empty list if none found.
     */
    List<OutboxDlqEvent> findBatch(BatchRequest request);

    /**
     * Counts DLQ events filtered by status and/or event type.
     * <p>
     * Null or blank parameters are ignored:
     * <ul>
     *   <li>If both parameters are null/blank -> returns total count.</li>
     *   <li>If only one is provided -> filtering is applied only by that parameter.</li>
     * </ul>
     *
     * @param status    the status to filter by, or null to ignore.
     * @param eventType the event type to filter by, or null/blank to ignore.
     * @return          total number of matching events.
     */
    long count(DlqStatus status, String eventType);

    /**
     * Updates the status of a single DLQ event.
     *
     * @param id     the ID of the event to update.
     * @param status the new status to set.
     * @throws OutboxDlqEventNotFoundException  if no event with the given ID exists.
     * @throws OutboxDlqEventInProcessException if the event currently has status {@link DlqStatus#IN_PROCESS}.
     */
    void updateStatus(UUID id, DlqStatus status);

    /**
     * Updates the status for a batch of DLQ events.
     * <p>
     * Events in {@link DlqStatus#IN_PROCESS} are excluded from updates.
     *
     * @param request the request containing selection criteria and target status.
     * @return        {@link BatchModificationResponse} containing total matched and successfully updated counts.
     * @throws IllegalArgumentException if neither IDs nor event type are provided.
     */
    BatchModificationResponse updateBatchStatus(BatchUpdateRequest request);

    /**
     * Deletes a single DLQ event by its ID.
     *
     * @param id the ID of the event to delete.
     * @return   number of deleted events (always 1 if successful).
     * @throws OutboxDlqEventNotFoundException  if no event with the given ID exists.
     * @throws OutboxDlqEventInProcessException if the event currently has status {@link DlqStatus#IN_PROCESS}.
     */
    int deleteById(UUID id);

    /**
     * Deletes a batch of DLQ events.
     * <p>
     * Events in {@link DlqStatus#IN_PROCESS} are excluded from deletion.
     *
     * @param request the request containing selection criteria.
     * @return        {@link BatchModificationResponse} containing total matched and successfully deleted counts.
     * @throws IllegalArgumentException if neither IDs nor event type are provided.
     */
    BatchModificationResponse deleteBatch(BatchDeleteRequest request);
}
