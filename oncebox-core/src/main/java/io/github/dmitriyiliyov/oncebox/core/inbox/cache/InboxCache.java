package io.github.dmitriyiliyov.oncebox.core.inbox.cache;

import java.util.UUID;

/**
 * A cache of the IDs of events that have already been consumed.
 * <p>
 * It answers whether an event has been consumed and records one as consumed, so that
 * a repeated delivery can be skipped without a database round trip.
 */
public interface InboxCache {

    /**
     * Checks whether the event with the given ID has already been consumed.
     *
     * @param id the unique identifier of the event.
     * @return   {@code true} if the event is present in the cache (consumed), {@code false} otherwise.
     */
    boolean isConsumed(UUID id);

    /**
     * Marks the event with the given ID as consumed by adding it to the cache.
     *
     * @param id the unique identifier of the event to cache.
     */
    void consume(UUID id);

    /**
     * An implementation that never reports an event as consumed, so every check goes to the inbox table.
     */
    InboxCache NOOP_CACHE = new InboxCache() {
        @Override
        public boolean isConsumed(UUID id) {
            return false;
        }

        @Override
        public void consume(UUID id) { }
    };
}
