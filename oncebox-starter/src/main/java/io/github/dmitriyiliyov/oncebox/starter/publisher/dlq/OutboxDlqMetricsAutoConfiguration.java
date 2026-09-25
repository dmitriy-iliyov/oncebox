package io.github.dmitriyiliyov.oncebox.starter.publisher.dlq;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.OutboxDlqManager;
import io.github.dmitriyiliyov.oncebox.metrics.OutboxMetrics;
import io.github.dmitriyiliyov.oncebox.metrics.publisher.dlq.*;
import io.github.dmitriyiliyov.oncebox.metrics.publisher.utils.NoopOutboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.publisher.utils.OutboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.publisher.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.oncebox.starter.OutboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.publisher.OutboxPublisherProperties;
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
        prefix = "oncebox.publisher.dlq",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnClass({MeterRegistry.class, OutboxMetrics.class})
@ConditionalOnProperty(
        prefix = "oncebox.publisher.metrics",
        name = "enabled",
        havingValue = "true"
)
public class OutboxDlqMetricsAutoConfiguration {

    private final OutboxPublisherProperties publisherProperties;

    public OutboxDlqMetricsAutoConfiguration(OutboxPublisherProperties publisherProperties) {
        this.publisherProperties = publisherProperties;
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxDlqCache")
    public OutboxCache<DlqStatus> outboxDlqCache() {
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
    public OutboxDlqManager outboxDlqManagerMetricsDecorator(@Qualifier("outboxDlqManager") OutboxDlqManager manager,
                                                             MeterRegistry registry) {
        return new OutboxDlqManagerMetricsDecorator(registry, manager);

    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "oncebox.publisher.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxDlqMetricsRepository outboxDlqMetricsRepository(
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new MultiDialectOutboxDlqMetricsRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "oncebox.publisher.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxDlqMetricsService outboxDlqMetricsService(OutboxDlqMetricsRepository repository,
                                                           OutboxCache<DlqStatus> cache) {
        return new DefaultOutboxDlqMetricsService(repository, cache);
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxDlqMetrics")
    @ConditionalOnProperty(
            prefix = "oncebox.publisher.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetrics outboxDlqMetrics(OutboxPublisherProperties properties,
                                          MeterRegistry registry,
                                          OutboxDlqMetricsService metricsService) {
        return new OutboxDlqMetrics(properties, registry, metricsService);
    }
}
