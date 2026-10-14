package io.github.dmitriyiliyov.oncebox.core.inbox;

import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class DefaultInboxManager implements InboxManager {

    protected final InboxRepository repository;

    public DefaultInboxManager(InboxRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
    }

    @Transactional
    @Override
    public boolean tryConsume(UUID id) {
        return repository.saveIfAbsent(id) == 1;
    }

    @Transactional
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
        return repository.deleteBatchOlderThan(ttl, batchSize);
    }
}
