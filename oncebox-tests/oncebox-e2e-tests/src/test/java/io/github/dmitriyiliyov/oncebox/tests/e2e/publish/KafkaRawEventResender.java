package io.github.dmitriyiliyov.oncebox.tests.e2e.publish;

import io.github.dmitriyiliyov.oncebox.core.OnceboxHeaders;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.E2eEvents;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;

public class KafkaRawEventResender implements RawEventResender {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaRawEventResender(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void resend(UUID eventId, String eventType, String payloadType, String payloadJson) {
        Message<String> message = MessageBuilder
                .withPayload(payloadJson)
                .setHeader(KafkaHeaders.TOPIC, E2eEvents.TOPIC)
                .setHeader(OnceboxHeaders.EVENT_ID.getValue(), eventId.toString())
                .setHeader(OnceboxHeaders.EVENT_TYPE.getValue(), eventType)
                .setHeader(OnceboxHeaders.EVENT_PAYLOAD_TYPE.getValue(), payloadType)
                .build();
        kafkaTemplate.send(message).join();
    }
}
