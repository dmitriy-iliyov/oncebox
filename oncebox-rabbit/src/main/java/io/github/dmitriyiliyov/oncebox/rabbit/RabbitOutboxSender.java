package io.github.dmitriyiliyov.oncebox.rabbit;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConfirmListener;
import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxSender;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxHeaders;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.SenderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RabbitOutboxSender implements OutboxSender {

    private static final Logger log = LoggerFactory.getLogger(RabbitOutboxSender.class);

    private final RabbitTemplate rabbitTemplate;
    private final long emergencyTimeoutMillis;

    public RabbitOutboxSender(RabbitTemplate rabbitTemplate, long emergencyTimeoutMillis) {
        this.rabbitTemplate = Objects.requireNonNull(rabbitTemplate, "rabbitTemplate cannot be null");
        this.emergencyTimeoutMillis = emergencyTimeoutMillis;
    }

    @Override
    public SenderResult sendEvents(String exchange, List<OutboxEvent> events) {
        if (events == null || events.isEmpty()) {
            return SenderResult.empty();
        }
        Set<UUID> processedIds = ConcurrentHashMap.newKeySet();
        Set<UUID> failedIds = ConcurrentHashMap.newKeySet();
        Map<Long, UUID> deliveryTagToEventId = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(events.size());
        try {
            rabbitTemplate.execute(channel -> {
                channel.confirmSelect();
                channel.addConfirmListener(new OutboxConfirmListener(processedIds, failedIds, deliveryTagToEventId, latch));
                for (OutboxEvent event : events) {
                    try {
                        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                                .deliveryMode(2)
                                .headers(Map.of(
                                        OutboxHeaders.EVENT_ID.getValue(), event.getId().toString(),
                                        OutboxHeaders.EVENT_TYPE.getValue(), event.getEventType(),
                                        OutboxHeaders.EVENT_PAYLOAD_TYPE.getValue(), event.getPayloadType()
                                ))
                                .build();
                        long deliveryTag = channel.getNextPublishSeqNo();
                        deliveryTagToEventId.put(deliveryTag, event.getId());
                        channel.basicPublish(
                                exchange,
                                event.getEventType(),
                                false,
                                props,
                                event.getPayload().getBytes(StandardCharsets.UTF_8)
                        );
                    } catch (Exception e) {
                        failedIds.add(event.getId());
                        latch.countDown();
                        log.error("Error when sending event with id={} to exchange={} ", event.getId(), exchange, e);
                    }
                }
                return null;
            });
            boolean completed = latch.await(emergencyTimeoutMillis, TimeUnit.MILLISECONDS);
            if (!completed) {
                events.stream()
                        .filter(e -> !processedIds.contains(e.getId()) && !failedIds.contains(e.getId()))
                        .forEach(e -> failedIds.add(e.getId()));
                log.error("Sending batch of events to exchange={} was non completed before timeout ", exchange);
            }
        } catch (Exception e) {
            failedIds.addAll(events.stream().map(OutboxEvent::getId).collect(Collectors.toSet()));
            log.error(
                    "Error when preparing batch of '{}' events to send in exchange={}, mark whole batch as failed",
                    events.getFirst().getEventType(), exchange, e
            );
        }
        return new SenderResult(new HashSet<>(processedIds), new HashSet<>(failedIds));
    }

    private static class OutboxConfirmListener implements ConfirmListener {

        private final Set<UUID> processedIds;
        private final Set<UUID> failedIds;
        private final Map<Long, UUID> deliveryTagToEventId;
        private final CountDownLatch latch;
        private final Set<Long> processedTags = ConcurrentHashMap.newKeySet();

        private OutboxConfirmListener(Set<UUID> processedIds, Set<UUID> failedIds, Map<Long, UUID> deliveryTagToEventId, CountDownLatch latch) {
            this.processedIds = processedIds;
            this.failedIds = failedIds;
            this.deliveryTagToEventId = deliveryTagToEventId;
            this.latch = latch;
        }

        @Override
        public void handleAck(long l, boolean b) {
            handle(l, b, true);
        }

        @Override
        public void handleNack(long l, boolean b) {
            handle(l, b, false);
        }

        private void handle(long tag, boolean multiple, boolean ack) {
            if (multiple) {
                deliveryTagToEventId.entrySet().stream()
                        .filter(e -> e.getKey() <= tag)
                        .filter(e -> !processedTags.contains(e.getKey()))
                        .forEach(e -> {
                            if (ack) {
                                processedIds.add(e.getValue());
                            } else {
                                failedIds.add(e.getValue());
                            }
                            processedTags.add(e.getKey());
                            latch.countDown();
                        });
            } else {
                if (!processedTags.contains(tag)) {
                    UUID id = deliveryTagToEventId.get(tag);
                    if (id != null) {
                        if (ack) {
                            processedIds.add(id);
                        } else {
                            failedIds.add(id);
                        }
                        processedTags.add(tag);
                        latch.countDown();
                    }
                }
            }
        }
    }
}
