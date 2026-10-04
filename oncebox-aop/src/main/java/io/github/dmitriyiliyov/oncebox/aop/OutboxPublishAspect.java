package io.github.dmitriyiliyov.oncebox.aop;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxPublisher;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.List;
import java.util.Objects;

/**
 * Aspect that intercepts methods annotated with {@link OutboxPublish} and publishes an outbox event.
 * <p>
 * This aspect extracts the event payload from the method's return value or arguments using SpEL.
 * <p>
 * See {@link OutboxPublish} for the transactional-ordering caveat when this aspect is combined
 * with {@code @Transactional} on the same method.
 * */
@Aspect
public class OutboxPublishAspect {

    private final ExpressionParser expressionParser;
    private final OutboxPublisher publisher;

    public OutboxPublishAspect(OutboxPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher cannot be null");
        this.expressionParser = new SpelExpressionParser();
    }

    @Pointcut("@annotation(outboxPublish) && execution(public * *(..))")
    public void pointcut(OutboxPublish outboxPublish) {}

    @AfterReturning(
            pointcut = "pointcut(outboxPublish)",
            returning = "result",
            argNames = "joinPoint,outboxPublish,result"
    )
    public void advice(JoinPoint joinPoint, OutboxPublish outboxPublish, Object result) {
        Object payload = result;
        String spelPayload = outboxPublish.payload();
        if (spelPayload != null && !spelPayload.isBlank() && !spelPayload.equals("#result")) {
            StandardEvaluationContext context = getContext(joinPoint);
            context.setVariable("result", result);
            payload = expressionParser.parseExpression(spelPayload).getValue(context);
        }
        Objects.requireNonNull(payload, "payload cannot be null");
        if (payload instanceof List<?>) {
            publisher.publish(outboxPublish.eventType(), (List<?>) payload);
            return;
        }
        publisher.publish(outboxPublish.eventType(), payload);
    }

    StandardEvaluationContext getContext(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < args.length; i++) {
            if (paramNames != null && i < paramNames.length) {
                context.setVariable(paramNames[i], args[i]);
            } else {
                context.setVariable("args[" + i + "]", args[i]);
            }
        }
        return context;
    }
}
