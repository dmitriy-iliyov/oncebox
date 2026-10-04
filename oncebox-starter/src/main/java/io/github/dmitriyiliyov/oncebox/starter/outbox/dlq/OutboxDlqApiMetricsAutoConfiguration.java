package io.github.dmitriyiliyov.oncebox.starter.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.dlq.api.OutboxDlqApiService;
import io.github.dmitriyiliyov.oncebox.dlq.api.OutboxDlqController;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.OutboxMetrics;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq.OutboxDlqApiServiceMetricsDecorator;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty(
        prefix = "oncebox.outbox.dlq",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnClass({OutboxDlqController.class, MeterRegistry.class, OutboxMetrics.class})
@ConditionalOnProperty(
        prefix = "oncebox.outbox.metrics",
        name = "enabled",
        havingValue = "true"
)
public class OutboxDlqApiMetricsAutoConfiguration {

    @Bean
    @Primary
    public OutboxDlqApiService outboxDlqApiServiceMetricsDecorator(@Qualifier("outboxDlqApiService") OutboxDlqApiService service,
                                                                   MeterRegistry registry) {
        return new OutboxDlqApiServiceMetricsDecorator(registry, service);
    }
}
