package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.dlq.api.exception.InvalidDlqFilterException;
import io.github.dmitriyiliyov.oncebox.dlq.api.exception.OutboxDlqEventInProcessException;
import io.github.dmitriyiliyov.oncebox.dlq.api.exception.OutboxDlqEventNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxDlqControllerAdviceUnitTests {

    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");
    private static final String URI_PATH = "/api/outbox-dlq/events/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", URI_PATH);
    private final OutboxDlqControllerAdvice tested = new OutboxDlqControllerAdvice(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("UT constructor when clock is null should throw NullPointerException")
    void constructor_whenClockIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new OutboxDlqControllerAdvice(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("clock cannot be null");
    }

    @Test
    @DisplayName("UT handleNotFoundException() should answer 404 with the exception detail")
    void handleNotFoundException_shouldAnswer404() {
        // when
        ProblemDetail problemDetail = tested.handleNotFoundException(new OutboxDlqEventNotFoundException(EVENT_ID), request);

        // then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problemDetail.getType()).isEqualTo(ProblemTypes.DLQ_EVENT_NOT_FOUND);
        assertThat(problemDetail.getTitle()).isEqualTo("DLQ event not found");
        assertThat(problemDetail.getDetail()).isEqualTo("No OutboxDlqEvent found with id=" + EVENT_ID);
        assertStamped(problemDetail);
    }

    @Test
    @DisplayName("UT handleOutboxDlqEventInProcessException() should answer 409 with the exception detail")
    void handleOutboxDlqEventInProcessException_shouldAnswer409() {
        // when
        ProblemDetail problemDetail =
                tested.handleOutboxDlqEventInProcessException(new OutboxDlqEventInProcessException(EVENT_ID), request);

        // then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problemDetail.getType()).isEqualTo(ProblemTypes.DLQ_EVENT_IN_PROCESS);
        assertThat(problemDetail.getTitle()).isEqualTo("DLQ event is in process");
        assertThat(problemDetail.getDetail()).contains(EVENT_ID.toString());
        assertStamped(problemDetail);
    }

    @Test
    @DisplayName("UT handleInvalidDlqFilterException() should answer 400 with the exception detail")
    void handleInvalidDlqFilterException_shouldAnswer400() {
        // when
        ProblemDetail problemDetail =
                tested.handleInvalidDlqFilterException(new InvalidDlqFilterException("Filter hasn't both params"), request);

        // then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getType()).isEqualTo(ProblemTypes.INVALID_DLQ_FILTER);
        assertThat(problemDetail.getDetail()).isEqualTo("Filter hasn't both params");
        assertStamped(problemDetail);
    }

    @Test
    @DisplayName("UT handleUnexpectedException() should answer 500 with a fixed detail, never the cause")
    void handleUnexpectedException_shouldAnswer500WithFixedDetail() {
        // given
        Exception exception = new DataAccessResourceFailureException(
                "select * from outbox_dlq_events", new RuntimeException("password authentication failed")
        );

        // when
        ProblemDetail problemDetail = tested.handleUnexpectedException(exception, request);

        // then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problemDetail.getType()).isEqualTo(ProblemTypes.INTERNAL_ERROR);
        assertThat(problemDetail.getTitle()).isEqualTo("Internal server error");
        assertThat(problemDetail.getDetail()).isEqualTo("The operation could not be completed");
        assertStamped(problemDetail);
    }

    @Test
    @DisplayName("UT handleMethodArgumentNotValid() should list each field error under errors")
    void handleMethodArgumentNotValid_whenConstraintViolated_shouldListFieldErrors() {
        // given
        MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "request");
        bindingResult.addError(new FieldError("request", "batchSize", "batchSize must be at least 10"));

        // when
        ProblemDetail problemDetail = validationProblem(bindingResult);

        // then
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problemDetail.getType()).isEqualTo(ProblemTypes.VALIDATION_FAILED);
        assertThat(errors(problemDetail))
                .containsExactly(Map.of("field", "batchSize", "message", "batchSize must be at least 10"));
        assertStamped(problemDetail);
    }

    @Test
    @DisplayName("UT handleMethodArgumentNotValid() when an enum fails to bind should list its constants, not Java types")
    void handleMethodArgumentNotValid_whenEnumFailsToBind_shouldListConstants() {
        // given
        MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "request");
        bindingResult.addError(bindingFailure("status", "FOO", new TypeMismatchException("FOO", DlqStatus.class)));

        // when
        ProblemDetail problemDetail = validationProblem(bindingResult);

        // then
        assertThat(errors(problemDetail)).containsExactly(Map.of(
                "field", "status",
                "message", "status must be one of MOVED, IN_PROCESS, RESOLVED, TO_RETRY"
        ));
    }

    @Test
    @DisplayName("UT handleMethodArgumentNotValid() when a required primitive is missing should say it must be provided")
    void handleMethodArgumentNotValid_whenPrimitiveMissing_shouldSayMustBeProvided() {
        // given
        MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "request");
        bindingResult.addError(bindingFailure("batchNumber", null, new TypeMismatchException((Object) null, int.class)));

        // when
        ProblemDetail problemDetail = validationProblem(bindingResult);

        // then
        assertThat(errors(problemDetail))
                .containsExactly(Map.of("field", "batchNumber", "message", "batchNumber must be provided"));
    }

    @Test
    @DisplayName("UT handleMethodArgumentNotValid() when a number fails to bind should name the required type")
    void handleMethodArgumentNotValid_whenNumberFailsToBind_shouldNameRequiredType() {
        // given
        MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "request");
        bindingResult.addError(bindingFailure("batchNumber", "abc", new TypeMismatchException("abc", int.class)));

        // when
        ProblemDetail problemDetail = validationProblem(bindingResult);

        // then
        assertThat(errors(problemDetail))
                .containsExactly(Map.of("field", "batchNumber", "message", "batchNumber must be a valid int"));
    }

    @Test
    @DisplayName("UT handleMethodArgumentNotValid() when a binding failure carries no type mismatch should not guess a type")
    void handleMethodArgumentNotValid_whenBindingFailureWithoutTypeMismatch_shouldSayValidValue() {
        // given
        MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "request");
        bindingResult.addError(new FieldError("request", "eventType", "x", true, null, null, "Failed to bind"));

        // when
        ProblemDetail problemDetail = validationProblem(bindingResult);

        // then
        assertThat(errors(problemDetail))
                .containsExactly(Map.of("field", "eventType", "message", "eventType must be a valid value"));
    }

    @Test
    @DisplayName("UT handleMethodArgumentNotValid() when an error is not about a field should list its message without a field")
    void handleMethodArgumentNotValid_whenGlobalError_shouldListMessageWithoutField() {
        // given
        MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "request");
        bindingResult.addError(new ObjectError("request", "request must select events"));

        // when
        ProblemDetail problemDetail = validationProblem(bindingResult);

        // then
        assertThat(errors(problemDetail)).containsExactly(Map.of("message", "request must select events"));
    }

    @Test
    @DisplayName("UT handleExceptionInternal() when a standard Spring failure is a 5xx should keep its status and stamp it")
    void handleExceptionInternal_whenStandard5xx_shouldKeepStatusAndStamp() {
        // given
        ProblemDetail body = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);

        // when
        ResponseEntity<Object> response = tested.handleExceptionInternal(
                new AsyncRequestTimeoutException(), body, new HttpHeaders(), HttpStatus.SERVICE_UNAVAILABLE,
                new ServletWebRequest(request)
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertStamped((ProblemDetail) response.getBody());
    }

    @Test
    @DisplayName("UT handleExceptionInternal() should stamp path and timestamp onto a standard Spring problem")
    void handleExceptionInternal_whenStandardProblem_shouldStampPathAndTimestamp() {
        // given
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Failed to read request");

        // when
        ResponseEntity<Object> response = tested.handleExceptionInternal(
                new IllegalStateException(), body, new HttpHeaders(), HttpStatus.BAD_REQUEST, new ServletWebRequest(request)
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail problemDetail = (ProblemDetail) response.getBody();
        assertThat(problemDetail.getType()).isEqualTo(URI.create("about:blank"));
        assertStamped(problemDetail);
    }

    private ProblemDetail validationProblem(MapBindingResult bindingResult) {
        ResponseEntity<Object> response = tested.handleMethodArgumentNotValid(
                new MethodArgumentNotValidException(null, bindingResult),
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                new ServletWebRequest(request)
        );
        return (ProblemDetail) response.getBody();
    }

    private FieldError bindingFailure(String field, Object rejectedValue, TypeMismatchException cause) {
        FieldError error = new FieldError("request", field, rejectedValue, true, null, null, "Failed to convert");
        error.wrap(cause);
        return error;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> errors(ProblemDetail problemDetail) {
        return (List<Map<String, String>>) problemDetail.getProperties().get("errors");
    }

    private void assertStamped(ProblemDetail problemDetail) {
        assertThat(problemDetail.getInstance()).isEqualTo(URI.create(URI_PATH));
        assertThat(problemDetail.getProperties())
                .containsEntry("path", URI_PATH)
                .containsEntry("timestamp", NOW);
    }
}
