package io.github.dmitriyiliyov.oncebox.example.consumer;

import io.github.dmitriyiliyov.oncebox.core.OnceboxHeadersUtils;
import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.example.shared.OrderDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.List;

@Profile("rabbit")
@Component
@Slf4j
public class OrderAnalyticRabbitListener {

    private final IdempotentConsumer idempotentConsumer;

    public OrderAnalyticRabbitListener(
            @Qualifier("inboxTransactionalIdempotentConsumer") IdempotentConsumer idempotentConsumer
    ) {
        this.idempotentConsumer = idempotentConsumer;
    }

    @RabbitListener(queues = "orders", containerFactory = "inboxRabbitListenerContainerFactory")
    public void listen(Message<?> message) {
        idempotentConsumer.consume(
                message,
                OnceboxHeadersUtils::extractId,
                m -> log.info("Analytics receive '{}' event {}", OnceboxHeadersUtils.extractEventType(m), m.getPayload())
        );
    }

    @RabbitListener(queues = "orders.created", containerFactory = "rabbitBatchListenerContainerFactory")
    public void listenBatch(List<Message<OrderDto>> messages) {
        idempotentConsumer.consume(
                messages,
                OnceboxHeadersUtils::extractId,
                m -> m.forEach(
                        message -> log.info("Analytics receive '{}' event {}", OnceboxHeadersUtils.extractEventType(message), message.getPayload())
                )
        );
    }
}
