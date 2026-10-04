package io.github.dmitriyiliyov.oncebox.metrics.inbox;

import io.github.dmitriyiliyov.oncebox.core.inbox.cache.InboxCacheListener;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Objects;

public class MetricsInboxCacheListener implements InboxCacheListener {

    private final Counter hits;
    private final Counter misses;

    public MetricsInboxCacheListener(MeterRegistry registry) {
        Objects.requireNonNull(registry, "registry cannot be null");
        this.hits = registry.counter("inbox_cache_action_total", "action_type", "hit");
        this.misses = registry.counter("inbox_cache_action_total", "action_type", "miss");
    }

    @Override
    public void onHit() {
        hits.increment();
    }

    @Override
    public void onMiss() {
        misses.increment();
    }
}
