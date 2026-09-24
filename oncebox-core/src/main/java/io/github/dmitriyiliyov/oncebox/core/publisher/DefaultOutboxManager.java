package io.github.dmitriyiliyov.oncebox.core.publisher;

import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class DefaultOutboxManager implements OutboxManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxManager.class);

    protected final OutboxRepository repository;
    protected final Clock clock;

    public DefaultOutboxManager(OutboxRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void save(OutboxEvent event) {
        repository.save(event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void saveBatch(List<OutboxEvent> eventBatch) {
        repository.saveBatch(eventBatch);
    }

    @Transactional
    @Override
    public List<OutboxEvent> loadBatch(String eventType, int batchSize, UUID lockToken) {
        Objects.requireNonNull(lockToken, "lockToken cannot be null");
        return repository.findAndLockBatchByEventTypeAndStatus(
                eventType,
                EventStatus.PENDING,
                batchSize,
                lockToken,
                EventStatus.IN_PROCESS
        );
    }

    @Transactional
    @Override
    public List<OutboxEvent> loadBatch(EventStatus status, int batchSize) {
        return repository.findAndLockBatchByStatus(status, batchSize, EventStatus.IN_PROCESS);
    }

    @Transactional
    @Override
    public void finalizeBatch(List<OutboxEvent> events,
                              Set<UUID> processedIds,
                              Set<UUID> failedIds,
                              int maxRetryCount,
                              Function<Integer, Instant> nextRetryAtSupplier,
                              UUID lockToken) {
        Objects.requireNonNull(lockToken, "lockToken cannot be null");

        boolean hasProcessed = !SetUtils.isEmpty(processedIds);
        boolean hasFailed = !SetUtils.isEmpty(failedIds);
        Set<UUID> processedIdsCopy = SetUtils.mutableCopy(processedIds);

        if (!hasProcessed && !hasFailed) {
            log.warn("Finalization nullable or empty batch not delegating to repository layer");
            return;
        }
        if (hasProcessed && hasFailed && processedIdsCopy.removeAll(failedIds)) {
            log.warn("Set of ids was overlapped, all overlapped ids moved from processedIds to failedIds");
        }

        int expected = 0;
        int updated = 0;

        if (hasProcessed && !processedIdsCopy.isEmpty()) {
            expected += processedIdsCopy.size();
            updated += repository.updateBatchStatusByLockToken(processedIdsCopy, lockToken, EventStatus.PROCESSED);
        }

        if (hasFailed) {
            List<OutboxEvent> failedEvents = prepareFailedEvents(events, failedIds, maxRetryCount, nextRetryAtSupplier);
            expected += failedEvents.size();
            updated += repository.partiallyUpdateBatchByLockToken(failedEvents, lockToken);
        }

        if (updated < expected) {
            log.warn("""
                    Outcome of %d of %d events not written, they were captured again by another poller \
                    after this batch outlived stuck recovery; lockToken=%s"""
                    .formatted(expected - updated, expected, lockToken)
            );
        }
    }

    private List<OutboxEvent> prepareFailedEvents(
            List<OutboxEvent> events,
            Set<UUID> failedIds,
            int maxRetryCount,
            Function<Integer, Instant> nextRetryAtSupplier
    ) {
        return events.stream()
                .filter(event -> failedIds.contains(event.getId()))
                .map(event -> {
                            EventStatus newStatus;
                            // because this is after try
                            int newRetryCount = event.getRetryCount() + 1;
                            Instant nextRetryAt;
                            if (newRetryCount < maxRetryCount) {
                                newStatus = EventStatus.PENDING;
                                // should count time for next try
                                nextRetryAt = nextRetryAtSupplier.apply(newRetryCount);
                            } else {
                                newStatus = EventStatus.FAILED;
                                nextRetryAt = event.getNextRetryAt();
                            }
                            return new OutboxEvent(
                                    event.getId(),
                                    newStatus,
                                    event.getEventType(),
                                    event.getPayloadType(),
                                    event.getPayload(),
                                    Math.min(newRetryCount, maxRetryCount),
                                    nextRetryAt,
                                    event.getCreatedAt(),
                                    clock.instant()
                            );
                })
                .toList();
    }

    @Transactional
    @Override
    public int recoverStuckBatch(Duration maxBatchProcessingTime, int batchSize) {
        int recoveredCount = repository.updateBatchStatusByStatusAndThreshold(
                EventStatus.IN_PROCESS,
                clock.instant().minusMillis(maxBatchProcessingTime.toMillis()),
                batchSize,
                EventStatus.PENDING
        );
        log.info("Stuck events batch recovered, recoveredCount={}; batchSize={} ", recoveredCount, batchSize);
        return recoveredCount;
    }

    @Transactional
    @Override
    public int deleteProcessedBatch(Duration ttl, int batchSize) {
        Instant threshold = clock.instant().minusMillis(ttl.toMillis());
        return repository.deleteBatchByStatusAndThreshold(EventStatus.PROCESSED, threshold, batchSize);
    }

    @Transactional
    @Override
    public int deleteBatch(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return repository.deleteBatch(ids);
    }
}
