package io.github.dmitriyiliyov.oncebox.kafka;

import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxSender;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxHeaders;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.SenderResult;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class KafkaOutboxSender implements OutboxSender {

    private static final Logger log = LoggerFactory.getLogger(KafkaOutboxSender.class);

    /**
     * These exceptions will not cause the entire batch of events to fail
     */
    private static final Set<Class<? extends KafkaException>> IGNORABLE_EXCEPTIONS = Set.of(
            org.apache.kafka.common.errors.RecordTooLargeException.class,
            org.apache.kafka.common.errors.SerializationException.class,
            org.apache.kafka.common.errors.InvalidTopicException.class,
            org.apache.kafka.common.errors.UnknownTopicOrPartitionException.class
    );

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final long emergencyTimeoutMillis;

    public KafkaOutboxSender(KafkaTemplate<String, String> kafkaTemplate, long emergencyTimeoutMillis) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate cannot be null");
        this.emergencyTimeoutMillis = emergencyTimeoutMillis;
    }

    @Override
    public SenderResult sendEvents(String topic, List<OutboxEvent> events) {
        if (events == null || events.isEmpty()) {
            return SenderResult.empty();
        }

        Set<UUID> processedIds = ConcurrentHashMap.newKeySet();
        Set<UUID> failedIds = ConcurrentHashMap.newKeySet();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (OutboxEvent event : events) {
            try {
                Message<?> message = buildMessage(topic, event);
                CompletableFuture<Void> future = kafkaTemplate
                        .send(message)
                        .thenAccept(success -> processedIds.add(event.getId()))
                        .exceptionally(ex -> {
                            failedIds.add(event.getId());
                            log.error("Error when sending event with id={} to topic={} ", event.getId(), topic, ex);
                            return null;
                        });
                futures.add(future);
            } catch (Exception e) {
                log.error("Error when preparing event with id={} to send in topic={}", event.getId(), topic, e);
                if (isInfrastructureError(e)) {
                    failedIds.addAll(events.stream()
                            .map(OutboxEvent::getId)
                            .toList()
                    );
                    log.info("Mark whole '{}' event batch as failed", event.getEventType());
                    return new SenderResult(Collections.emptySet(), new HashSet<>(failedIds));
                }
                failedIds.add(event.getId());
            }
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .orTimeout(emergencyTimeoutMillis, TimeUnit.MILLISECONDS)
                    .join();
        } catch (CompletionException e) {
            events.stream()
                    .filter(event -> !processedIds.contains(event.getId()) && !failedIds.contains(event.getId()))
                    .forEach(event -> failedIds.add(event.getId()));
        }
        return new SenderResult(new HashSet<>(processedIds), new HashSet<>(failedIds));
    }

    private Message<?> buildMessage(String topic, OutboxEvent event) {
        return MessageBuilder
                .withPayload(event.getPayload())
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(OutboxHeaders.EVENT_ID.getValue(), event.getId().toString())
                .setHeader(OutboxHeaders.EVENT_TYPE.getValue(), event.getEventType())
                .setHeader(OutboxHeaders.EVENT_PAYLOAD_TYPE.getValue(), event.getPayloadType())
                .build();
    }

    private boolean isInfrastructureError(Throwable t) {
        while (t != null) {
            if (t instanceof org.apache.kafka.common.KafkaException) {
                for (Class<? extends KafkaException> e : IGNORABLE_EXCEPTIONS) {
                    if (e.isInstance(t)) {
                        return false;
                    }
                }
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
