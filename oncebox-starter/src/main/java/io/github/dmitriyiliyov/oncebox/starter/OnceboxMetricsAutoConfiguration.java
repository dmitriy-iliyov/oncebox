package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.metrics.MetricsOnceboxScheduleStrategyListener;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.OutboxMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;

@Configuration
@ConditionalOnClass({MeterRegistry.class, MetricsOnceboxScheduleStrategyListener.class})
@ConditionalOnAnyMetricsEnabled
public class OnceboxMetricsAutoConfiguration {

    @Bean
    @Primary
    public OnceboxScheduleStrategyListenerSupplier metricsOnceboxScheduleStrategyListenerSupplier(MeterRegistry registry) {
        return new MetricsOnceboxScheduleStrategyListenerSupplier(registry);
    }

    @Bean
    @Primary
    public ContinuableTaskDecoratorSupplier metricsOnceboxContinuableTaskDecoratorSupplier(MeterRegistry registry) {
        return new ContinuableTaskTimeMeasureDecoratorSupplier(registry);
    }

    @Bean
    public PostApplicationReadyOnceboxInitializer metricsOnceboxInitializer(Map<String, OutboxMetrics> metrics) {
        return new MetricsPostApplicationReadyOnceboxInitializer(metrics);
    }
}
