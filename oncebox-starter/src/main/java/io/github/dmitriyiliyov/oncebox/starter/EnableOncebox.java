package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.starter.inbox.InboxAutoConfiguration;
import io.github.dmitriyiliyov.oncebox.starter.outbox.OutboxAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({
        OnceboxAutoConfiguration.class,
        OutboxAutoConfiguration.class,
        InboxAutoConfiguration.class
})
public @interface EnableOncebox { }