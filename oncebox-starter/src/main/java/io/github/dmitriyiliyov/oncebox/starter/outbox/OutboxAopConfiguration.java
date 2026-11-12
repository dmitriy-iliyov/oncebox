package io.github.dmitriyiliyov.oncebox.starter.outbox;

import io.github.dmitriyiliyov.oncebox.aop.OutboxPublishAspect;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxPublisher;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(Aspect.class)
public class OutboxAopConfiguration {

    @Bean
    public OutboxPublishAspect outboxPublishAspect(OutboxPublisher publisher) {
        return new OutboxPublishAspect(publisher);
    }
}
