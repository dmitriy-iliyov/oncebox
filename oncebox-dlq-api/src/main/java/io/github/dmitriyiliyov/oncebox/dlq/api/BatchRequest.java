package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Batch get request for DLQ events")
public record BatchRequest(

        @Schema(
                description = "Filter by event type",
                example = "event-type",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String eventType,

        @Schema(
                description = "Filter by DLQ status",
                example = "MOVED",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        DlqStatus status,

        @Schema(
                description = "Zero-based batch index (pagination offset in batches)",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @PositiveOrZero(message = "batchNumber must not be negative")
        int batchNumber,

        @Schema(
                description = "Number of events per batch",
                example = "50",
                minimum = "10",
                maximum = "100",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Min(value = 10, message = "batchSize must be at least {value}")
        @Max(value = 100, message = "batchSize must be at most {value}")
        int batchSize
) { }
