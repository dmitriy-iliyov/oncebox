package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.core.inbox.InboxManager;
import io.github.dmitriyiliyov.oncebox.metrics.inbox.InboxManagerMetricsDecorator;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

@Configuration
@ConditionalOnProperty(
        prefix = "oncebox.inbox",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnClass({MeterRegistry.class, InboxManagerMetricsDecorator.class})
@ConditionalOnProperty(
        prefix = "oncebox.inbox.metrics",
        name = "enabled",
        havingValue = "true"
)
public class InboxMetricsAutoConfiguration {

    @Bean
    @Primary
    public InboxManager inboxManagerMetricsDecorator(
            @Qualifier("inboxManager") InboxManager inboxManager,
            MeterRegistry registry
    ) {
        return new InboxManagerMetricsDecorator(registry, inboxManager);
    }

    @Bean
    @Order(2)
    public IdempotentConsumerDecoratorSupplier inboxIdempotentConsumerMetricsDecoratorSupplier(MeterRegistry registry) {
        return new IdempotentConsumerMetricsDecoratorSupplier(registry);
    }
}
