package io.github.dmitriyiliyov.oncebox.tests.integration.consume.kafka;

import io.github.dmitriyiliyov.oncebox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.messaging.OutboxHeadersUtils;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.ConsumerBusinessRepository;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.ConsumerVariant;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class KafkaConsumerFaultyBusinessService {

    public static final String SINGLE_FAILING_TOPIC = "test.outbox.single.failing";
    public static final String BATCH_FAILING_TOPIC  = "test.outbox.batch.failing";
    public static final String SINGLE_ID_FAILING_TOPIC = "test.outbox.single.id.failing";
    public static final String BATCH_ID_FAILING_TOPIC = "test.outbox.batch.id.failing";
    public static final String CONSUMER_GROUP = "test-outbox-faulty-consumer";

    private final OutboxIdempotentConsumer outboxConsumer;
    private final ConsumerBusinessRepository repository;
    private final ConsumerVariant variant;
    private final AtomicBoolean shouldFail = new AtomicBoolean(true);
    private final AtomicInteger failures = new AtomicInteger();

    public KafkaConsumerFaultyBusinessService(OutboxIdempotentConsumer outboxConsumer,
                                              ConsumerBusinessRepository repository,
                                              ConsumerVariant variant) {
        this.outboxConsumer = outboxConsumer;
        this.repository = repository;
        this.variant = variant;
    }

    public ConsumerVariant variant() {
        return variant;
    }

    /**
     * The queue or topic this bean listens on for the given base name - read by the listener annotations.
     */
    public String name(String baseName) {
        return variant.of(baseName);
    }

    public void setShouldFail(boolean fail) {
        shouldFail.set(fail);
    }

    /**
     * How many times a business operation failed after writing its rows - a rollback test waits for one, so it
     * does not pass on a message that never arrived.
     */
    public int failures() {
        return failures.get();
    }

    @KafkaListener(topics = "#{__listener.name('" + SINGLE_FAILING_TOPIC + "')}", groupId = CONSUMER_GROUP, containerFactory = "testSingleKafkaListenerContainerFactory")
    public void listenFailing(Message<BusinessEvent> message, Acknowledgment ack) {
        try {
            outboxConsumer.consume(
                    message,
                    OutboxHeadersUtils::extractId,
                    msg -> {
                        repository.save(msg.getPayload());
                        if (shouldFail.get()) {
                            failures.incrementAndGet();
                            throw new RuntimeException("Exception in business operation");
                        }
                    }
            );
            ack.acknowledge();
        } catch (Exception e) {
            throw e;
        }
    }

    @KafkaListener(topics = "#{__listener.name('" + BATCH_FAILING_TOPIC + "')}", groupId = CONSUMER_GROUP, containerFactory = "testBatchKafkaListenerContainerFactory")
    public void listenBatchFailing(List<Message<BusinessEvent>> messages, Acknowledgment ack) {
        try {
            outboxConsumer.consume(
                    messages,
                    OutboxHeadersUtils::extractId,
                    deduped -> {
                        repository.saveAll(deduped.stream().map(Message::getPayload).toList());
                        if (shouldFail.get()) {
                            failures.incrementAndGet();
                            throw new RuntimeException("Exception in business operation");
                        }
                    }
            );
            ack.acknowledge();
        } catch (Exception e) {
            throw e;
        }
    }

    @KafkaListener(topics = "#{__listener.name('" + SINGLE_ID_FAILING_TOPIC + "')}", groupId = CONSUMER_GROUP, containerFactory = "testSingleKafkaListenerContainerFactory")
    public void listenSingleIdFailing(Message<BusinessEvent> message, Acknowledgment ack) {
        try {
            UUID eventId = OutboxHeadersUtils.extractId(message);
            outboxConsumer.consume(
                    eventId,
                    () -> {
                        repository.save(message.getPayload());
                        if (shouldFail.get()) {
                            failures.incrementAndGet();
                            throw new RuntimeException("Exception in business operation");
                        }
                    }
            );
            ack.acknowledge();
        } catch (Exception e) {
            throw e;
        }
    }

    @KafkaListener(topics = "#{__listener.name('" + BATCH_ID_FAILING_TOPIC + "')}", groupId = CONSUMER_GROUP, containerFactory = "testBatchKafkaListenerContainerFactory")
    public void listenBatchIdsFailing(List<Message<BusinessEvent>> messages, Acknowledgment ack) {
        try {
            Set<UUID> allIds = messages.stream()
                    .map(OutboxHeadersUtils::extractId)
                    .collect(Collectors.toSet());

            outboxConsumer.consume(
                    allIds,
                    newIds -> {
                        List<BusinessEvent> eventsToSave = messages.stream()
                                .filter(msg -> newIds.contains(OutboxHeadersUtils.extractId(msg)))
                                .map(Message::getPayload)
                                .toList();

                        repository.saveAll(eventsToSave);
                        if (shouldFail.get()) {
                            failures.incrementAndGet();
                            throw new RuntimeException("Exception in business operation");
                        }
                    }
            );
            ack.acknowledge();
        } catch (Exception e) {
            throw e;
        }
    }
}
