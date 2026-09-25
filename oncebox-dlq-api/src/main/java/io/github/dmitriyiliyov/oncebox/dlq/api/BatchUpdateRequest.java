package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

@Schema(description = "Batch update request for DLQ events")
@ExactlyOneSelector
public record BatchUpdateRequest(

        @Schema(
                description = "List of event ids to update, not together with eventType",
                example = "[\"550e8400-e29b-41d4-a716-446655440000\", \"550e8400-e29b-41d4-a716-446655440001\"]",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 1000, message = "ids must contain at most {max} elements")
        Set<UUID> ids,

        @Schema(
                description = "Type of events to update, not together with ids",
                example = "event-type",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String eventType,

        @Schema(
                description = "New status to assign to all specified events, any but IN_PROCESS",
                example = "RESOLVED",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "status must not be null")
        @NotInProcess
        DlqStatus status

) implements DlqEventSelection { }
