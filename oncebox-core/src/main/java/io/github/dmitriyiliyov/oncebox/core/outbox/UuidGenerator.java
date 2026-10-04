package io.github.dmitriyiliyov.oncebox.core.outbox;

import java.util.UUID;

/**
 * Generates the id of a new outbox event.
 * <p>
 * The id travels with the event to the consuming service, where the inbox deduplicates by it, so every call has
 * to return a globally unique value. The default implementation produces time-ordered UUIDv7 values, which keep
 * inserts at the tail of the primary key index.
 */
public interface UuidGenerator {
    UUID generate();
}
