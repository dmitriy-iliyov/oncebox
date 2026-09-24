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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RabbitConsumerFaultyBusinessService {

    public static final String SINGLE_FAILING_QUEUE = "test.outbox.single.failing";
    public static final String BATCH_FAILING_QUEUE  = "test.outbox.batch.failing";
    public static final String SINGLE_ID_FAILING_QUEUE = "test.outbox.single.id.failing";
    public static final String BATCH_ID_FAILING_QUEUE = "test.outbox.batch.id.failing";

    private final OutboxIdempotentConsumer outboxConsumer;
    private final ConsumerBusinessRepository repository;
    private final ConsumerVariant variant;
    private final AtomicBoolean shouldFail = new AtomicBoolean(true);
    private final AtomicInteger failures = new AtomicInteger();

    public RabbitConsumerFaultyBusinessService(OutboxIdempotentConsumer outboxConsumer,
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

    @Transactional
    public void listenFailing(Message<BusinessEvent> message) {
        outboxConsumer.consume(
                message,
                OutboxHeadersUtils::extractId,
                msg -> {
                    repository.save(msg.getPayload());
                    if (shouldFail.get()) {
                        failures.incrementAndGet();
                        throw new RuntimeException("Simulated business operation failure");
                    }
                }
        );
    }

    @Transactional
    public void listenBatchFailing(List<Message<BusinessEvent>> messages) {
        outboxConsumer.consume(
                messages,
                OutboxHeadersUtils::extractId,
                deduped -> {
                    repository.saveAll(deduped.stream().map(Message::getPayload).toList());
                    if (shouldFail.get()) {
                        failures.incrementAndGet();
                        throw new RuntimeException("Simulated batch business operation failure");
                    }
                }
        );
    }

    @Transactional
    public void listenSingleIdFailing(Message<BusinessEvent> message) {
        UUID eventId = OutboxHeadersUtils.extractId(message);
        outboxConsumer.consume(
                eventId,
                () -> {
                    repository.save(message.getPayload());
                    if (shouldFail.get()) {
                        failures.incrementAndGet();
                        throw new RuntimeException("Simulated business operation failure");
                    }
                }
        );
    }

    @Transactional
    public void listenBatchIdsFailing(List<Message<BusinessEvent>> messages) {
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
                        throw new RuntimeException("Simulated batch business operation failure");
                    }
                }
        );
    }
}