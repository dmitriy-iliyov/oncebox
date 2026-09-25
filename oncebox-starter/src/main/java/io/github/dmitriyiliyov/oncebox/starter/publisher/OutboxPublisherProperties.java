package io.github.dmitriyiliyov.oncebox.starter.publisher;

import io.github.dmitriyiliyov.oncebox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.oncebox.starter.OutboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.PollingType;
import io.github.dmitriyiliyov.oncebox.starter.TransportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OutboxPublisherProperties implements OutboxPublisherPropertiesHolder {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherProperties.class);

    private Boolean enabled;
    @NestedConfigurationProperty
    private SenderProperties sender;
    @NestedConfigurationProperty
    private EventProperties.Defaults defaults;
    private Map<String, EventProperties> events;
    @NestedConfigurationProperty
    private StuckRecoveryProperties stuckRecovery;
    @NestedConfigurationProperty
    private OutboxProperties.CleanUpProperties cleanUp;
    @NestedConfigurationProperty
    private DlqProperties dlq;
    @NestedConfigurationProperty
    private OutboxProperties.MetricsProperties metrics;

    public OutboxPublisherProperties() {}

    public void applyDefaults() {
        if (enabled == null || enabled) {
            enabled = true;

            if (sender == null) {
                throw new IllegalArgumentException("'sender' cannot be null");
            }
            sender.applyDefaults();

            defaults = defaults == null ? new EventProperties.Defaults() : defaults;
            defaults.applyDefaults();

            if (events == null) {
                throw new IllegalArgumentException("'events' cannot be null");
            }
            if (events.isEmpty()) {
                log.warn("Outbox is configured with empty 'events'");
            }
            events = applyDefaults(events);

            stuckRecovery = stuckRecovery == null ? new StuckRecoveryProperties() : stuckRecovery;
            stuckRecovery.applyDefaults();

            if (sender.getEmergencyTimeout().compareTo(stuckRecovery.getMaxBatchProcessingTime()) >= 0) {
                throw new IllegalStateException("""
                                'sender.emergency-timeout' (%s) must be less than 'stuck-recovery.max-batch-processing-time' (%s), \
                                otherwise a batch still being sent is recovered as stuck and published twice"""
                        .formatted(sender.getEmergencyTimeout(), stuckRecovery.getMaxBatchProcessingTime())
                );
            }

            if (cleanUp == null) {
                cleanUp = new OutboxProperties.CleanUpProperties();
                cleanUp.setEnabled(true);
            }
            cleanUp.applyDefaults();
            if (!cleanUp.isEnabled()) {
                log.warn("Outbox Publisher is configured with 'clean-up.enabled=false', processed outbox storage will not be cleaned automatically");
            }

            if (dlq == null) {
                dlq = new DlqProperties();
                dlq.setEnabled(false);
            }
            dlq.applyDefaults();
            if (!dlq.isEnabled()) {
                log.warn("Outbox is configured with 'dlq.enabled=false', failed outbox events will not be managed automatically");
            }

            if (metrics == null) {
                metrics = new OutboxProperties.MetricsProperties();
                metrics.setEnabled(false);
            }
            metrics.applyDefaults();
            log.debug("OutboxPublisherProperties successfully initialized");
        } else {
            enabled = false;

            defaults = new EventProperties.Defaults();
            defaults.applyDefaults();

            events = Collections.emptyMap();

            cleanUp = new OutboxProperties.CleanUpProperties();
            cleanUp.setEnabled(false);
            cleanUp.applyDefaults();

            dlq = new DlqProperties();
            dlq.setEnabled(false);
            dlq.applyDefaults();

            metrics = new OutboxProperties.MetricsProperties();
            metrics.setEnabled(false);
            metrics.applyDefaults();
        }
    }

    private Map<String, EventProperties> applyDefaults(Map<String, EventProperties> eventPropertiesMap) {
        return eventPropertiesMap.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        e -> {
                            String eventType = e.getKey();
                            if (eventType == null) {
                                throw new IllegalArgumentException("'events' key cannot be null");
                            }
                            if (eventType.isBlank()) {
                                throw new IllegalArgumentException("'events' key cannot be blank");
                            }
                            return eventType;
                        },
                        e -> {
                            EventProperties event = e.getValue();
                            event.setEventType(e.getKey());
                            event.applyDefaults(defaults);
                            return event;
                        }
                ));
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public SenderProperties getSender() {
        return sender;
    }

    public void setSender(SenderProperties sender) {
        this.sender = sender;
    }

    public OutboxPublisherProperties.EventProperties.Defaults getDefaults() {
        return this.defaults;
    }

    public void setDefaults(EventProperties.Defaults defaults) {
        this.defaults = defaults;
    }

    @Override
    public boolean existEventType(String eventType) {
        return events.containsKey(eventType);
    }

    @Override
    public Map<String, EventPropertiesHolder> getEventHolders() {
        return events.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (EventPropertiesHolder) e.getValue()));
    }

    public Map<String, EventProperties> getEvents() {
        return events;
    }

    public void setEvents(Map<String, EventProperties> events) {
        this.events = events;
    }

    public StuckRecoveryProperties getStuckRecovery() {
        return stuckRecovery;
    }

    public void setStuckRecovery(StuckRecoveryProperties stuckRecovery) {
        this.stuckRecovery = stuckRecovery;
    }

    public boolean isCleanUpEnabled() {
        return cleanUp.isEnabled();
    }

    public OutboxProperties.CleanUpProperties getCleanUp() {
        return cleanUp;
    }

    public void setCleanUp(OutboxProperties.CleanUpProperties cleanUp) {
        this.cleanUp = cleanUp;
    }

    public DlqProperties getDlq() {
        return dlq;
    }

    public void setDlq(DlqProperties dlq) {
        this.dlq = dlq;
    }

    public OutboxProperties.MetricsProperties getMetrics() {
        return metrics;
    }

    public void setMetrics(OutboxProperties.MetricsProperties metrics) {
        this.metrics = metrics;
    }

    @Override
    public String toString() {
        return "OutboxPublisherProperties{" +
                "enabled=" + enabled +
                ", sender=" + sender +
                ", defaults=" + defaults +
                ", events=" + events +
                ", stuckRecovery=" + stuckRecovery +
                ", cleanUp=" + cleanUp +
                ", dlq=" + dlq +
                ", metrics=" + metrics +
                '}';
    }

    public static final class SenderProperties {

        private static final Duration DEFAULT_EMERGENCY_TIMEOUT = Duration.ofSeconds(120);

        private TransportType type;
        private String beanName;
        private Duration emergencyTimeout;

        public void applyDefaults() {
            if (type == null) {
                throw new IllegalArgumentException("'sender.type' cannot be null");
            }
            emergencyTimeout = emergencyTimeout == null ? DEFAULT_EMERGENCY_TIMEOUT : emergencyTimeout;
        }

        public TransportType getType() {
            return type;
        }

        public void setType(TransportType type) {
            this.type = type;
        }

        public String getBeanName() {
            return beanName;
        }

        public void setBeanName(String beanName) {
            this.beanName = beanName;
        }

        public Duration getEmergencyTimeout() {
            return emergencyTimeout;
        }

        public void setEmergencyTimeout(Duration emergencyTimeout) {
            this.emergencyTimeout = emergencyTimeout;
        }

        @Override
        public String toString() {
            return "SenderProperties{" +
                    "type=" + type +
                    ", beanName='" + beanName + '\'' +
                    ", emergencyTimeout=" + emergencyTimeout +
                    '}';
        }
    }

    public static final class BackoffProperties {

        private Boolean enabled;
        private Duration delay;
        private Double multiplier;

        public BackoffProperties() {
            this.enabled = true;
            this.delay = Duration.ZERO;
            this.multiplier = Double.NaN;
        }

        public BackoffProperties(Boolean enabled, Duration delay, Double multiplier) {
            this.enabled = enabled;
            this.delay = delay;
            this.multiplier = multiplier;
        }

        public void applyDefaults(Defaults defaults) {
            if (enabled == null || enabled) {
                enabled = true;
                delay = delay == null || delay.isZero() ? defaults.delay() : delay;
                multiplier = multiplier == null || multiplier < 1 || Double.isNaN(multiplier) ? defaults.multiplier() : multiplier;
            } else {
                enabled = false;
                delay = Duration.ofSeconds(0);
                multiplier = 1.0;
            }
        }

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getDelay() {
            return delay;
        }

        public void setDelay(Duration delay) {
            this.delay = delay;
        }

        public Double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(Double multiplier) {
            this.multiplier = multiplier;
        }

        @Override
        public String toString() {
            return "BackoffProperties{" +
                    "enabled=" + enabled +
                    ", delay=" + delay +
                    ", multiplier=" + multiplier +
                    '}';
        }

        public record Defaults(
                Duration delay,
                Double multiplier
        ) {

            public static Defaults ofBackoffProperties(BackoffProperties backoff) {
                return new Defaults(backoff.getDelay(), backoff.getMultiplier());
            }
        }
    }

    public static final class EventProperties implements EventPropertiesHolder {

        private String eventType;
        private String topic;
        private Integer batchSize;
        @NestedConfigurationProperty
        private OutboxProperties.PollingProperties polling;
        private Integer maxRetries;
        @NestedConfigurationProperty
        private BackoffProperties backoff;

        public void applyDefaults(Defaults defaults) {
            if (eventType == null) {
                throw new IllegalArgumentException("'events' key cannot be null");
            }
            if (eventType.isBlank()) {
                throw new IllegalArgumentException("'events' key cannot be blank");
            }
            if (topic == null) {
                throw new IllegalArgumentException("'topic' cannot be null");
            }
            if (topic.isBlank()) {
                throw new IllegalArgumentException("'topic' cannot be blank");
            }
            batchSize = batchSize == null ? defaults.getBatchSize() : batchSize;
            polling = polling == null ? defaults.getPolling() : polling;
            polling.applyDefaults(defaults.getPoolingDefaults());
            maxRetries = maxRetries == null ? defaults.getMaxRetries() : maxRetries;
            if (backoff == null) {
                backoff = defaults.getBackoff();
            } else if (!backoff.isEnabled()) {
                backoff = new BackoffProperties();
                backoff.setEnabled(false);
            }
            backoff.applyDefaults(defaults.getBackoffDefaults());
        }

        @Override
        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }

        @Override
        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        @Override
        public Integer getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
        }

        public OutboxProperties.PollingProperties getPolling() {
            return polling;
        }

        public void setPolling(OutboxProperties.PollingProperties polling) {
            this.polling = polling;
        }

        @Override
        public Integer getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
        }

        public BackoffProperties getBackoff() {
            return backoff;
        }

        public void setBackoff(BackoffProperties backoff) {
            this.backoff = backoff;
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
        public Double backoffMultiplier() {
            return backoff.getMultiplier();
        }

        @Override
        public Long backoffDelay() {
            return backoff.getDelay().toSeconds();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EventProperties that)) return false;
            return Objects.equals(eventType, that.eventType)
                    && Objects.equals(topic, that.topic)
                    && Objects.equals(batchSize, that.batchSize)
                    && Objects.equals(polling, that.polling)
                    && Objects.equals(maxRetries, that.maxRetries)
                    && Objects.equals(backoff, that.backoff);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventType, topic, batchSize, polling, maxRetries, backoff);
        }

        @Override
        public String toString() {
            return "EventProperties{" +
                    "eventType='" + eventType + '\'' +
                    ", topic='" + topic + '\'' +
                    ", batchSize=" + batchSize +
                    ", polling=" + polling +
                    ", maxRetries=" + maxRetries +
                    ", backoff=" + backoff +
                    '}';
        }

        public static final class Defaults {

            private static final int DEFAULT_BATCH_SIZE = 200;
            private static final OutboxProperties.PollingProperties.Defaults POOLING_DEFAULTS = OutboxProperties.PollingProperties.Defaults.ofAdaptive(
                    PollingType.ADAPTIVE,
                    Duration.ofMinutes(5),
                    Duration.ofMillis(250),
                    Duration.ofMinutes(1),
                    1.5
            );
            private static final int DEFAULT_MAX_RETRY = 3;
            private static final BackoffProperties.Defaults BACKOFF_DEFAULTS = new BackoffProperties.Defaults(
                    Duration.ofSeconds(10), 3.0
            );

            private Integer batchSize;
            @NestedConfigurationProperty
            private OutboxProperties.PollingProperties polling;
            private Integer maxRetries;
            @NestedConfigurationProperty
            private BackoffProperties backoff;

            public Defaults() {
                batchSize = DEFAULT_BATCH_SIZE;
                polling = new OutboxProperties.PollingProperties();
                maxRetries = DEFAULT_MAX_RETRY;
                backoff = new BackoffProperties();
            }

            public void applyDefaults() {
                batchSize = batchSize == null || batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
                polling = polling == null ? new OutboxProperties.PollingProperties() : polling;
                polling.applyDefaults(POOLING_DEFAULTS);
                maxRetries = maxRetries == null || maxRetries < 0 ? DEFAULT_MAX_RETRY : maxRetries;
                backoff = backoff == null ? new BackoffProperties() : backoff;
                backoff.applyDefaults(BACKOFF_DEFAULTS);
            }

            public void setBatchSize(Integer batchSize) {
                this.batchSize = batchSize;
            }

            public int getBatchSize() {
                return batchSize;
            }

            public OutboxProperties.PollingProperties getPolling() {
                return polling;
            }

            public OutboxProperties.PollingProperties.Defaults getPoolingDefaults() {
                return OutboxProperties.PollingProperties.Defaults.ofPollingProperties(polling);
            }

            public void setPolling(OutboxProperties.PollingProperties polling) {
                this.polling = polling;
            }

            public int getMaxRetries() {
                return maxRetries;
            }

            public void setMaxRetries(Integer maxRetries) {
                this.maxRetries = maxRetries;
            }

            public BackoffProperties getBackoff() {
                return backoff;
            }

            public BackoffProperties.Defaults getBackoffDefaults() {
                return BackoffProperties.Defaults.ofBackoffProperties(backoff);
            }

            public void setBackoff(BackoffProperties backoff) {
                this.backoff = backoff;
            }

            @Override
            public String toString() {
                return "Defaults{" +
                        "batchSize=" + batchSize +
                        ", polling=" + polling +
                        ", maxRetries=" + maxRetries +
                        ", backoff=" + backoff +
                        '}';
            }
        }
    }

    public static final class StuckRecoveryProperties implements StuckRecoveryPropertiesHolder {

        private static final int DEFAULT_BATCH_SIZE = 500;
        private static final Duration DEFAULT_MAX_BATCH_PROCESSING_TIME = Duration.ofMinutes(5);
        private static final OutboxProperties.PollingProperties.Defaults POLLING_DEFAULTS = OutboxProperties.PollingProperties.Defaults.ofAdaptive(
                PollingType.ADAPTIVE,
                Duration.ofMinutes(5),
                Duration.ofSeconds(1),
                Duration.ofMinutes(1),
                4.0
        );

        private Integer batchSize;
        private Duration maxBatchProcessingTime;
        @NestedConfigurationProperty
        private OutboxProperties.PollingProperties polling;

        public StuckRecoveryProperties() {
            this.batchSize = DEFAULT_BATCH_SIZE;
            this.maxBatchProcessingTime = DEFAULT_MAX_BATCH_PROCESSING_TIME;
            this.polling = new OutboxProperties.PollingProperties();
        }

        public void applyDefaults() {
            batchSize = batchSize == null || batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
            maxBatchProcessingTime = maxBatchProcessingTime == null ? DEFAULT_MAX_BATCH_PROCESSING_TIME : maxBatchProcessingTime;
            polling = polling == null ? new OutboxProperties.PollingProperties() : polling;
            polling.applyDefaults(POLLING_DEFAULTS);
        }

        @Override
        public Integer getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
        }

        @Override
        public Duration getMaxBatchProcessingTime() {
            return maxBatchProcessingTime;
        }

        public void setMaxBatchProcessingTime(Duration maxBatchProcessingTime) {
            this.maxBatchProcessingTime = maxBatchProcessingTime;
        }

        public OutboxProperties.PollingProperties getPolling() {
            return polling;
        }

        public void setPolling(OutboxProperties.PollingProperties polling) {
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
            return "StuckRecoveryProperties{" +
                    "batchSize=" + batchSize +
                    ", maxBatchProcessingTime=" + maxBatchProcessingTime +
                    ", polling=" + polling +
                    '}';
        }
    }

    public static final class DlqProperties implements DlqPropertiesHolder {

        private static final Defaults DEFAULTS = new Defaults(
                500,
                OutboxProperties.PollingProperties.Defaults.ofAdaptive(
                        PollingType.ADAPTIVE,
                        Duration.ofMinutes(5),
                        Duration.ofSeconds(1),
                        Duration.ofMinutes(2),
                        10.0
                )
        );

        private Boolean enabled;
        private Integer batchSize;
        @NestedConfigurationProperty
        private OutboxProperties.PollingProperties polling;
        @NestedConfigurationProperty
        private TransferProperties transferTo;
        @NestedConfigurationProperty
        private TransferProperties transferFrom;
        @NestedConfigurationProperty
        private OutboxProperties.CleanUpProperties cleanUp;

        public void applyDefaults() {
            if (enabled != null && enabled) {
                enabled = true;

                batchSize = batchSize == null || batchSize <= 0 ? DEFAULTS.batchSize() : batchSize;
                polling = polling == null ? new OutboxProperties.PollingProperties() : polling;
                polling.applyDefaults(DEFAULTS.pollingDefaults());

                Defaults currentDefaults = new Defaults(
                        batchSize,
                        OutboxProperties.PollingProperties.Defaults.ofPollingProperties(polling)
                );

                transferTo = transferTo == null ? new TransferProperties() : transferTo;
                transferTo.applyDefaults(currentDefaults);
                transferFrom = transferFrom == null ? new TransferProperties() : transferFrom;
                transferFrom.applyDefaults(currentDefaults);

                if (cleanUp == null) {
                    cleanUp = new OutboxProperties.CleanUpProperties();
                    cleanUp.setEnabled(true);
                }
                cleanUp.applyDefaults();
            } else {
                enabled = false;
                transferTo = new TransferProperties();
                transferFrom = new TransferProperties();

                cleanUp = new OutboxProperties.CleanUpProperties();
                cleanUp.setEnabled(false);
                cleanUp.applyDefaults();
            }
        }

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
        }

        public OutboxProperties.PollingProperties getPolling() {
            return polling;
        }

        public void setPolling(OutboxProperties.PollingProperties polling) {
            this.polling = polling;
        }

        @Override
        public TransferProperties getTransferTo() {
            return transferTo;
        }

        public void setTransferTo(TransferProperties transferTo) {
            this.transferTo = transferTo;
        }

        @Override
        public TransferProperties getTransferFrom() {
            return transferFrom;
        }

        public void setTransferFrom(TransferProperties transferFrom) {
            this.transferFrom = transferFrom;
        }

        public OutboxProperties.CleanUpProperties getCleanUp() {
            return cleanUp;
        }

        public void setCleanUp(OutboxProperties.CleanUpProperties cleanUp) {
            this.cleanUp = cleanUp;
        }

        @Override
        public String toString() {
            return "DlqProperties{" +
                    "enabled=" + enabled +
                    ", transferTo=" + transferTo +
                    ", transferFrom=" + transferFrom +
                    ", cleanUp=" + cleanUp +
                    '}';
        }

        public record Defaults(
                Integer batchSize,
                OutboxProperties.PollingProperties.Defaults pollingDefaults
        ) {}

        public static class TransferProperties implements TransferPropertiesHolder {

            private Integer batchSize;
            @NestedConfigurationProperty
            private OutboxProperties.PollingProperties polling;

            public void applyDefaults(Defaults defaults) {
                batchSize = batchSize == null || batchSize <= 0 ? defaults.batchSize() : batchSize;
                polling = polling == null ? new OutboxProperties.PollingProperties() : polling;
                polling.applyDefaults(defaults.pollingDefaults());
            }

            @Override
            public Integer getBatchSize() {
                return batchSize;
            }

            public void setBatchSize(Integer batchSize) {
                this.batchSize = batchSize;
            }

            public OutboxProperties.PollingProperties getPolling() {
                return polling;
            }

            public void setPolling(OutboxProperties.PollingProperties polling) {
                this.polling = polling;
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
            public Duration getInitialDelay() {
                return polling.getInitialDelay();
            }

            @Override
            public Duration getFixedDelay() {
                return polling.getFixedDelay();
            }

            @Override
            public String toString() {
                return "TransferProperties{" +
                        "batchSize=" + batchSize +
                        ", polling=" + polling +
                        '}';
            }
        }
    }
}