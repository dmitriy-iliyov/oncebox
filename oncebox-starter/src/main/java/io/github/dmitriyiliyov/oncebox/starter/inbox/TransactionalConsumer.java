package io.github.dmitriyiliyov.oncebox.starter.inbox;

import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier("inboxTransactionalIdempotentConsumer")
@Documented
public @interface TransactionalConsumer { }
