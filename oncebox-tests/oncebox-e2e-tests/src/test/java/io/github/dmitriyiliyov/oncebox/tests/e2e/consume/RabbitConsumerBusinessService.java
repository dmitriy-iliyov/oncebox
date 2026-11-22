package io.github.dmitriyiliyov.oncebox.tests.e2e.consume;

import io.github.dmitriyiliyov.oncebox.core.OnceboxHeadersUtils;
import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.BusinessEvent;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.E2eEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.Message;

public class RabbitConsumerBusinessService {

    private static final Logger log = LoggerFactory.getLogger(RabbitConsumerBusinessService.class);

    private final IdempotentConsumer transactionalConsumer;
    private final IdempotentConsumer nonTransactionalConsumer;
    private final RecordingBusinessOperation operation;

    public RabbitConsumerBusinessService(IdempotentConsumer transactionalConsumer,
                                         IdempotentConsumer nonTransactionalConsumer,
                                         RecordingBusinessOperation operation) {
        this.transactionalConsumer = transactionalConsumer;
        this.nonTransactionalConsumer = nonTransactionalConsumer;
        this.operation = operation;
    }

    @RabbitListener(queues = E2eEvents.QUEUE, containerFactory = "inboxRabbitListenerContainerFactory")
    public void listen(Message<BusinessEvent> message) {
        consume(transactionalConsumer, message);
    }

    @RabbitListener(queues = E2eEvents.AT_MOST_ONCE_QUEUE, containerFactory = "inboxRabbitListenerContainerFactory")
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
