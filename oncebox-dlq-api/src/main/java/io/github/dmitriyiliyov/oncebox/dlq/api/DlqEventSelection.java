package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.utils.SetUtils;

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
        return !SetUtils.isEmpty(ids());
    }

    default boolean hasValidEventType() {
        return eventType() != null && !eventType().isBlank();
    }
}
