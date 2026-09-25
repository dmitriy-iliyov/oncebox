package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the constraint through a real {@link Validator}: which property nodes it reports is its contract, and a
 * mocked {@code ConstraintValidatorContext} would only restate the builder calls.
 */
class ExactlyOneSelectorValidatorUnitTests {

    private static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("UT isValid() when only ids provided should pass")
    void isValid_whenOnlyIdsProvided_shouldPass() {
        // when
        Set<ConstraintViolation<BatchUpdateRequest>> violations =
                validator.validate(new BatchUpdateRequest(Set.of(EVENT_ID), null, DlqStatus.RESOLVED));

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UT isValid() when only event type provided should pass")
    void isValid_whenOnlyEventTypeProvided_shouldPass() {
        // when
        Set<ConstraintViolation<BatchDeleteRequest>> violations =
                validator.validate(new BatchDeleteRequest(null, "event-type"));

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UT isValid() when both provided should report ids and eventType")
    void isValid_whenBothProvided_shouldReportBothFields() {
        // when
        Set<ConstraintViolation<BatchUpdateRequest>> violations =
                validator.validate(new BatchUpdateRequest(Set.of(EVENT_ID), "event-type", DlqStatus.RESOLVED));

        // then
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("ids", "eventType");
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsOnly("exactly one of ids and eventType must be provided");
    }

    @Test
    @DisplayName("UT isValid() when empty ids and blank event type should report ids and eventType")
    void isValid_whenEmptyIdsAndBlankEventType_shouldReportBothFields() {
        // when
        Set<ConstraintViolation<BatchDeleteRequest>> violations =
                validator.validate(new BatchDeleteRequest(Set.of(), "  "));

        // then
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("ids", "eventType");
    }

    @Test
    @DisplayName("UT isValid() when neither provided should report ids and eventType")
    void isValid_whenNeitherProvided_shouldReportBothFields() {
        // when
        Set<ConstraintViolation<BatchDeleteRequest>> violations =
                validator.validate(new BatchDeleteRequest(null, null));

        // then
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("ids", "eventType");
    }

    @Test
    @DisplayName("UT isValid() when the request itself is null should leave it to @NotNull")
    void isValid_whenSelectionIsNull_shouldPass() {
        // when / then
        assertThat(new ExactlyOneSelectorValidator().isValid(null, null)).isTrue();
    }
}
