package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

/**
 * Represents the lifecycle status of a DLQ event.
 */
public enum DlqStatus {

    /**
     * Event has been moved to the DLQ from the main outbox.
     */
    MOVED,

    /**
     * Event is currently being processed and is locked.
     */
    IN_PROCESS,

    /**
     * Event has been successfully resolved and requires no further action.
     */
    RESOLVED,

    /**
     * Event is scheduled to be moved back to the main outbox for retry.
     */
    TO_RETRY;

    @JsonCreator
    public static DlqStatus fromString(String value) {
        Objects.requireNonNull(value, "value cannot be null");
        try {
            return valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown DlqStatus %s".formatted(value));
        }
    }
}
