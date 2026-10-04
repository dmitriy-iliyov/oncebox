package io.github.dmitriyiliyov.oncebox.example.consumer;

import io.github.dmitriyiliyov.oncebox.core.OnceboxHeadersUtils;
import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.example.shared.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.List;


@Profile("kafka")
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderAnalyticKafkaListener {

    private final IdempotentConsumer idempotentConsumer;

    /**
     * Example of routing messages to different handlers based on the event type.
     * The event type is extracted from the Kafka record headers using OnceboxHeaders.EVENT_TYPE.
     * This demonstrates how Outbox pattern headers can be used to dynamically dispatch messages.
     */
    @KafkaListener(topics = "orders", groupId = "analytics", containerFactory = "inboxKafkaListenerContainerFactory")
    public void listen(Message<?> message, Acknowledgment ack) {
        try {
            idempotentConsumer.consume(
                    message,
                    OnceboxHeadersUtils::extractId,
                    m -> log.info("Analytics receive '{}' event {}", OnceboxHeadersUtils.extractEventType(m), m.getPayload())
            );
            ack.acknowledge();
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Demonstration of batch processing with KafkaListener.
     * This method shows how to consume a list of records in a single batch
     * and process them together for better throughput and efficiency.
     */
    @KafkaListener(topics = "orders.created", groupId = "analytics", containerFactory = "kafkaBatchListenerContainerFactory")
    public void listenBatch(List<Message<OrderDto>> messages, Acknowledgment ack) {
        try {
            idempotentConsumer.consume(
                    messages,
                    OnceboxHeadersUtils::extractId,
                    messageList -> messageList.forEach(
                            message -> log.info("Analytics receive 'created-order' {}", message.getPayload())
                    )
            );
            ack.acknowledge();
        } catch (Exception e) {
            throw e;
        }
    }
}