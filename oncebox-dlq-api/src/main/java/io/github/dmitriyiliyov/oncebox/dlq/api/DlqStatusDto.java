package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "DLQ status container")
public record DlqStatusDto(

        @Schema(
                description = "DLQ status to set as new event status, any but IN_PROCESS",
                example = "RESOLVED",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "status must not be null")
        @NotInProcess
        DlqStatus status
) {}
