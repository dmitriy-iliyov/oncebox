package io.github.dmitriyiliyov.oncebox.dlq.api;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Exactly one of {@code ids} and {@code eventType} of a {@link DlqEventSelection} is given; a violation is
 * reported on both fields, so the client sees the names it sent.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ExactlyOneSelectorValidator.class)
public @interface ExactlyOneSelector {

    String message() default "exactly one of ids and eventType must be provided";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
