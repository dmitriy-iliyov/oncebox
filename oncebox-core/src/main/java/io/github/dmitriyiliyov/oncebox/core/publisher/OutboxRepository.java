package io.github.dmitriyiliyov.oncebox.core.publisher;

import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * DAO layer for outbox events.
 * <p>
 * Provides a contract for storing, retrieving, and managing outbox events in a persistent store.
 */
public interface OutboxRepository {

    /**
     * Saves a single outbox event.
     *
     * @param event the event to save.
     */
    void save(OutboxEvent event);

    /**
     * Saves a batch of outbox events.
     * <p>
     * Does nothing if the list is null or empty.
     *
     * @param eventBatch the list of events to save.
     */
    void saveBatch(List<OutboxEvent> eventBatch);

    /**
     * Finds and locks a batch of events by their type and status.
     * <p>
     * Must atomically select events and update their status to {@code lockStatus}.
     * Implementations should use row-level locking (e.g. {@code SELECT ... FOR UPDATE SKIP LOCKED})
     * to allow concurrent processing across multiple instances.
     * May return fewer than {@code batchSize} events under contention.
     *
     * @param eventType  the type of events to find.
     * @param status     the current status of events to find.
     * @param batchSize  the maximum number of events to retrieve.
     * @param lockToken  the token of this capture, written to every locked event; the finalization of the batch
     *                   passes it back, so it cannot overwrite an event captured since by another poller.
     * @param lockStatus the new status to set for the locked events.
     * @return           a list of locked outbox events with status set to {@code lockStatus};
     *                   empty list if none available.
     */
    List<OutboxEvent> findAndLockBatchByEventTypeAndStatus(String eventType,
                                                           EventStatus status,
                                                           int batchSize,
                                                           UUID lockToken,
                                                           EventStatus lockStatus);

    /**
     * Finds and locks a batch of events by their status, without filtering by event type.
     * <p>
     * Behaves identically to
     * {@link #findAndLockBatchByEventTypeAndStatus(String, EventStatus, int, UUID, EventStatus)}
     * except that all event types are considered.
     *
     * @param status     the current status of events to find.
     * @param batchSize  the maximum number of events to retrieve.
     * @param lockStatus the new status to set for the locked events.
     * @return           a list of locked outbox events with status set to {@code lockStatus};
     *                   empty list if none available.
     */
    List<OutboxEvent> findAndLockBatchByStatus(EventStatus status, int batchSize, EventStatus lockStatus);

    /**
     * Updates the status for a batch of events that are still held by the given capture.
     * <p>
     * Does nothing and returns 0 if the set is empty.
     * IDs that do not correspond to existing events, or whose events carry another lock token, are silently
     * ignored: such an event was recovered and captured again, and its newer owner writes its outcome.
     * <p>
     * The token alone decides, whatever the current status: an event recovered to {@link EventStatus#PENDING}
     * and not yet captured again still carries this token and takes this outcome - the outcome of the capture
     * that actually sent it.
     *
     * @param ids       the IDs of the events to update.
     * @param lockToken the token the events were captured with.
     * @param newStatus the new status to set.
     * @return          the number of updated events.
     * @throws IllegalArgumentException if {@code newStatus} is {@link EventStatus#FAILED};
     *                                  use {@link #partiallyUpdateBatchByLockToken(List, UUID)} for failed events.
     * @throws NullPointerException     if {@code ids} is null.
     */
    int updateBatchStatusByLockToken(Set<UUID> ids, UUID lockToken, EventStatus newStatus);

    /**
     * Updates the status of events that match a given status and were last updated
     * strictly before the given threshold.
     * <p>
     * Typically used for recovering events stuck in {@link EventStatus#IN_PROCESS}.
     * At most {@code batchSize} events are updated per call.
     *
     * @param status    the current status of events to update.
     * @param threshold events with {@code updated_at} strictly before this timestamp will be updated.
     * @param batchSize the maximum number of events to update in one call.
     * @param newStatus the new status to set.
     * @return          the number of updated events.
     */
    int updateBatchStatusByStatusAndThreshold(EventStatus status,
                                              Instant threshold,
                                              int batchSize,
                                              EventStatus newStatus);

    /**
     * Updates a batch of events using the state carried by each event object.
     * <p>
     * Used when finalizing a batch where some events failed processing.
     * Does nothing and returns 0 if the list is null or empty.
     * Only events still held by the given capture are updated, on the same terms as
     * {@link #updateBatchStatusByLockToken(Set, UUID, EventStatus)}.
     *
     * @param events    the list of events to update; each event carries its own updated state.
     * @param lockToken the token the events were captured with.
     * @return          the number of updated events.
     */
    int partiallyUpdateBatchByLockToken(List<OutboxEvent> events, UUID lockToken);

    /**
     * Deletes a batch of events by their IDs.
     * <p>
     * Does nothing and returns 0 if the set is empty.
     * IDs that do not correspond to existing events are silently ignored.
     *
     * @param ids the set of event IDs to delete.
     * @return    the number of deleted events.
     * @throws NullPointerException if {@code ids} is null.
     */
    int deleteBatch(Set<UUID> ids);

    /**
     * Deletes events that match a given status and were last updated
     * strictly before the given threshold.
     * <p>
     * At most {@code batchSize} events are deleted per call.
     *
     * @param status    the status of events to delete.
     * @param threshold events with {@code updated_at} strictly before this timestamp will be deleted.
     * @param batchSize the maximum number of events to delete in one call.
     * @return          the number of deleted events.
     */
    int deleteBatchByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize);
}
