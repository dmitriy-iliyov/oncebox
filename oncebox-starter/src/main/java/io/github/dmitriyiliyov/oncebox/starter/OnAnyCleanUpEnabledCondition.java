package io.github.dmitriyiliyov.oncebox.starter;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnAnyCleanUpEnabledCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Boolean isOutboxCleanUpEnabled = context.getEnvironment().getProperty("oncebox.outbox.clean-up.enabled", Boolean.class);
        Boolean isInboxCleanUpEnabled = context.getEnvironment().getProperty("oncebox.inbox.clean-up.enabled", Boolean.class);
        if (isOutboxCleanUpEnabled == null || isOutboxCleanUpEnabled) {
            return ConditionOutcome.match();
        }
        if (isInboxCleanUpEnabled != null && isInboxCleanUpEnabled) {
            return ConditionOutcome.match();
        }
        return ConditionOutcome.noMatch("Nobody has the clean-up function enabled");
    }
}
