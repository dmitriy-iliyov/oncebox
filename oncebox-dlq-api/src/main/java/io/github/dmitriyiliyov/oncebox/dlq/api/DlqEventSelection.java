package io.github.dmitriyiliyov.oncebox.dlq.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A batch request that selects DLQ events either by ids or by event type, never by both.
 */
public interface DlqEventSelection {

    List<String> FIELD_NAMES = List.of("ids", "eventType");

    Set<UUID> ids();

    String eventType();

    default boolean hasValidIds() {
        return ids() != null && !ids().isEmpty();
    }

    default boolean hasValidEventType() {
        return eventType() != null && !eventType().isBlank();
    }
}
