package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.starter.OnceboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.TransportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Map;
import java.util.Objects;

public class InboxProperties {

    private static final Logger log = LoggerFactory.getLogger(InboxProperties.class);

    private Boolean enabled;
    private SourceProperties source;
    private Map<String, Class<?>> mappings;
    @NestedConfigurationProperty
    private OnceboxProperties.CleanUpProperties cleanUp;
    @NestedConfigurationProperty
    private CacheProperties cache;
    @NestedConfigurationProperty
    private OnceboxProperties.MetricsProperties metrics;

    public void applyDefaults() {
        if (enabled != null && enabled) {

            source.applyDefaults();

            if (mappings == null || mappings.isEmpty()) {
                log.warn("Inbox is configured with empty 'mappings'");
            }

            if (cleanUp == null) {
                cleanUp = new OnceboxProperties.CleanUpProperties();
                cleanUp.setEnabled(true);
            }
            cleanUp.applyDefaults();
            if (!cleanUp.isEnabled()) {
                log.warn("Inbox is configured with 'clean-up.enabled=false', inbox storage will not be cleaned automatically");
            }

            if (cache == null) {
                cache = new CacheProperties();
                cache.setEnabled(false);
            }
            cache.applyDefaults();
            if (!cache.isEnabled()) {
                log.warn("Inbox is configured with 'cache.enabled=false'");
            }

            if (metrics == null) {
                metrics = new OnceboxProperties.MetricsProperties();
                metrics.setEnabled(false);
            }
            metrics.applyDefaults();

            log.debug("InboxProperties successfully initialized");
        } else {
            enabled = false;

            source = new SourceProperties();

            cleanUp = new OnceboxProperties.CleanUpProperties();
            cleanUp.setEnabled(false);
            cleanUp.applyDefaults();

            cache = new CacheProperties();
            cache.setEnabled(false);
            cache.applyDefaults();

            metrics = new OnceboxProperties.MetricsProperties();
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

    public OnceboxProperties.CleanUpProperties getCleanUp() {
        return cleanUp;
    }

    public void setCleanUp(OnceboxProperties.CleanUpProperties cleanUp) {
        this.cleanUp = cleanUp;
    }

    public CacheProperties getCache() {
        return cache;
    }

    public void setCache(CacheProperties cache) {
        this.cache = cache;
    }

    public OnceboxProperties.MetricsProperties getMetrics() {
        return metrics;
    }

    public void setMetrics(OnceboxProperties.MetricsProperties metrics) {
        this.metrics = metrics;
    }

    @Override
    public String toString() {
        return "InboxProperties{" +
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
