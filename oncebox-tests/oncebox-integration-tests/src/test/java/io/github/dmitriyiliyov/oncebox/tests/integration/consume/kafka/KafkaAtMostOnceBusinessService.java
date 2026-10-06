package io.github.dmitriyiliyov.oncebox.tests.integration.consume.kafka;

import io.github.dmitriyiliyov.oncebox.core.OnceboxHeadersUtils;
import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.ConsumerBusinessRepository;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.ConsumerVariant;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.OperationRuns;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Listens through the non-transactional consumer, and its every operation writes its rows and then fails. No
 * listener method is transactional: a transaction around the consumer would take the recorded IDs back with it.
 */
public class KafkaAtMostOnceBusinessService {

    public static final String SINGLE_TOPIC = "test.outbox.single.at-most-once";
    public static final String BATCH_TOPIC = "test.outbox.batch.at-most-once";
    public static final String SINGLE_ID_TOPIC = "test.outbox.single.id.at-most-once";
    public static final String BATCH_ID_TOPIC = "test.outbox.batch.id.at-most-once";
    public static final String CONSUMER_GROUP = "test-outbox-at-most-once-consumer";

    private final IdempotentConsumer idempotentConsumer;
    private final ConsumerBusinessRepository repository;
    private final ConsumerVariant variant;
    private final OperationRuns runs = new OperationRuns();

    public KafkaAtMostOnceBusinessService(IdempotentConsumer idempotentConsumer,
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
     * The queue or topic this bean listens on for the given base name - read by the listener annotations.
     */
    public String name(String baseName) {
        return variant.of(baseName);
    }

    public OperationRuns runs() {
        return runs;
    }

    @KafkaListener(topics = "#{__listener.name('" + SINGLE_TOPIC + "')}", groupId = CONSUMER_GROUP, containerFactory = "testSingleKafkaListenerContainerFactory")
    public void listenSingleMessage(Message<BusinessEvent> message) {
        idempotentConsumer.consume(
                message,
                OnceboxHeadersUtils::extractId,
                msg -> {
                    runs.record(Set.of(OnceboxHeadersUtils.extractId(msg)));
                    repository.save(msg.getPayload());
                    throw new RuntimeException("Simulated business operation failure");
                }
        );
    }

    @KafkaListener(topics = "#{__listener.name('" + BATCH_TOPIC + "')}", groupId = CONSUMER_GROUP, containerFactory = "testBatchKafkaListenerContainerFactory")
    public void listenBatchMessages(List<Message<BusinessEvent>> messages) {
        idempotentConsumer.consume(
                messages,
                OnceboxHeadersUtils::extractId,
                deduped -> {
                    runs.record(deduped.stream().map(OnceboxHeadersUtils::extractId).collect(Collectors.toSet()));
                    repository.saveAll(deduped.stream().map(Message::getPayload).toList());
                    throw new RuntimeException("Simulated batch business operation failure");
                }
        );
    }

    @KafkaListener(topics = "#{__listener.name('" + SINGLE_ID_TOPIC + "')}", groupId = CONSUMER_GROUP, containerFactory = "testSingleKafkaListenerContainerFactory")
    public void listenSingleId(Message<BusinessEvent> message) {
        UUID eventId = OnceboxHeadersUtils.extractId(message);
        idempotentConsumer.consume(
                eventId,
                () -> {
                    runs.record(Set.of(eventId));
                    repository.save(message.getPayload());
                    throw new RuntimeException("Simulated business operation failure");
                }
        );
    }

    @KafkaListener(topics = "#{__listener.name('" + BATCH_ID_TOPIC + "')}", groupId = CONSUMER_GROUP, containerFactory = "testBatchKafkaListenerContainerFactory")
    public void listenBatchIds(List<Message<BusinessEvent>> messages) {
        Set<UUID> allIds = messages.stream()
                .map(OnceboxHeadersUtils::extractId)
                .collect(Collectors.toSet());

        idempotentConsumer.consume(
                allIds,
                newIds -> {
                    runs.record(newIds);
                    repository.saveAll(messages.stream()
                            .filter(msg -> newIds.contains(OnceboxHeadersUtils.extractId(msg)))
                            .map(Message::getPayload)
                            .toList());
                    throw new RuntimeException("Simulated batch business operation failure");
                }
        );
    }
}
