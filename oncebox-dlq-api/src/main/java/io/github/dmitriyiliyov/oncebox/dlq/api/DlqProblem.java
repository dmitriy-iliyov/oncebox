package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * The body of every error response of the DLQ API as the client reads it, for the OpenAPI document only.
 * springdoc reads the Java class of {@link org.springframework.http.ProblemDetail} and does not see Jackson's mixin
 * that lifts {@code properties} to the top level, so it would document a {@code properties} object the body does
 * not have. The components are the keys {@link ProblemDetailFactory} puts into the real body.
 */
@Schema(description = "Error response of the DLQ API, application/problem+json (RFC 9457)")
public record DlqProblem(

        @Schema(
                description = """
                        URI naming the kind of error, the only field a client may branch on: \
                        dlq-event-not-found, dlq-event-in-process, invalid-dlq-filter, validation-failed""",
                example = "https://oncebox.io/errors/validation-failed",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String type,

        @Schema(
                description = "Short summary of the kind of error, the same for every occurrence of it",
                example = "Validation failed",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String title,

        @Schema(
                description = "HTTP status code",
                example = "400",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        int status,

        @Schema(
                description = "Explanation of this occurrence of the error",
                example = "Request validation failed",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String detail,

        @Schema(
                description = "URI of the request that failed",
                example = "/api/outbox-dlq/events/batch",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String instance,

        @Schema(
                description = "Path of the request that failed",
                example = "/api/outbox-dlq/events/batch",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String path,

        @Schema(
                description = "Moment the error was produced",
                example = "2026-09-30T10:15:30.123456Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant timestamp,

        @Schema(
                description = "Invalid values, present only when the request failed validation",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        List<DlqFieldError> errors

) {

    @Schema(description = "One invalid value of the request")
    public record DlqFieldError(

            @Schema(
                    description = "JSON name of the invalid field, absent when the violation concerns the whole request",
                    example = "batchSize",
                    requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            String field,

            @Schema(
                    description = "Why the value is invalid",
                    example = "batchSize must be at least 10",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String message

    ) {
    }
}
