package io.github.dmitriyiliyov.oncebox.dlq.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The single table of "what went wrong" -> status, {@link ProblemTypes type} and title.
 */
final class ProblemDetailFactory {

    static final String INTERNAL_ERROR_DETAIL = "The operation could not be completed";

    private static final String PATH = "path";
    private static final String TIMESTAMP = "timestamp";
    private static final String ERRORS = "errors";

    private ProblemDetailFactory() {}

    static ProblemDetail dlqEventNotFound(String detail, String instance, Instant timestamp) {
        return problem(
                HttpStatus.NOT_FOUND,
                ProblemTypes.DLQ_EVENT_NOT_FOUND,
                "DLQ event not found",
                detail,
                instance,
                timestamp
        );
    }

    static ProblemDetail dlqEventInProcess(String detail, String instance, Instant timestamp) {
        return problem(
                HttpStatus.CONFLICT,
                ProblemTypes.DLQ_EVENT_IN_PROCESS,
                "DLQ event is in process",
                detail,
                instance,
                timestamp
        );
    }

    static ProblemDetail invalidDlqFilter(String detail, String instance, Instant timestamp) {
        return problem(
                HttpStatus.BAD_REQUEST,
                ProblemTypes.INVALID_DLQ_FILTER,
                "Invalid DLQ filter",
                detail,
                instance,
                timestamp
        );
    }

    static ProblemDetail validationFailed(List<Map<String, String>> errors, String instance, Instant timestamp) {
        ProblemDetail problemDetail = problem(
                HttpStatus.BAD_REQUEST,
                ProblemTypes.VALIDATION_FAILED,
                "Validation failed",
                "Request validation failed",
                instance,
                timestamp
        );
        problemDetail.setProperty(ERRORS, errors);
        return problemDetail;
    }

    static ProblemDetail internalError(String instance, Instant timestamp) {
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ProblemTypes.INTERNAL_ERROR,
                "Internal server error",
                INTERNAL_ERROR_DETAIL,
                instance,
                timestamp
        );
    }

    static void stamp(ProblemDetail problemDetail, String instance, Instant timestamp) {
        problemDetail.setInstance(URI.create(instance));
        problemDetail.setProperty(PATH, instance);
        problemDetail.setProperty(TIMESTAMP, timestamp);
    }

    private static ProblemDetail problem(HttpStatus status,
                                         URI type,
                                         String title,
                                         String detail,
                                         String instance,
                                         Instant timestamp) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(type);
        problemDetail.setTitle(title);
        stamp(problemDetail, instance, timestamp);
        return problemDetail;
    }
}
