package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents the aggregated result of a batch operation.
 * <p>
 * Indicates whether all requested entities were processed or only a subset.
 */
@Schema(
        description = "Aggregated outcome of a batch operation: SUCCESS - all requested entities were processed, "
                + "PARTIAL_SUCCESS - only part of the requested entities was processed, "
                + "POSSIBLE_PARTIAL_SUCCESS - the operation may have processed only a subset of matching entities",
        enumAsRef = true
)
public enum OperationStatus {

    /**
     * All requested entities were successfully processed.
     */
    SUCCESS,

    /**
     * Only a subset of requested entities was processed.
     * <p>
     * Typically occurs when some entities cannot be processed due to constraints
     * (e.g. being in IN_PROCESS state).
     */
    PARTIAL_SUCCESS,

    /**
     * The exact outcome is unknown, but it is possible that only a subset
     * of requested entities was processed due to constraints (e.g. being in IN_PROCESS state).
     */
    POSSIBLE_PARTIAL_SUCCESS
}
