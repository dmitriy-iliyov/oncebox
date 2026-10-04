package io.github.dmitriyiliyov.oncebox.tests.e2e.publish;

import io.github.dmitriyiliyov.oncebox.core.OnceboxHeaders;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.E2eEvents;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class RabbitRawEventResender implements RawEventResender {

    private final RabbitTemplate rabbitTemplate;

    public RabbitRawEventResender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void resend(UUID eventId, String eventType, String payloadType, String payloadJson) {
        // Mirrors how the library's RabbitOutboxSender publishes: raw JSON bytes with the outbox headers,
        // routed through the TOPIC exchange with the event type as routing key
        MessageProperties props = new MessageProperties();
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        props.setHeader(OnceboxHeaders.EVENT_ID.getValue(), eventId.toString());
        props.setHeader(OnceboxHeaders.EVENT_TYPE.getValue(), eventType);
        props.setHeader(OnceboxHeaders.EVENT_PAYLOAD_TYPE.getValue(), payloadType);
        Message message = new Message(payloadJson.getBytes(StandardCharsets.UTF_8), props);
        rabbitTemplate.send(E2eEvents.TOPIC, eventType, message);
    }
}
