package io.github.dmitriyiliyov.oncebox.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.SenderResult;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KafkaOutboxSenderUnitTests {

    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    long emergencyTimeoutMillis = 120_000L;

    KafkaOutboxSender tested;

    @BeforeEach
    void setup() {
        tested = new KafkaOutboxSender(kafkaTemplate, emergencyTimeoutMillis);
    }

    @Test
    @DisplayName("UT constructor when kafkaTemplate is null should throw NullPointerException")
    void constructor_whenKafkaTemplateIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new KafkaOutboxSender(null, emergencyTimeoutMillis))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("kafkaTemplate cannot be null");
    }

    @Test
    @DisplayName("UT sendEvents(), when ids is null should return empty sender result")
    public void sendEvents_whenIdsIsNull_shouldNotSendEvents() {
        // given
        String topic = "test-topic";
        List<OutboxEvent> events = null;

        // when
        SenderResult result = tested.sendEvents(topic, events);

        // then
        assertThat(result.failedIds()).isEmpty();
        assertThat(result.processedIds()).isEmpty();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("UT sendEvents(), when ids is empty should send events")
    public void sendEvents_whenIdsIsEmpty_shouldNotSendEvents() {
        // given
        String topic = "test-topic";
        List<OutboxEvent> events = List.of();

        // when
        SenderResult result = tested.sendEvents(topic, events);

        // then
        assertThat(result.failedIds()).isEmpty();
        assertThat(result.processedIds()).isEmpty();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("UT sendEvents(), should send events")
    public void sendEvents_shouldSendEvents() throws JsonProcessingException {
        // given
        String topic = "test-topic";
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                EventStatus.PENDING,
                "TestOutboxEvent",
                TestOutboxEvent.class.getName(),
                "{\"id\":\"%s\"}".formatted(UUID.randomUUID()),
                0,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
        List<OutboxEvent> events = List.of(event);
        when(kafkaTemplate.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(null));

        // when
        SenderResult result = tested.sendEvents(topic, events);

        // then
        assertEquals(Set.of(event.getId()), result.processedIds());
        assertEquals(Set.of(), result.failedIds());
        verify(kafkaTemplate, times(events.size())).send(any(Message.class));
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("UT sendEvents() when Kafka throws, should return correct SenderResult")
    public void sendEvents_whenKafkaThrows_shouldReturnSenderResult() throws JsonProcessingException {
        // given
        String topic = "test-topic";
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                EventStatus.PENDING,
                "TestOutboxEvent",
                TestOutboxEvent.class.getName(),
                "{\"id\":\"%s\"}".formatted(UUID.randomUUID()),
                0,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
        List<OutboxEvent> events = List.of(event);
        when(kafkaTemplate.send(any(Message.class))).thenThrow(new RuntimeException());

        // when
        SenderResult result = tested.sendEvents(topic, events);

        // then
        assertEquals(Set.of(event.getId()), result.failedIds());
        assertEquals(Set.of(), result.processedIds());
        verify(kafkaTemplate, times(events.size())).send(any(Message.class));
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("UT sendEvents() when CompletableFuture throws, should return correct SenderResult")
    public void sendEvents_whenCompletableFutureThrows_shouldReturnSenderResult() throws JsonProcessingException {
        // given
        String topic = "test-topic";
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                EventStatus.PENDING,
                "TestOutboxEvent",
                TestOutboxEvent.class.getName(),
                "{\"id\":\"%s\"}".formatted(UUID.randomUUID()),
                0,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
        List<OutboxEvent> events = List.of(event);
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka send failed"));
        when(kafkaTemplate.send(any(Message.class))).thenReturn((CompletableFuture) future);

        // when
        SenderResult result = tested.sendEvents(topic, events);

        // then
        assertEquals(Set.of(event.getId()), result.failedIds());
        assertEquals(Set.of(), result.processedIds());
        verify(kafkaTemplate, times(events.size())).send(any(Message.class));
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("UT sendEvents() when emergency timeout occurs should add unprocessed to failedIds")
    public void sendEvents_whenEmergencyTimeout_shouldAddUnprocessedToFailedIds() throws JsonProcessingException {
        // given
        tested = new KafkaOutboxSender(kafkaTemplate, 0);
        String topic = "test-topic";
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                EventStatus.PENDING,
                "TestOutboxEvent",
                TestOutboxEvent.class.getName(),
                "{}",
                0,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
        List<OutboxEvent> events = List.of(event);
        
        when(kafkaTemplate.send(any(Message.class))).thenReturn(new CompletableFuture<>());

        // when
        SenderResult result = tested.sendEvents(topic, events);

        // then
        assertEquals(Set.of(event.getId()), result.failedIds());
        assertEquals(Set.of(), result.processedIds());
    }

    @Test
    @DisplayName("UT sendEvents() when detected Kafka infrastructure exception")
    public void sendEvents_whenDetectedKafkaInfrastructureException_shouldFailWholeBatch() throws JsonProcessingException {
        // given
        String topic = "test-topic";
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(),
                EventStatus.PENDING,
                "TestOutboxEvent",
                TestOutboxEvent.class.getName(),
                "invalid-json",
                0,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
        List<OutboxEvent> events = List.of(event);
        when(kafkaTemplate.send(any(Message.class))).thenThrow(new KafkaException());

        // when
        SenderResult result = tested.sendEvents(topic, events);

        // then
        assertEquals(Set.of(event.getId()), result.failedIds());
        assertEquals(Set.of(), result.processedIds());
        verify(kafkaTemplate).send(any(Message.class));
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("UT sendEvents() when detected ignorable Kafka infrastructure exception")
    public void sendEvents_whenDetectedIgnorableKafkaInfrastructureException_shouldFailOnlyProblematicEvent() throws JsonProcessingException {
        // given
        String topic = "test-topic";

        OutboxEvent validEvent = new OutboxEvent(
                UUID.randomUUID(),
                EventStatus.PENDING,
                "TestOutboxEvent",
                TestOutboxEvent.class.getName(),
                "{}",
                0,
                Instant.now(),
                Instant.now(),
                null
        );

        OutboxEvent badEvent = new OutboxEvent(
                UUID.randomUUID(),
                EventStatus.PENDING,
                "TestOutboxEvent",
                TestOutboxEvent.class.getName(),
                "{}",
                0,
                Instant.now(),
                Instant.now(),
                null
        );

        List<OutboxEvent> events = List.of(validEvent, badEvent);

        when(kafkaTemplate.send(any(Message.class)))
                .thenReturn(CompletableFuture.completedFuture(null))
                .thenThrow(new org.apache.kafka.common.errors.SerializationException("Too big or bad format"));

        // when
        SenderResult result = tested.sendEvents(topic, events);

        // then
        assertTrue(result.processedIds().contains(validEvent.getId()), "Valid event should be processed");
        assertTrue(result.failedIds().contains(badEvent.getId()), "Bad event should be failed");

        assertEquals(1, result.processedIds().size());
        assertEquals(1, result.failedIds().size());

        verify(kafkaTemplate, times(2)).send(any(Message.class));
    }

    @Test
    @DisplayName("UT sendEvents() when CompletionException and event is partially processed or failed, should add remaining to failed")
    public void sendEvents_whenCompletionException_shouldAddRemainingToFailed() {
        // given
        tested = new KafkaOutboxSender(kafkaTemplate, 0);
        String topic = "test-topic";
        OutboxEvent event1 = new OutboxEvent(
                UUID.randomUUID(),
                EventStatus.PENDING,
                "TestOutboxEvent",
                TestOutboxEvent.class.getName(),
                "{}",
                0,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
        OutboxEvent event2 = new OutboxEvent(
                UUID.randomUUID(),
                EventStatus.PENDING,
                "TestOutboxEvent",
                TestOutboxEvent.class.getName(),
                "{}",
                0,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
        OutboxEvent event3 = new OutboxEvent(
                UUID.randomUUID(),
                EventStatus.PENDING,
                "TestOutboxEvent",
                TestOutboxEvent.class.getName(),
                "{}",
                0,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
        List<OutboxEvent> events = List.of(event1, event2, event3);

        when(kafkaTemplate.send(any(Message.class))).thenAnswer(invocation -> {
            Message<?> msg = invocation.getArgument(0);
            String idStr = (String) msg.getHeaders().get(io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxHeaders.EVENT_ID.getValue());
            UUID id = UUID.fromString(idStr);
            if (id.equals(event1.getId())) {
                return CompletableFuture.completedFuture(null);
            } else if (id.equals(event2.getId())) {
                CompletableFuture<Object> failed = new CompletableFuture<>();
                failed.completeExceptionally(new RuntimeException("failed"));
                return failed;
            } else {
                return new CompletableFuture<>();
            }
        });

        // when
        SenderResult result = tested.sendEvents(topic, events);

        // then
        assertThat(result.processedIds()).containsExactly(event1.getId());
        assertThat(result.failedIds()).containsExactlyInAnyOrder(event2.getId(), event3.getId());
    }
}
