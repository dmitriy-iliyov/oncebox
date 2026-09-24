package io.github.dmitriyiliyov.oncebox.core.publisher;

import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Manages the lifecycle of outbox events, including saving, loading, and updating their state.
 * <p>
 * Acts as a service layer between the application and the persistence layer,
 * handling state transitions, retry calculation, and stuck event recovery.
 */
public interface OutboxManager {

    /**
     * Saves a single outbox event within an existing transaction.
     * <p>
     * Must be called within an active transaction - uses MANDATORY propagation.
     * Throws an exception if no transaction is active at the time of the call.
     *
     * @param event the event to save.
     */
    void save(OutboxEvent event);

    /**
     * Saves a batch of outbox events within an existing transaction.
     * <p>
     * Must be called within an active transaction - uses MANDATORY propagation.
     * Throws an exception if no transaction is active at the time of the call.
     * Does nothing if the list is null or empty.
     *
     * @param eventBatch the list of events to save.
     */
    void saveBatch(List<OutboxEvent> eventBatch);

    /**
     * Loads and locks a batch of events of a specific type for processing.
     * <p>
     * Locked events have their status changed to {@link EventStatus#IN_PROCESS}
     * to prevent concurrent processing by other instances.
     * Only events with status {@link EventStatus#PENDING} are returned.
     *
     * @param eventType the type of events to load.
     * @param batchSize the maximum number of events to load and lock.
     * @param lockToken the token of this capture; the same token must be passed to
     *                  {@link #finalizeBatch(List, Set, Set, int, Function, UUID)} for this batch.
     * @return          a list of locked outbox events ready for processing; empty list if none available.
     */
    List<OutboxEvent> loadBatch(String eventType, int batchSize, UUID lockToken);

    /**
     * Loads and locks a batch of events with a specific status.
     * <p>
     * Locked events have their status changed to {@link EventStatus#IN_PROCESS}.
     * Used for DLQ transfer.
     *
     * @param status    the status of events to load.
     * @param batchSize the maximum number of events to load and lock.
     * @return          a list of locked outbox events; empty list if none available.
     */
    List<OutboxEvent> loadBatch(EventStatus status, int batchSize);

    /**
     * Finalizes a batch of events after processing, marking them as processed or updating retry state.
     * <p>
     * Successfully processed events are marked as {@link EventStatus#PROCESSED}.
     * Failed events have their retry count incremented and next retry time recalculated.
     * Once the retry count reaches {@code maxRetryCount}, the event is marked as {@link EventStatus#FAILED}.
     * <p>
     * If an ID appears in both {@code processedIds} and {@code failedIds}, it is treated as failed.
     * If both sets are null or empty, the method is a no-op.
     * <p>
     * Events captured again by another poller since {@code lockToken} was issued are left untouched.
     *
     * @param events              the original list of events in the batch.
     * @param processedIds        the IDs of successfully processed events.
     * @param failedIds           the IDs of failed events.
     * @param maxRetryCount       the maximum number of retries allowed; must be non-negative.
     * @param nextRetryAtSupplier a function that calculates the next retry time given the current retry count.
     * @param lockToken           the token the batch was loaded with.
     * @throws IllegalArgumentException if {@code maxRetryCount} is negative.
     */
    void finalizeBatch(List<OutboxEvent> events,
                       Set<UUID> processedIds,
                       Set<UUID> failedIds,
                       int maxRetryCount,
                       Function<Integer, Instant> nextRetryAtSupplier,
                       UUID lockToken);

    /**
     * Recovers events stuck in {@link EventStatus#IN_PROCESS} state for longer than the given duration.
     * <p>
     * Stuck events are moved back to {@link EventStatus#PENDING} to be retried.
     * This handles cases where a processing node crashed without completing the batch.
     *
     * @param maxBatchProcessingTime the maximum allowed duration in {@link EventStatus#IN_PROCESS}
     *                               before an event is considered stuck.
     * @param batchSize              the maximum number of events to recover in one call.
     * @return                       the number of recovered events.
     */
    int recoverStuckBatch(Duration maxBatchProcessingTime, int batchSize);

    /**
     * Deletes processed events with {@code updated_at} strictly before the given threshold.
     * <p>
     * At most {@code batchSize} records are deleted per call.
     *
     * @param ttl       the duration after which a processed event record is considered expired.
     * @param batchSize the maximum number of events to delete in one call.
     * @return          the number of deleted events.
     */
    int deleteProcessedBatch(Duration ttl, int batchSize);

    /**
     * Deletes a batch of events by their IDs without any additional status checks.
     * <p>
     * Does nothing and returns 0 if the set is null or empty.
     * IDs that do not correspond to existing events are silently ignored.
     *
     * @param ids the set of event IDs to delete.
     * @return    the number of actually deleted events.
     */
    int deleteBatch(Set<UUID> ids);
}
