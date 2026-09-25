package io.github.dmitriyiliyov.oncebox.dlq.api;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static io.github.dmitriyiliyov.oncebox.dlq.api.DlqEventSelection.FIELD_NAMES;

public class ExactlyOneSelectorValidator implements ConstraintValidator<ExactlyOneSelector, DlqEventSelection> {

    @Override
    public boolean isValid(DlqEventSelection selection, ConstraintValidatorContext context) {
        if (selection == null || selection.hasValidIds() ^ selection.hasValidEventType()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        for (String field : FIELD_NAMES) {
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(field)
                    .addConstraintViolation();
        }
        return false;
    }
}
