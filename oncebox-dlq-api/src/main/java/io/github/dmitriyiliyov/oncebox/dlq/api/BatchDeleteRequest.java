package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

@Schema(description = "Batch delete request for DLQ events")
@ExactlyOneSelector
public record BatchDeleteRequest(

        @Schema(
                description = "List of event ids to delete, not together with eventType",
                example = "[\"550e8400-e29b-41d4-a716-446655440000\", \"550e8400-e29b-41d4-a716-446655440001\"]",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 1000, message = "ids must contain at most {max} elements")
        Set<UUID> ids,

        @Schema(
                description = "Type of events to delete, not together with ids",
                example = "event-type",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String eventType

) implements DlqEventSelection { }
