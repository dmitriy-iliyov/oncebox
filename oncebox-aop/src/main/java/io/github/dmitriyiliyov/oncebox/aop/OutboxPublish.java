package io.github.dmitriyiliyov.oncebox.aop;

import org.intellij.lang.annotations.Language;

import java.lang.annotation.*;

/**
 * Annotation to mark a method for automatic outbox event publishing.
 * <p>
 * When a method annotated with this is executed, the return value (or a derived value using SpEL)
 * is published as an outbox event.
 * <p>
 * When combined with {@code @Transactional} on the same method, event persistence relies on
 * {@link OutboxPublishAspect} running before the transaction commits, which in turn relies on the
 * transactional advisor keeping Spring's default (unconfigured) {@code LOWEST_PRECEDENCE} order.
 * If the application customizes {@code @EnableTransactionManagement(order = ...)} to a value with
 * higher precedence than the default, the aspect may end up running after commit and publishing
 * will fail with {@code IllegalTransactionStateException}. In that case, call
 * {@link io.github.dmitriyiliyov.oncebox.core.outbox.OutboxPublisher#publish} manually from
 * within the transactional method instead of relying on this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface OutboxPublish {

    String eventType();

    @Language("SpEL")
    String payload() default "#result";
}
