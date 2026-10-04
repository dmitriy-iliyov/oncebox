package io.github.dmitriyiliyov.oncebox.core.inbox;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class DefaultInboxManager implements InboxManager {

    protected final InboxRepository repository;
    protected final Clock clock;

    public DefaultInboxManager(InboxRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public boolean tryConsume(UUID id) {
        return repository.saveIfAbsent(id) == 1;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public Set<UUID> tryConsumeAndGetDuplicates(Set<UUID> ids) {
        Set<UUID> unconsumedIds = repository.saveIfAbsent(ids);
        return ids.stream()
                .filter(id -> !unconsumedIds.contains(id))
                .collect(Collectors.toSet());
    }

    @Transactional
    @Override
    public int cleanBatchByTtl(Duration ttl, int batchSize) {
        Instant threshold = clock.instant().minusMillis(ttl.toMillis());
        return repository.deleteBatchByThreshold(threshold, batchSize);
    }
}
