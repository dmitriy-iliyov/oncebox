package io.github.dmitriyiliyov.oncebox.core.publisher;

import io.github.dmitriyiliyov.oncebox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.SenderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class DefaultOutboxProcessor implements OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(DefaultOutboxProcessor.class);

    protected final OutboxManager manager;
    protected final OutboxSender sender;
    protected final Clock clock;

    public DefaultOutboxProcessor(OutboxManager manager, OutboxSender sender, Clock clock) {
        this.manager = Objects.requireNonNull(manager, "manager cannot be null");
        this.sender = Objects.requireNonNull(sender, "sender cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public int process(OutboxPublisherPropertiesHolder.EventPropertiesHolder properties) {
        Objects.requireNonNull(properties, "properties cannot be null");

        UUID lockToken = UUID.randomUUID();
        List<OutboxEvent> events = manager.loadBatch(properties.getEventType(), properties.getBatchSize(), lockToken);
        if (events == null) {
            log.warn("Outbox events batch is unexpectedly null, for eventType={}", properties.getEventType());
            return 0;
        }
        if (events.isEmpty()) {
            log.info("Outbox events batch is empty, for eventType={}", properties.getEventType());
            return 0;
        }

        SenderResult result;
        try {
            result = sender.sendEvents(properties.getTopic(), events);
        } catch (Exception e) {
            log.error("Error when processing batch {} events with size={}", properties.getEventType(), properties.getBatchSize(), e);
            result = new SenderResult(
                    null,
                    events.stream()
                            .map(OutboxEvent::getId)
                            .collect(Collectors.toSet())
            );
        }

        manager.finalizeBatch(
                events,
                result.processedIds(),
                result.failedIds(),
                properties.getMaxRetries(),
                retryCount -> {
                    double currentMultiplier = Math.pow(properties.backoffMultiplier(), retryCount);
                    return clock.instant()
                            .plusSeconds((long) currentMultiplier * properties.backoffDelay());
                },
                lockToken
        );
        return events.size();
    }
}
