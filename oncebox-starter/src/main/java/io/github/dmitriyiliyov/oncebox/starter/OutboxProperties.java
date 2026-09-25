package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.OutboxPropertiesHolder;
import io.github.dmitriyiliyov.oncebox.core.polling.PollingPropertiesHolder;
import io.github.dmitriyiliyov.oncebox.starter.consumer.OutboxConsumerProperties;
import io.github.dmitriyiliyov.oncebox.starter.publisher.OutboxPublisherProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ConfigurationProperties(prefix = "oncebox")
public class OutboxProperties implements OutboxPropertiesHolder {

    private static final int DEFAULT_THREAD_POOL_SIZE = Math.min(Runtime.getRuntime().availableProcessors(), 5);

    private final UUID workerId = UUID.randomUUID();
    private Integer threadPoolSize;
    @NestedConfigurationProperty
    private OutboxPublisherProperties publisher;
    @NestedConfigurationProperty
    private OutboxConsumerProperties consumer;
    @NestedConfigurationProperty
    private OutboxProperties.TablesProperties tables;
    @NestedConfigurationProperty
    private DistributedLockProperties distributedLock;

    public OutboxProperties() {}

    @PostConstruct
    public void applyDefaults() {
        threadPoolSize = threadPoolSize == null ? DEFAULT_THREAD_POOL_SIZE : threadPoolSize;

        if (publisher == null) {
            publisher = new OutboxPublisherProperties();
            publisher.setEnabled(false);
        }
        publisher.applyDefaults();

        if (consumer == null) {
            consumer = new OutboxConsumerProperties();
            consumer.setEnabled(false);
        }
        consumer.applyDefaults();

        if (tables == null) {
            tables = new TablesProperties();
            tables.setAutoCreate(true);
        }
        tables.applyDefaults();

        if (distributedLock == null) {
            distributedLock = new DistributedLockProperties();
        }
        distributedLock.applyDefaults();
    }

    public UUID getWorkerId() {
        return workerId;
    }

    public Integer getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(Integer threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public OutboxPublisherProperties getPublisher() {
        return publisher;
    }

    public void setPublisher(OutboxPublisherProperties publisher) {
        this.publisher = publisher;
    }

    public OutboxConsumerProperties getConsumer() {
        return consumer;
    }

    public void setConsumer(OutboxConsumerProperties consumer) {
        this.consumer = consumer;
    }

    public TablesProperties getTables() {
        return tables;
    }

    public void setTables(TablesProperties tables) {
        this.tables = tables;
    }

    public DistributedLockProperties getDistributedLock() {
        return distributedLock;
    }

    public void setDistributedLock(DistributedLockProperties distributedLock) {
        this.distributedLock = distributedLock;
    }

    public String toStringWithPublisher() {
        return "OutboxProperties{" +
                "workerId=" + workerId +
                ", threadPoolSize=" + threadPoolSize +
                ", publisher=" + publisher +
                ", tables=" + tables +
                ", distributedLock=" + distributedLock +
                '}';
    }

    public String toStringWithConsumer() {
        return "OutboxProperties{" +
                "workerId=" + workerId +
                ", threadPoolSize=" + threadPoolSize +
                ", consumer=" + consumer +
                ", tables=" + tables +
                ", distributedLock=" + distributedLock +
                '}';
    }

    @Override
    public String toString() {
        return "OutboxProperties{" +
                "workerId=" + workerId +
                ", threadPoolSize=" + threadPoolSize +
                ", publisher=" + publisher +
                ", consumer=" + consumer +
                ", tables=" + tables +
                ", distributedLock=" + distributedLock +
                '}';
    }

    public static final class CleanUpProperties implements CleanUpPropertiesHolder {

        private static final int DEFAULT_BATCH_SIZE = 500;
        private static final Duration DEFAULT_TTL = Duration.ofHours(24);
        private static final PollingProperties.Defaults POLLING_DEFAULTS = PollingProperties.Defaults.ofAdaptive(
                PollingType.ADAPTIVE,
                Duration.ofMinutes(5),
                Duration.ofSeconds(5),
                Duration.ofMinutes(1),
                2.0
        );

        private Boolean enabled;
        private Integer batchSize;
        private Duration ttl;
        @NestedConfigurationProperty
        private PollingProperties polling;

        public void applyDefaults() {
            if (enabled == null || enabled) {
                enabled = true;
                batchSize = (batchSize == null || batchSize <= 0) ? DEFAULT_BATCH_SIZE : batchSize;
                ttl = ttl == null ? DEFAULT_TTL : ttl;
                polling = polling == null ? new PollingProperties() : polling;
                polling.applyDefaults(POLLING_DEFAULTS);
            } else {
                enabled = false;
                batchSize = 0;
                ttl = null;
                polling = new PollingProperties();
            }
        }

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public Integer getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
        }

        @Override
        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }

