package io.github.dmitriyiliyov.oncebox.tests.e2e.consume;

import io.github.dmitriyiliyov.oncebox.core.OnceboxHeadersUtils;
import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.BusinessEvent;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.E2eEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;

public class KafkaConsumerBusinessService {

    public static final String CONSUMER_GROUP = "e2e-consumer-group";

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerBusinessService.class);

    private final IdempotentConsumer transactionalConsumer;
    private final IdempotentConsumer nonTransactionalConsumer;
    private final RecordingBusinessOperation operation;

    public KafkaConsumerBusinessService(IdempotentConsumer transactionalConsumer,
                                        IdempotentConsumer nonTransactionalConsumer,
                                        RecordingBusinessOperation operation) {
        this.transactionalConsumer = transactionalConsumer;
        this.nonTransactionalConsumer = nonTransactionalConsumer;
        this.operation = operation;
    }

    @KafkaListener(topics = E2eEvents.TOPIC, groupId = CONSUMER_GROUP, containerFactory = "inboxKafkaListenerContainerFactory")
    public void listen(Message<BusinessEvent> message) {
        consume(transactionalConsumer, message);
    }

    @KafkaListener(
            topics = E2eEvents.AT_MOST_ONCE_TOPIC,
            groupId = CONSUMER_GROUP,
            containerFactory = "inboxKafkaListenerContainerFactory"
    )
    public void listenAtMostOnce(Message<BusinessEvent> message) {
        consume(nonTransactionalConsumer, message);
    }

    private void consume(IdempotentConsumer consumer, Message<BusinessEvent> message) {
        operation.recordDelivery(message.getPayload().verifyId());
        try {
            consumer.consume(
                    message,
                    OnceboxHeadersUtils::extractId,
                    msg -> operation.run(msg.getPayload().verifyId())
            );
        } catch (Exception e) {
            log.error("Error when consuming message", e);
            throw e;
        }
    }
}
