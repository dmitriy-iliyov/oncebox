package io.github.dmitriyiliyov.oncebox.metrics.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.CacheHelper;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.OutboxCache;

import java.util.Objects;

public class DefaultOutboxMetricsService implements OutboxMetricsService {

    private final OutboxMetricsRepository repository;
    private final OutboxCache<EventStatus> cache;

    public DefaultOutboxMetricsService(OutboxMetricsRepository repository, OutboxCache<EventStatus> cache) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.cache = Objects.requireNonNull(cache, "cache cannot be null");
    }

    @Override
    public long count() {
        return CacheHelper.count(cache, repository::count);
    }

    @Override
    public long countByStatus(EventStatus status) {
        return CacheHelper.countByStatus(cache, status, repository::countByStatus);
    }

    @Override
    public long countByEventTypeAndStatus(String eventType, EventStatus status) {
        return CacheHelper.countByEventTypeAndStatus(cache, eventType, status, repository::countByEventTypeAndStatus);
    }
}
