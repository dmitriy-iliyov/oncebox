package io.github.dmitriyiliyov.oncebox.tests.integration.consume.rabbit;

import io.github.dmitriyiliyov.oncebox.core.OnceboxHeadersUtils;
import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;
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

    private final IdempotentConsumer idempotentConsumer;
    private final ConsumerBusinessRepository repository;
    private final ConsumerVariant variant;

    public RabbitConsumerBusinessService(IdempotentConsumer idempotentConsumer,
                                         ConsumerBusinessRepository repository,
                                         ConsumerVariant variant) {
        this.idempotentConsumer = idempotentConsumer;
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
        idempotentConsumer.consume(
                message,
                OnceboxHeadersUtils::extractId,
                msg -> repository.save(msg.getPayload())
        );
    }

    @Transactional
    public void listenBatchMessages(List<Message<BusinessEvent>> messages) {
        idempotentConsumer.consume(
                messages,
                OnceboxHeadersUtils::extractId,
                deduped -> repository.saveAll(
                        deduped.stream().map(Message::getPayload).toList()
                )
        );
    }

    @Transactional
    public void listenSingleId(Message<BusinessEvent> message) {
        UUID eventId = OnceboxHeadersUtils.extractId(message);
        idempotentConsumer.consume(
                eventId,
                () -> repository.save(message.getPayload())
        );
    }

    @Transactional
    public void listenBatchIds(List<Message<BusinessEvent>> messages) {
        Set<UUID> allIds = messages.stream()
                .map(OnceboxHeadersUtils::extractId)
                .collect(Collectors.toSet());

        idempotentConsumer.consume(
                allIds,
                newIds -> {
                    List<BusinessEvent> eventsToSave = messages.stream()
                            .filter(msg -> newIds.contains(OnceboxHeadersUtils.extractId(msg)))
                            .map(Message::getPayload)
                            .toList();
                    repository.saveAll(eventsToSave);
                }
        );
    }
}