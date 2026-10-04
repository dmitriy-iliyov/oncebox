package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

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

public class DefaultOutboxDlqManager implements OutboxDlqManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxDlqManager.class);

    private final OutboxDlqRepository repository;
    private final Clock clock;

    public DefaultOutboxDlqManager(OutboxDlqRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Transactional
    @Override
    public void saveBatch(List<OutboxDlqEvent> events) {
        if (events == null || events.isEmpty()) {
            log.debug("List of submitted events is empty, so saving was rejected");
            return;
        }
        repository.saveBatch(events);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public List<OutboxDlqEvent> loadAndLockBatch(DlqStatus status, int batchSize) {
        return repository.findAndLockBatchByStatus(status, batchSize, DlqStatus.IN_PROCESS);
    }

    @Transactional
    @Override
    public int deleteBatch(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            log.debug("List of submitted ids is empty, so deleting was rejected");
            return 0;
        }
        return repository.deleteBatch(ids);
    }

    @Transactional
    @Override
    public int deleteResolvedBatch(Duration ttl, int batchSize) {
        Objects.requireNonNull(ttl, "ttl cannot be null");
        Instant threshold = clock.instant().minusMillis(ttl.toMillis());
        return repository.deleteBatchByStatusAndThreshold(DlqStatus.RESOLVED, threshold, batchSize);
    }
}
