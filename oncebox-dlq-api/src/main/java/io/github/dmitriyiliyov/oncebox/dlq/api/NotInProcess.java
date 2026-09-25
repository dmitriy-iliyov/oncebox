package io.github.dmitriyiliyov.oncebox.dlq.api;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * The target {@code DlqStatus} is not {@code IN_PROCESS}: that status belongs to the transfer job, and an event
 * a client put into it could no longer be updated or deleted through the API.
 */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = NotInProcessValidator.class)
public @interface NotInProcess {

    String message() default "status must not be IN_PROCESS";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
