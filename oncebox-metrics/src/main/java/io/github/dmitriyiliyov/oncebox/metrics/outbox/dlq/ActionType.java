package io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq;

/**
 * Defines tags for the {@code outbox_dlq_events_by_action_type_rate_total} metric.
 * Used by decorators to track the lifecycle of events within the DLQ.
 */
public enum ActionType {

    /**
     * Incremented upon attempt to move events from the DLQ to the main outbox table.
     */
    ATTEMPT_MOVE_TO_OUTBOX,

    /**
     * Incremented upon the automatic deletion of a batch from the DLQ by the recovery process.
     */
    SUCCESS_MOVED_TO_OUTBOX,

    /**
     * Manual deletion of events from the DLQ throw the REST API.
     */
    MANUAL_DELETED,

    /**
     * Incremented when resolved events are deleted from the DLQ.
     */
    CLEANED;
}
