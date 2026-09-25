package io.github.dmitriyiliyov.oncebox.starter.publisher;

import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxManager;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.oncebox.metrics.publisher.*;
import io.github.dmitriyiliyov.oncebox.metrics.publisher.utils.NoopOutboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.publisher.utils.OutboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.publisher.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.oncebox.starter.OutboxProperties;
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
        prefix = "oncebox.publisher",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnClass({MeterRegistry.class, OutboxMetrics.class})
@ConditionalOnProperty(
        prefix = "oncebox.publisher.metrics",
        name = "enabled",
        havingValue = "true"
)
public class OutboxPublisherMetricsAutoConfiguration {

    private final OutboxPublisherProperties publisherProperties;

    public OutboxPublisherMetricsAutoConfiguration(OutboxPublisherProperties publisherProperties) {
        this.publisherProperties = publisherProperties;
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxCache")
    public OutboxCache<EventStatus> outboxCache() {
        OutboxProperties.MetricsProperties metricsProperties = publisherProperties.getMetrics();
        if (metricsProperties != null) {
            OutboxProperties.MetricsProperties.GaugeProperties gaugeProperties = metricsProperties.getGauge();
            if (gaugeProperties != null && Boolean.TRUE.equals(gaugeProperties.isEnabled())) {
                OutboxProperties.MetricsProperties.GaugeProperties.CacheProperties cacheProperties = gaugeProperties.getCache();
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
        return new OutboxManagerMetricsDecorator(publisherProperties, registry, manager);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "oncebox.publisher.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetricsRepository outboxMetricsRepository(
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new MultiDialectOutboxMetricsRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "oncebox.publisher.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetricsService outboxMetricsService(OutboxMetricsRepository repository, OutboxCache<EventStatus> cache) {
        return new DefaultOutboxMetricsService(repository, cache);
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxMetrics")
    @ConditionalOnProperty(
            prefix = "oncebox.publisher.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetrics outboxMetrics(MeterRegistry registry, OutboxMetricsService metricsService) {
        return new DefaultOutboxMetrics(publisherProperties, registry, metricsService);
    }
}
