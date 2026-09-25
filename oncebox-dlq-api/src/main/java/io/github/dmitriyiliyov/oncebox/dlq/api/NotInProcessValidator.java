package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotInProcessValidator implements ConstraintValidator<NotInProcess, DlqStatus> {

    @Override
    public boolean isValid(DlqStatus status, ConstraintValidatorContext context) {
        return !DlqStatus.IN_PROCESS.equals(status);
    }
}
