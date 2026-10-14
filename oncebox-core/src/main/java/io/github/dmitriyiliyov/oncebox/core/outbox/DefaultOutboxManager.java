package io.github.dmitriyiliyov.oncebox.core.outbox;

import io.github.dmitriyiliyov.oncebox.core.utils.ListUtils;
import io.github.dmitriyiliyov.oncebox.core.utils.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class DefaultOutboxManager implements OutboxManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxManager.class);

    protected final OutboxRepository repository;

    public DefaultOutboxManager(OutboxRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void save(OutboxEvent event) {
        repository.save(event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void saveBatch(List<OutboxEvent> eventBatch) {
        if (ListUtils.isEmpty(eventBatch)) {
            return;
        }
        repository.saveBatch(eventBatch);
    }

    @Transactional
    @Override
    public void saveBatchWithCreatedAt(List<OutboxEvent> eventBatch) {
        if (ListUtils.isEmpty(eventBatch)) {
            return;
        }
        repository.saveBatchWithCreatedAt(eventBatch);
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

    @Transactional(propagation = Propagation.MANDATORY)
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
                              Function<Integer, Duration> retryDelaySupplier,
                              UUID lockToken) {
        Objects.requireNonNull(lockToken, "lockToken cannot be null");
        if (maxRetryCount < 0) {
            throw new IllegalArgumentException("maxRetryCount cannot be negative");
        }

        boolean hasProcessed = !SetUtils.isEmpty(processedIds);
        boolean hasFailed = !SetUtils.isEmpty(failedIds);
        Set<UUID> processedIdsCopy = SetUtils.mutableCopy(processedIds);

        if (!hasProcessed && !hasFailed) {
            log.warn("Outbox events batch has neither processed nor failed ids, nothing to finalize, lockToken={}", lockToken);
            return;
        }
        if (hasProcessed && hasFailed) {
            int processedCount = processedIdsCopy.size();
            if (processedIdsCopy.removeAll(failedIds)) {
                log.warn("{} ids are both processed and failed, treating them as failed, lockToken={}",
                        processedCount - processedIdsCopy.size(), lockToken);
            }
        }

        int expected = 0;
        int updated = 0;

        if (hasProcessed && !processedIdsCopy.isEmpty()) {
            expected += processedIdsCopy.size();
            updated += repository.updateBatchStatusByLockToken(processedIdsCopy, lockToken, EventStatus.PROCESSED);
        }

        if (hasFailed) {
            List<OutboxEvent> failedEvents = prepareFailedEvents(events, failedIds, maxRetryCount, retryDelaySupplier);
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
            Function<Integer, Duration> retryDelaySupplier
    ) {
        return events.stream()
                .filter(event -> failedIds.contains(event.getId()))
                .map(event -> {
                            EventStatus newStatus;
                            // because this is after try
                            int newRetryCount = event.getRetryCount() + 1;
                            Duration retryDelay;
                            if (newRetryCount < maxRetryCount) {
                                newStatus = EventStatus.PENDING;
                                // should count time for next try
                                retryDelay = retryDelaySupplier.apply(newRetryCount);
                            } else {
                                newStatus = EventStatus.FAILED;
                                // keeps next_retry_at as it was
                                retryDelay = null;
                            }

                            OutboxEvent newEvent = event.copy();
                            newEvent.setStatus(newStatus);
                            newEvent.setRetryCount(Math.min(newRetryCount, maxRetryCount));
                            newEvent.setRetryDelay(retryDelay);
                            return newEvent;
                })
                .toList();
    }

    @Transactional
    @Override
    public int recoverStuckBatch(Duration maxBatchProcessingTime, int batchSize) {
        int recoveredCount = repository.updateBatchStatusByStatusOlderThan(
                EventStatus.IN_PROCESS,
                maxBatchProcessingTime,
                batchSize,
                EventStatus.PENDING
        );
        if (recoveredCount > 0) {
            log.info("Recovered {} stuck outbox events back to PENDING, maxBatchProcessingTime={}, batchSize={}",
                    recoveredCount, maxBatchProcessingTime, batchSize);
        }
        return recoveredCount;
    }

    @Transactional
    @Override
    public int deleteProcessedBatch(Duration ttl, int batchSize) {
        return repository.deleteBatchByStatusOlderThan(EventStatus.PROCESSED, ttl, batchSize);
    }

    @Transactional
    @Override
    public int deleteBatch(Set<UUID> ids) {
        if (SetUtils.isEmpty(ids)) {
            return 0;
        }
        return repository.deleteBatch(ids);
    }
}
