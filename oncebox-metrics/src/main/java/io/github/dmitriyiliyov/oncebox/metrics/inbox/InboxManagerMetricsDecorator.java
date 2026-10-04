package io.github.dmitriyiliyov.oncebox.metrics.inbox;

import io.github.dmitriyiliyov.oncebox.core.inbox.InboxManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class InboxManagerMetricsDecorator implements InboxManager {

    private final Counter rejectedDuplicates;
    private final Counter consumed;
    private final Counter cleaned;
    private final InboxManager delegate;

    public InboxManagerMetricsDecorator(MeterRegistry registry, InboxManager delegate) {
        Objects.requireNonNull(registry, "registry cannot be null");
        this.rejectedDuplicates = registry.counter("inbox_events_total", "type", "rejected_duplicates");
        this.consumed = registry.counter("inbox_events_total", "type", "consumed");
        this.cleaned = registry.counter("inbox_events_total", "type", "cleaned");
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    }

    @Override
    public boolean tryConsume(UUID id) {
        boolean isConsumed = delegate.tryConsume(id);
        if (isConsumed) {
            consumed.increment();
        } else {
            rejectedDuplicates.increment();
        }
        return isConsumed;
    }

    @Override
    public Set<UUID> tryConsumeAndGetDuplicates(Set<UUID> ids) {
        Set<UUID> duplicates = delegate.tryConsumeAndGetDuplicates(ids);
        if (!duplicates.isEmpty()) {
            double duplicatesCount = duplicates.size();
            rejectedDuplicates.increment(duplicatesCount);
            consumed.increment(ids.size() - duplicatesCount);
        } else {
            consumed.increment(ids.size());
        }
        return duplicates;
    }

    @Override
    public int cleanBatchByTtl(Duration ttl, int batchSize) {
        int cleanedCount = delegate.cleanBatchByTtl(ttl, batchSize);
        cleaned.increment(cleanedCount);
        return cleanedCount;
    }
}
