package io.github.dmitriyiliyov.oncebox.core.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.domain.SenderResult;
import io.github.dmitriyiliyov.oncebox.core.utils.SetUtils;
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
    public int process(OutboxPropertiesHolder.EventPropertiesHolder properties) {
        Objects.requireNonNull(properties, "properties cannot be null");

        UUID lockToken = UUID.randomUUID();
        List<OutboxEvent> events = manager.loadBatch(properties.getEventType(), properties.getBatchSize(), lockToken);

        if (events == null) {
            log.debug("Outbox events batch is unexpectedly null, eventType={}", properties.getEventType());
            return 0;
        }
        if (events.isEmpty()) {
            log.debug("Outbox events batch is empty, eventType={}", properties.getEventType());
            return 0;
        }

        SenderResult result = sendEvents(properties, events, lockToken);

        manager.finalizeBatch(
                events,
                result.processedIds(),
                result.failedIds(),
                properties.getMaxRetries(),
                retryCount -> {
                    double currentMultiplier = Math.pow(properties.backoffMultiplier(), retryCount);
                    return clock.instant()
                            .plusMillis((long) (currentMultiplier * properties.backoffDelay()));
                },
                lockToken
        );

        log.debug(
                "Outbox events batch finalized, eventType={}, size={}, processed={}, failed={}, lockToken={}",
                properties.getEventType(), events.size(),
                SetUtils.size(result.processedIds()), SetUtils.size(result.failedIds()), lockToken
        );

        return events.size();
    }

    private SenderResult sendEvents(OutboxPropertiesHolder.EventPropertiesHolder properties,
                                    List<OutboxEvent> events,
                                    UUID lockToken) {
        try {
            return sender.sendEvents(properties.getTopic(), events);
        } catch (Exception e) {
            log.error(
                    "Error when sending outbox events batch, eventType={}, topic={}, size={}, lockToken={}",
                    properties.getEventType(), properties.getTopic(), events.size(), lockToken, e
            );
            return new SenderResult(
                    null,
                    events.stream()
                            .map(OutboxEvent::getId)
                            .collect(Collectors.toSet())
            );
        }
    }
}