        public PollingProperties getPolling() {
            return polling;
        }

        public void setPolling(PollingProperties polling) {
            this.polling = polling;
        }

        @Override
        public Duration getInitialDelay() {
            return polling.getInitialDelay();
        }

        @Override
        public Duration getFixedDelay() {
            return polling.getFixedDelay();
        }

        @Override
        public Duration getMinFixedDelay() {
            return polling.getMinFixedDelay();
        }

        @Override
        public Duration getMaxFixedDelay() {
            return polling.getMaxFixedDelay();
        }

        @Override
        public Double getMultiplier() {
            return polling.getMultiplier();
        }

        @Override
        public String toString() {
            return "CleanUpProperties{" +
                    "enabled=" + enabled +
                    ", batchSize=" + batchSize +
                    ", ttl=" + ttl +
                    ", polling=" + polling +
                    '}';
        }
    }

    public static final class TablesProperties {

        private Boolean autoCreate;

        public void applyDefaults() {
            autoCreate = autoCreate == null || autoCreate;
        }

        public Boolean isAutoCreate() {
            return autoCreate;
        }

        public void setAutoCreate(Boolean autoCreate) {
            this.autoCreate = autoCreate;
        }

        @Override
        public String toString() {
            return "TablesProperties{" +
                    "autoCreate=" + autoCreate +
                    '}';
        }
    }

    public static final class MetricsProperties {

        private Boolean enabled;
        @NestedConfigurationProperty
        private GaugeProperties gauge;

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public GaugeProperties getGauge() {
            return gauge;
        }

        public void setGauge(GaugeProperties gauge) {
            this.gauge = gauge;
        }

        public void applyDefaults() {
            if (enabled != null && enabled) {
                enabled = true;
                if (gauge == null) {
                    gauge = new GaugeProperties();
                    gauge.setEnabled(false);
                }
            } else {
                enabled = false;
                gauge = new GaugeProperties();
                gauge.setEnabled(false);
            }
            gauge.applyDefaults();
        }

        @Override
        public String toString() {
            return "MetricsProperties{" +
                    "enabled=" + enabled +
                    ", gauge=" + gauge +
                    '}';
        }

        public static final class GaugeProperties {

            private Boolean enabled;
            @NestedConfigurationProperty
            private CacheProperties cache;

            public Boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(Boolean enabled) {
                this.enabled = enabled;
            }

            public CacheProperties getCache() {
                return cache;
            }

            public void setCache(CacheProperties cache) {
                this.cache = cache;
            }

            public void applyDefaults() {
                if (enabled != null && enabled) {
                    enabled = true;
                    if (cache == null) {
                        cache = new CacheProperties();
                        cache.setEnabled(true);
                    }
                } else {
                    enabled = false;
                    cache = new CacheProperties();
                    cache.setEnabled(false);
                }
                cache.applyDefaults();
            }

            @Override
            public String toString() {
                return "GaugeProperties{" +
                        "enabled=" + enabled +
                        ", cache=" + cache +
                        '}';
            }

            public static final class CacheProperties {

                private static final List<Duration> DEFAULT_TTLS = List.of(
                        Duration.ofSeconds(60), Duration.ofSeconds(60), Duration.ofSeconds(60)
                );

                private Boolean enabled;
                private List<Duration> ttls;

                public Boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(Boolean enabled) {
                    this.enabled = enabled;
                }

                public List<Duration> getTtls() {
                    return ttls;
                }

                public void setTtls(List<Duration> ttls) {
                    this.ttls = ttls;
                }

                public void applyDefaults() {
                    if (enabled == null || enabled) {
                        enabled = true;
                        if (ttls == null || ttls.isEmpty() || ttls.size() != DEFAULT_TTLS.size()) {
                            ttls = DEFAULT_TTLS;
                        }
                    } else {
                        enabled = false;
                        ttls = Collections.emptyList();
                    }
                }

                @Override
                public String toString() {
                    return "CacheProperties{" +
                            "enabled=" + enabled +
                            ", ttls=" + ttls +
                            '}';
                }
            }
        }
    }

    public static final class PollingProperties implements PollingPropertiesHolder {

        private PollingType type;
        private Duration initialDelay;
        private Duration fixedDelay;
        private Duration minFixedDelay;
        private Duration maxFixedDelay;
        private Double multiplier;

        public void applyDefaults(Defaults defaults) {
            type = type == null ? defaults.type() : type;
            initialDelay = initialDelay == null ? defaults.initialDelay() : initialDelay;
            switch (type) {
                case FIXED -> {
                    fixedDelay = fixedDelay == null ? defaults.fixedDelay() : fixedDelay;
                    minFixedDelay = Duration.ZERO;
                    maxFixedDelay = Duration.ZERO;
                    multiplier = Double.NaN;
                }
                case ADAPTIVE -> {
                    minFixedDelay = minFixedDelay == null ? defaults.minFixedDelay() : minFixedDelay;
                    maxFixedDelay = maxFixedDelay == null ? defaults.maxFixedDelay() : maxFixedDelay;
                    multiplier = multiplier == null ? defaults.multiplier() : multiplier;
                    fixedDelay = Duration.ZERO;
                }
            }
            validate();
        }

        public void validate() {
            switch (type) {
                case FIXED -> {
                    Objects.requireNonNull(initialDelay, "'initial-delay' cannot be null");
                    Objects.requireNonNull(fixedDelay, "'fixed-delay' cannot be null");
                }
                case ADAPTIVE -> {
                    Objects.requireNonNull(type, "type cannot be null");
                    Objects.requireNonNull(initialDelay, "'initial-delay' cannot be null");
                    Objects.requireNonNull(minFixedDelay, "'min-fixed-delay' cannot be null");
                    Objects.requireNonNull(maxFixedDelay, "'max-fixed-delay' cannot be null");
                    if (minFixedDelay.compareTo(maxFixedDelay) > 0) {
                        throw new IllegalArgumentException("'min-fixed-delay' cannot be greater than 'max-fixed-delay'");
                    }
                    Objects.requireNonNull(multiplier, "'multiplier' cannot be null");
                    if (multiplier <= 0) {
                        throw new IllegalArgumentException("'multiplier' cannot be negative or 0");
                    }
                }
            }
        }

        public PollingType getType() {
            return type;
        }

        public void setType(PollingType type) {
            this.type = type;
        }

        @Override
        public Duration getInitialDelay() {
            return initialDelay;
        }

        public void setInitialDelay(Duration initialDelay) {
            this.initialDelay = initialDelay;
        }

        @Override
        public Duration getFixedDelay() {
            return fixedDelay;
        }

        public void setFixedDelay(Duration fixedDelay) {
            this.fixedDelay = fixedDelay;
        }

        @Override
        public Duration getMinFixedDelay() {
            return minFixedDelay;
        }

        public void setMinFixedDelay(Duration minFixedDelay) {
            this.minFixedDelay = minFixedDelay;
        }

        @Override
        public Duration getMaxFixedDelay() {
            return maxFixedDelay;
        }

        public void setMaxFixedDelay(Duration maxFixedDelay) {
            this.maxFixedDelay = maxFixedDelay;
        }

        @Override
        public Double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(Double multiplier) {
            this.multiplier = multiplier;
        }

        @Override
        public String toString() {
            return "PollingProperties{" +
                    "type=" + type +
                    ", initialDelay=" + initialDelay +
                    ", fixedDelay=" + fixedDelay +
                    ", minFixedDelay=" + minFixedDelay +
                    ", maxFixedDelay=" + maxFixedDelay +
                    ", multiplier=" + multiplier +
                    '}';
        }

        public record Defaults(
                PollingType type,
                Duration initialDelay,
                Duration fixedDelay,
                Duration minFixedDelay,
                Duration maxFixedDelay,
                Double multiplier
        ) {

            public Defaults {
                validate(type, initialDelay, fixedDelay, minFixedDelay, maxFixedDelay, multiplier);
            }

            private static void validate(PollingType type, Duration initialDelay, Duration fixedDelay,
                                         Duration minFixedDelay, Duration maxFixedDelay, Double multiplier) {
                Objects.requireNonNull(type, "type cannot be null");
                switch (type) {
                    case FIXED -> validateForFixed(type, initialDelay, fixedDelay);
                    case ADAPTIVE -> validateForAdaptive(type, initialDelay, minFixedDelay, maxFixedDelay, multiplier);
                }
            }

            private static void validateForAdaptive(PollingType type, Duration initialDelay, Duration minFixedDelay,
                                                    Duration maxFixedDelay, Double multiplier) {
                Objects.requireNonNull(type, "type cannot be null");
                Objects.requireNonNull(initialDelay, "'initial-delay' cannot be null");
                Objects.requireNonNull(minFixedDelay, "'min-fixed-delay' cannot be null");
                Objects.requireNonNull(maxFixedDelay, "'max-fixed-delay' cannot be null");
                if (minFixedDelay.compareTo(maxFixedDelay) > 0) {
                    throw new IllegalArgumentException("'min-fixed-delay' cannot be greater than 'max-fixed-delay'");
                }
                Objects.requireNonNull(multiplier, "'multiplier' cannot be null");
                if (multiplier <= 0) {
                    throw new IllegalArgumentException("'multiplier' cannot be negative or 0");
                }
            }

            private static void validateForFixed(PollingType type, Duration initialDelay, Duration fixedDelay) {
                Objects.requireNonNull(type, "type cannot be null");
                Objects.requireNonNull(initialDelay, "'initial-delay' cannot be null");
                Objects.requireNonNull(fixedDelay, "'fixed-delay' cannot be null");
            }

            public static Defaults ofAdaptive(PollingType type, Duration initialDelay, Duration minFixedDelay,
                                              Duration maxFixedDelay, Double multiplier) {
                validateForAdaptive(type, initialDelay, minFixedDelay, maxFixedDelay, multiplier);
                return new Defaults(
                        type,
                        initialDelay,
                        Duration.ZERO,
                        minFixedDelay,
                        maxFixedDelay,
                        multiplier
                );
            }

            public static Defaults ofFixed(PollingType type, Duration initialDelay, Duration fixedDelay) {
                validateForFixed(type, initialDelay, fixedDelay);
                return new Defaults(
                        type,
                        initialDelay,
                        fixedDelay,
                        Duration.ZERO,
                        Duration.ZERO,
                        Double.NaN
                );
            }

            public static Defaults ofPollingProperties(PollingProperties polling) {
                validate(polling.getType(), polling.getInitialDelay(), polling.getFixedDelay(),
                        polling.getMinFixedDelay(), polling.getMaxFixedDelay(), polling.getMultiplier());
                return new Defaults(
                        polling.getType(),
                        polling.getInitialDelay(),
                        polling.getFixedDelay(),
                        polling.getMinFixedDelay(),
                        polling.getMaxFixedDelay(),
                        polling.getMultiplier()
                );
            }
        }
    }

    public static final class DistributedLockProperties {

        private Duration lockAtLeastFor = Duration.ofSeconds(1);
        private Duration lockAtMostFor = Duration.ofMinutes(1);
        private Boolean resolveByPollingProperties = true;

        public void applyDefaults() {
            Objects.requireNonNull(lockAtLeastFor, "'lock-at-least-for' cannot be null");
            Objects.requireNonNull(lockAtMostFor, "'lock-at-most-for' cannot be null");
            if (resolveByPollingProperties) {
                lockAtLeastFor = Duration.ZERO;
                lockAtMostFor = Duration.ZERO;
            }
        }

        public Duration getLockAtLeastFor() {
            return lockAtLeastFor;
        }

        public void setLockAtLeastFor(Duration lockAtLeastFor) {
            this.lockAtLeastFor = lockAtLeastFor;
        }

        public Duration getLockAtMostFor() {
            return lockAtMostFor;
        }

        public void setLockAtMostFor(Duration lockAtMostFor) {
            this.lockAtMostFor = lockAtMostFor;
        }

        public Boolean isResolveByPollingProperties() {
            return resolveByPollingProperties;
        }

        public void setResolveByPollingProperties(Boolean resolveByPollingProperties) {
            this.resolveByPollingProperties = resolveByPollingProperties;
        }

        @Override
        public String toString() {
            return "DistributedLockProperties{" +
                    "lockAtLeastFor=" + lockAtLeastFor +
                    ", lockAtMostFor=" + lockAtMostFor +
                    ", resolveByPollingProperties=" + resolveByPollingProperties +
                    '}';
        }
    }
}
