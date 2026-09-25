package io.github.dmitriyiliyov.oncebox.starter.consumer;

import io.github.dmitriyiliyov.oncebox.starter.OutboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.TransportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Map;
import java.util.Objects;

public class OutboxConsumerProperties {

    private static final Logger log = LoggerFactory.getLogger(OutboxConsumerProperties.class);

    private Boolean enabled;
    private SourceProperties source;
    private Map<String, Class<?>> mappings;
    @NestedConfigurationProperty
    private OutboxProperties.CleanUpProperties cleanUp;
    @NestedConfigurationProperty
    private CacheProperties cache;
    @NestedConfigurationProperty
    private OutboxProperties.MetricsProperties metrics;

    public void applyDefaults() {
        if (enabled != null && enabled) {

            source.applyDefaults();

            if (mappings == null || mappings.isEmpty()) {
                log.warn("Outbox Consumer is configured with empty 'mappings'");
            }

            if (cleanUp == null) {
                cleanUp = new OutboxProperties.CleanUpProperties();
                cleanUp.setEnabled(true);
            }
            cleanUp.applyDefaults();
            if (!cleanUp.isEnabled()) {
                log.warn("Outbox Consumer is configured with 'clean-up.enabled=false', consumed outbox storage will not be cleaned automatically");
            }

            if (cache == null) {
                cache = new CacheProperties();
                cache.setEnabled(false);
            }
            cache.applyDefaults();
            if (!cache.isEnabled()) {
                log.warn("Outbox Consumer is configured with 'cache.enabled=false'");
            }

            if (metrics == null) {
                metrics = new OutboxProperties.MetricsProperties();
                metrics.setEnabled(false);
            }
            metrics.applyDefaults();

            log.debug("OutboxConsumerProperties successfully initialized");
        } else {
            enabled = false;

            source = new SourceProperties();

            cleanUp = new OutboxProperties.CleanUpProperties();
            cleanUp.setEnabled(false);
            cleanUp.applyDefaults();

            cache = new CacheProperties();
            cache.setEnabled(false);
            cache.applyDefaults();

            metrics = new OutboxProperties.MetricsProperties();
            metrics.setEnabled(false);
            metrics.applyDefaults();
        }
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public SourceProperties getSource() {
        return source;
    }

    public void setSource(SourceProperties source) {
        this.source = source;
    }

    public Map<String, Class<?>> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, Class<?>> mappings) {
        this.mappings = mappings;
    }

    public OutboxProperties.CleanUpProperties getCleanUp() {
        return cleanUp;
    }

    public void setCleanUp(OutboxProperties.CleanUpProperties cleanUp) {
        this.cleanUp = cleanUp;
    }

    public CacheProperties getCache() {
        return cache;
    }

    public void setCache(CacheProperties cache) {
        this.cache = cache;
    }

    public OutboxProperties.MetricsProperties getMetrics() {
        return metrics;
    }

    public void setMetrics(OutboxProperties.MetricsProperties metrics) {
        this.metrics = metrics;
    }

    @Override
    public String toString() {
        return "OutboxConsumerProperties{" +
                "enabled=" + enabled +
                ", source=" + source +
                ", mappings=" + mappings +
                ", cleanUp=" + cleanUp +
                ", cache=" + cache +
                ", metrics=" + metrics +
                '}';
    }

    public static final class SourceProperties {

        private TransportType type;

        public void applyDefaults() {
            if (type == null) {
                throw new IllegalArgumentException("'source.type' cannot be null");
            }
        }

        public TransportType getType() {
            return type;
        }

        public void setType(TransportType type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "SourceProperties{" +
                    "type='" + type + '\'' +
                    '}';
        }
    }

    public static final class CacheProperties {

        public Boolean enabled;
        public String cacheName;

        public void applyDefaults() {
            if (enabled == null || enabled) {
                enabled = true;
                Objects.requireNonNull(cacheName, "'cache-name' cannot be null");
                if (cacheName.isBlank()) {
                    throw new IllegalArgumentException("'cache-name' cannot be empty or blank");
                }
            } else {
                enabled = false;
                cacheName = null;
            }
        }

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getCacheName() {
            return cacheName;
        }

        public void setCacheName(String cacheName) {
            this.cacheName = cacheName;
        }

        @Override
        public String toString() {
            return "CacheProperties{" +
                    "enabled=" + enabled +
                    ", cacheName='" + cacheName + '\'' +
                    '}';
        }
    }
}
