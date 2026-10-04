package io.github.dmitriyiliyov.oncebox.tests.e2e.consume;

import io.github.dmitriyiliyov.oncebox.core.OnceboxHeadersUtils;
import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.BusinessEvent;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.E2eEvents;
import io.github.dmitriyiliyov.oncebox.tests.e2e.repository.TestOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.Message;

public class RabbitConsumerBusinessService {

    private static final Logger log = LoggerFactory.getLogger(RabbitConsumerBusinessService.class);

    private final IdempotentConsumer idempotentConsumer;
    private final TestOutboxRepository repository;

    public RabbitConsumerBusinessService(IdempotentConsumer idempotentConsumer, TestOutboxRepository repository) {
        this.idempotentConsumer = idempotentConsumer;
        this.repository = repository;
    }

    @RabbitListener(queues = E2eEvents.QUEUE, containerFactory = "inboxRabbitListenerContainerFactory")
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
