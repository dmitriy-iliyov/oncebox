package io.github.dmitriyiliyov.oncebox.core.outbox.domain;

/**
 * Outbox event state machine.
 */
public enum EventStatus {
    /**
     * The initial state of an event after it has been created and saved.
     * <p>
     * It is waiting to be picked up by the publisher.
     */
    PENDING,

    /**
     * The event has been picked up by a publisher and is currently being processed.
     * <p>
     * This is a transient state.
     */
    IN_PROCESS,

    /**
     * The event has been successfully published to the message broker.
     * <p>
     * It is now eligible for cleanup.
     */
    PROCESSED,

    /**
     * The event has failed to be published after all retry attempts.
     * <p>
     * It may be moved to a DLQ for manual inspection.
     */
    FAILED;

    /**
     * Converts a string value to an {@link EventStatus} enum constant.
     *
     * @param value The string representation of the status.
     * @return      The corresponding {@link EventStatus}.
     * @throws      IllegalArgumentException if the string value is unknown.
     */
    public static EventStatus fromString(String value) {
        if (value == null) {
            return PENDING;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown EventStatus: " + value);
        }
    }
}
