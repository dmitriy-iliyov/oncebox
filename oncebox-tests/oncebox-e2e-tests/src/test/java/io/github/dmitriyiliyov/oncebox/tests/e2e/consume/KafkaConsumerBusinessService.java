package io.github.dmitriyiliyov.oncebox.tests.e2e.consume;

import io.github.dmitriyiliyov.oncebox.core.OnceboxHeadersUtils;
import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.BusinessEvent;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.E2eEvents;
import io.github.dmitriyiliyov.oncebox.tests.e2e.repository.TestOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;

public class KafkaConsumerBusinessService {

    public static final String CONSUMER_GROUP = "e2e-consumer-group";

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerBusinessService.class);

    private final IdempotentConsumer idempotentConsumer;
    private final TestOutboxRepository repository;

    public KafkaConsumerBusinessService(IdempotentConsumer idempotentConsumer, TestOutboxRepository repository) {
        this.idempotentConsumer = idempotentConsumer;
        this.repository = repository;
    }

    @KafkaListener(topics = E2eEvents.TOPIC, groupId = CONSUMER_GROUP, containerFactory = "inboxKafkaListenerContainerFactory")
    public void listen(Message<BusinessEvent> message) {
        try {
            idempotentConsumer.consume(
                    message,
                    OnceboxHeadersUtils::extractId,
                    msg -> repository.saveConsumedBusiness(msg.getPayload().verifyId())
            );
        } catch (Exception e) {
            log.error("Error when consuming message", e);
            throw e;
        }
    }
}
