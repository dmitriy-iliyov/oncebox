package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.dlq.api.exception.InvalidDlqFilterException;
import io.github.dmitriyiliyov.oncebox.dlq.api.exception.NotFoundException;
import io.github.dmitriyiliyov.oncebox.dlq.api.exception.OutboxDlqEventInProcessException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice(basePackageClasses = OutboxDlqController.class)
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class OutboxDlqControllerAdvice extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqControllerAdvice.class);

    private final Clock clock;

    public OutboxDlqControllerAdvice(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFoundException(NotFoundException e, HttpServletRequest request) {
        log.debug("DLQ event not found: {}", request.getRequestURI());
        return ProblemDetailFactory.dlqEventNotFound(e.getDetail(), request.getRequestURI(), clock.instant());
    }

    @ExceptionHandler(OutboxDlqEventInProcessException.class)
    public ProblemDetail handleOutboxDlqEventInProcessException(OutboxDlqEventInProcessException e,
                                                                HttpServletRequest request) {
        log.debug("DLQ event is in process: {}", request.getRequestURI());
        return ProblemDetailFactory.dlqEventInProcess(e.getDetail(), request.getRequestURI(), clock.instant());
    }

    @ExceptionHandler(InvalidDlqFilterException.class)
    public ProblemDetail handleInvalidDlqFilterException(InvalidDlqFilterException e, HttpServletRequest request) {
        log.debug("Invalid DLQ filter: {}", request.getRequestURI());
        return ProblemDetailFactory.invalidDlqFilter(e.getDetail(), request.getRequestURI(), clock.instant());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception e, HttpServletRequest request) {
        log.error("Unexpected error: {}", request.getRequestURI(), e);
        return ProblemDetailFactory.internalError(request.getRequestURI(), clock.instant());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, String>> errors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(Map.of("field", error.getField(), "message", fieldMessage(error)));
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            errors.add(Map.of("message", Objects.requireNonNullElse(error.getDefaultMessage(), "is invalid")));
        }
        ProblemDetail body = ProblemDetailFactory.validationFailed(errors, requestUri(request), clock.instant());
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             @Nullable Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        String uri = requestUri(request);
        if (statusCode.is5xxServerError()) {
            log.error("DLQ API request failed: {}", uri, ex);
        } else {
            log.debug("DLQ API request rejected with {}: {} {}", statusCode.value(), ex.getClass().getSimpleName(), uri);
        }
        if (body instanceof ProblemDetail problemDetail) {
            ProblemDetailFactory.stamp(problemDetail, uri, clock.instant());
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private String fieldMessage(FieldError error) {
        if (!error.isBindingFailure()) {
            return Objects.requireNonNullElse(error.getDefaultMessage(), error.getField() + " is invalid");
        }
        if (error.getRejectedValue() == null) {
            return error.getField() + " must be provided";
        }
        Class<?> requiredType = error.contains(TypeMismatchException.class)
                ? error.unwrap(TypeMismatchException.class).getRequiredType()
                : null;
        if (requiredType != null && requiredType.isEnum()) {
            String constants = Arrays.stream(requiredType.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            return "%s must be one of %s".formatted(error.getField(), constants);
        }
        String typeName = requiredType == null ? "value" : requiredType.getSimpleName();
        return "%s must be a valid %s".formatted(error.getField(), typeName);
    }

    private String requestUri(WebRequest request) {
        return ((ServletWebRequest) request).getRequest().getRequestURI();
    }
}
