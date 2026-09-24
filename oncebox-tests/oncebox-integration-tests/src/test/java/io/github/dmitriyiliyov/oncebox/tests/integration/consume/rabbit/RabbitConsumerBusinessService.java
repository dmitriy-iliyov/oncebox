package io.github.dmitriyiliyov.oncebox.tests.integration.consume.rabbit;

import io.github.dmitriyiliyov.oncebox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.messaging.OutboxHeadersUtils;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.ConsumerBusinessRepository;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.ConsumerVariant;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEvent;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class RabbitConsumerBusinessService {

    public static final String SINGLE_QUEUE = "test.outbox.single";
    public static final String BATCH_QUEUE  = "test.outbox.batch";
    public static final String SINGLE_ID_QUEUE = "test.outbox.single.id";
    public static final String BATCH_ID_QUEUE = "test.outbox.batch.id";

    private final OutboxIdempotentConsumer outboxConsumer;
    private final ConsumerBusinessRepository repository;
    private final ConsumerVariant variant;

    public RabbitConsumerBusinessService(OutboxIdempotentConsumer outboxConsumer,
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
     * The queue this bean listens on for the given base name - its endpoints are registered by
     * {@code RabbitIntegrationTestsConfig}, one set per variant.
     */
    public String name(String baseName) {
        return variant.of(baseName);
    }

    @Transactional
    public void listenSingleMessage(Message<BusinessEvent> message) {
        outboxConsumer.consume(
                message,
                OutboxHeadersUtils::extractId,
                msg -> repository.save(msg.getPayload())
        );
    }

    @Transactional
    public void listenBatchMessages(List<Message<BusinessEvent>> messages) {
        outboxConsumer.consume(
                messages,
                OutboxHeadersUtils::extractId,
                deduped -> repository.saveAll(
                        deduped.stream().map(Message::getPayload).toList()
                )
        );
    }

    @Transactional
    public void listenSingleId(Message<BusinessEvent> message) {
        UUID eventId = OutboxHeadersUtils.extractId(message);
        outboxConsumer.consume(
                eventId,
                () -> repository.save(message.getPayload())
        );
    }

    @Transactional
    public void listenBatchIds(List<Message<BusinessEvent>> messages) {
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
                }
        );
    }
}