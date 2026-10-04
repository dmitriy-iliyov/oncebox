package io.github.dmitriyiliyov.oncebox.starter;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnAnyMetricsEnabledCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();
        boolean isOutboxMetricsEnabled = environment.getProperty("oncebox.outbox.metrics.enabled", Boolean.class, false);
        boolean isInboxMetricsEnabled = environment.getProperty("oncebox.inbox.metrics.enabled", Boolean.class, false);
        if (isOutboxMetricsEnabled || isInboxMetricsEnabled) {
            return ConditionOutcome.match("At least somebody enabled metrics");
        }
        return ConditionOutcome.noMatch("Nobody has the metrics enabled");
    }
}
