package io.github.dmitriyiliyov.oncebox.metrics.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxManager;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxPropertiesHolder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OutboxManagerMetricsDecorator implements OutboxManager {

    private static final EventStatus [] STATUSES = new EventStatus[] {EventStatus.PROCESSED};
    private final OutboxManager delegate;
    private final Map<String, Map<EventStatus, Counter>> counters;
    private final Map<ActionType, Counter> actionCounters;

    public OutboxManagerMetricsDecorator(OutboxPropertiesHolder properties,
                                         MeterRegistry registry,
                                         OutboxManager delegate) {
        Objects.requireNonNull(properties, "properties cannot be null");
        Objects.requireNonNull(registry, "registry cannot be null");
        this.counters = properties.getEventHolders().keySet()
                .stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        eventType -> Arrays.stream(STATUSES)
                                .collect(Collectors.toMap(
                                        Function.identity(),
                                        eventStatus -> registry.counter(
                                                "outbox_events_rate_total",
                                                "event_type", eventType,
                                                "status", eventStatus.toString().toLowerCase()
                                        )
                                ))
                ));
        this.actionCounters = Arrays.stream(ActionType.values())
                .collect(Collectors.toMap(
                        Function.identity(),
                        type -> registry.counter(
                                "outbox_events_by_action_type_rate_total",
                                "action_type", type.toString().toLowerCase())
                        )
                );
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    }

    @Override
    public void save(OutboxEvent event) {
        delegate.save(event);
    }

    @Override
    public void saveBatch(List<OutboxEvent> eventBatch) {
        delegate.saveBatch(eventBatch);
    }

    @Override
    public void saveBatchWithCreatedAt(List<OutboxEvent> eventBatch) {
        delegate.saveBatchWithCreatedAt(eventBatch);
    }

    @Override
    public List<OutboxEvent> loadBatch(String eventType, int batchSize, UUID lockToken) {
        return delegate.loadBatch(eventType, batchSize, lockToken);
    }

    @Override
    public List<OutboxEvent> loadBatch(EventStatus status, int batchSize) {
        List<OutboxEvent> events = delegate.loadBatch(status, batchSize);
        actionCounters.get(ActionType.ATTEMPT_MOVE_TO_DLQ).increment(events.size());
        return events;
    }

    @Override
    public void finalizeBatch(List<OutboxEvent> events,
                              Set<UUID> processedIds,
                              Set<UUID> failedIds,
                              int maxRetryCount,
                              Function<Integer, Duration> retryDelaySupplier,
                              UUID lockToken) {
        delegate.finalizeBatch(events, processedIds, failedIds, maxRetryCount, retryDelaySupplier, lockToken);
        if (!events.isEmpty()) {
            String eventType = events.getFirst().getEventType();
            if (eventType != null) {
                Map<EventStatus, Counter> eventTypeCounter = counters.get(eventType);
                if (processedIds != null) {
                    eventTypeCounter.get(EventStatus.PROCESSED).increment(processedIds.size());
                }
            }
        }
    }

    @Override
    public int recoverStuckBatch(Duration maxBatchProcessingTime, int batchSize) {
        int recoveredCount = delegate.recoverStuckBatch(maxBatchProcessingTime, batchSize);
        actionCounters.get(ActionType.RECOVERED).increment(recoveredCount);
        return recoveredCount;
    }

    @Override
    public int deleteProcessedBatch(Duration ttl, int batchSize) {
        int deletedCount = delegate.deleteProcessedBatch(ttl, batchSize);
        actionCounters.get(ActionType.CLEANED).increment(deletedCount);
        return deletedCount;
    }

    @Override
    public int deleteBatch(Set<UUID> ids) {
        int deletedCount = delegate.deleteBatch(ids);
        actionCounters.get(ActionType.SUCCESS_MOVED_TO_DLQ).increment(deletedCount);
        return deletedCount;
    }

    /**
     * Defines tags for the {@code outbox_events_by_action_type_rate_total} metric.
     */
    private enum ActionType {

        /**
         * Incremented upon attempt to load and move failed events from the main outbox to the DLQ.
         */
        ATTEMPT_MOVE_TO_DLQ,

        /**
         * Incremented when stuck events are successfully recovered by the background process.
         */
        RECOVERED,

        /**
         * Incremented upon the automatic deletion of old processed events by the cleanup process.
         */
        CLEANED,

        /**
         * Incremented upon the successful deletion of a batch from the main outbox after moving it to the DLQ.
         */
        SUCCESS_MOVED_TO_DLQ;
    }
}
