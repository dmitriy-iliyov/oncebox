package io.github.dmitriyiliyov.oncebox.starter.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqManager;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.OutboxMetrics;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq.*;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.NoopOutboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.OutboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.utils.SimpleOutboxCache;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.outbox.OutboxProperties;
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
        prefix = "oncebox.outbox.dlq",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnClass({MeterRegistry.class, OutboxMetrics.class})
@ConditionalOnProperty(
        prefix = "oncebox.outbox.metrics",
        name = "enabled",
        havingValue = "true"
)
public class OutboxDlqMetricsAutoConfiguration {

    private final OutboxProperties outboxProperties;

    public OutboxDlqMetricsAutoConfiguration(OutboxProperties outboxProperties) {
        this.outboxProperties = outboxProperties;
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxDlqCache")
    public OutboxCache<DlqStatus> outboxDlqCache() {
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
    public OutboxDlqManager outboxDlqManagerMetricsDecorator(@Qualifier("outboxDlqManager") OutboxDlqManager manager,
                                                             MeterRegistry registry) {
        return new OutboxDlqManagerMetricsDecorator(registry, manager);

    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "oncebox.outbox.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxDlqMetricsRepository outboxDlqMetricsRepository(
            @Qualifier("onceboxJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new MultiDialectOutboxDlqMetricsRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "oncebox.outbox.metrics.gauge",
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
            prefix = "oncebox.outbox.metrics.gauge",
            name = "enabled",
            havingValue = "true"
    )
    public OutboxMetrics outboxDlqMetrics(OutboxProperties properties,
                                          MeterRegistry registry,
                                          OutboxDlqMetricsService metricsService) {
        return new OutboxDlqMetrics(properties, registry, metricsService);
    }
}
