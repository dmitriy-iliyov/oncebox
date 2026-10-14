package io.github.dmitriyiliyov.oncebox.starter.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxManager;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.*;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.NoopOutboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.OutboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.List;

@Configuration
@ConditionalOnProperty(
        prefix = "oncebox.outbox",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnClass({MeterRegistry.class, OutboxMetrics.class})
@ConditionalOnProperty(
        prefix = "oncebox.outbox.metrics",
        name = "enabled",
        havingValue = "true"
)
public class OutboxMetricsAutoConfiguration {

    private final OutboxProperties outboxProperties;

    public OutboxMetricsAutoConfiguration(OutboxProperties outboxProperties) {
        this.outboxProperties = outboxProperties;
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxCache")
    public OutboxCache<EventStatus> outboxCache() {
        OnceboxProperties.MetricsProperties metricsProperties = outboxProperties.getMetrics();
        if (metricsProperties != null) {
            OnceboxProperties.MetricsProperties.GaugeProperties gaugeProperties = metricsProperties.getGauge();
            if (gaugeProperties != null && Boolean.TRUE.equals(gaugeProperties.isEnabled())) {
                OnceboxProperties.MetricsProperties.GaugeProperties.CacheProperties cacheProperties = gaugeProperties.getCache();
                if (cacheProperties != null && !Boolean.FALSE.equals(cacheProperties.isEnabled())) {
                    List<Duration> ttls = cacheProperties.getTtls();
                    return new SimpleOutboxCache<>(
                            ttls.get(0).toMillis(), ttls.get(1).toMillis(), ttls.get(2).toMillis()
                    );
                }
            }
        }
        return new NoopOutboxCache<>();
    }

    @Bean
    @Primary
    public OutboxManager outboxManagerMetricsDecorator(@Qualifier("outboxManager") OutboxManager manager,
                                                       MeterRegistry registry) {
        return new OutboxManagerMetricsDecorator(outboxProperties, registry, manager);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "oncebox.outbox.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetricsRepository outboxMetricsRepository(
            @Qualifier("onceboxJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new MultiDialectOutboxMetricsRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "oncebox.outbox.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetricsService outboxMetricsService(OutboxMetricsRepository repository, OutboxCache<EventStatus> cache) {
        return new DefaultOutboxMetricsService(repository, cache);
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxMetrics")
    @ConditionalOnProperty(
            prefix = "oncebox.outbox.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetrics outboxMetrics(MeterRegistry registry, OutboxMetricsService metricsService) {
        return new DefaultOutboxMetrics(outboxProperties, registry, metricsService);
    }
}
