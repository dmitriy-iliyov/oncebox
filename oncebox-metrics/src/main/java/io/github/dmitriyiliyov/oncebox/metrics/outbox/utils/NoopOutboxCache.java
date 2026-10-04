package io.github.dmitriyiliyov.oncebox.metrics.outbox.utils;

public final class NoopOutboxCache<S extends Enum<S>> implements OutboxCache<S> {

    @Override
    public Long getCount() {
        return null;
    }

    @Override
    public Long putCount(long count) {
        return count;
    }

    @Override
    public Long getCountByStatus(S status) {
        return null;
    }

    @Override
    public Long putCountByStatus(S status, long count) {
        return count;
    }

    @Override
    public Long getCountByEventTypeAndStatus(String eventType, S status) {
        return null;
    }

    @Override
    public Long putCountByEventTypeAndStatus(String eventType, S status, long count) {
        return count;
    }
}
