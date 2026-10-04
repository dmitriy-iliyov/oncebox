package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.core.inbox.cache.InboxCacheListener;
import io.github.dmitriyiliyov.oncebox.inbox.cache.DefaultInboxCache;
import io.github.dmitriyiliyov.oncebox.metrics.inbox.MetricsInboxCacheListener;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty(
        prefix = "oncebox.inbox",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnClass({DefaultInboxCache.class, MetricsInboxCacheListener.class, MeterRegistry.class})
@ConditionalOnProperty(
        prefix = "oncebox.inbox.cache",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnProperty(
        prefix = "oncebox.inbox.metrics",
        name = "enabled",
        havingValue = "true"
)
public class InboxCacheMetricsAutoConfiguration {

    @Bean
    @Primary
    public InboxCacheListener metricsInboxCacheListener(MeterRegistry registry) {
        return new MetricsInboxCacheListener(registry);
    }
}
