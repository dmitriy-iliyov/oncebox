package io.github.dmitriyiliyov.oncebox.metrics.outbox.utils;

/**
 * Defines a caching mechanism for outbox-related metrics.
 * This interface allows for storing and retrieving various counts,
 * potentially for performance optimization in metrics collection.
 *
 * @param <S> The type of the status enum used for filtering metrics (e.g., EventStatus, DlqStatus).
 */
public interface OutboxCache<S extends Enum<S>> {

    /**
     * Retrieves the total cached count.
     *
     * @return The total count, or {@code null} if not cached.
     */
    Long getCount();

    /**
     * Caches and returns the total count.
     *
     * @param count The total count to cache.
     * @return      The cached total count.
     */
    Long putCount(long count);

    /**
     * Retrieves the cached count of events for a specific status.
     *
     * @param status The status to filter by.
     * @return       The count for the given status, or {@code null} if not cached.
     */
    Long getCountByStatus(S status);

    /**
     * Caches and returns the count of events for a specific status.
     *
     * @param status The status to filter by.
     * @param count  The count for the given status to cache.
     * @return       The cached count for the given status.
     */
    Long putCountByStatus(S status, long count);

    /**
     * Retrieves the cached count of events for a specific event type and status.
     *
     * @param eventType The event type to filter by.
     * @param status    The status to filter by.
     * @return          The count for the given event type and status, or {@code null} if not cached.
     */
    Long getCountByEventTypeAndStatus(String eventType, S status);

    /**
     * Caches and returns the count of events for a specific event type and status.
     *
     * @param eventType The event type to filter by.
     * @param status    The status to filter by.
     * @param count     The count for the given event type and status to cache.
     * @return          The cached count for the given event type and status.
     */
    Long putCountByEventTypeAndStatus(String eventType, S status, long count);
}
