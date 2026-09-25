package io.github.dmitriyiliyov.oncebox.rabbit;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.SenderResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * The broker's confirms are delivered on the calling thread, right after the channel callback has published
 * every event and before the sender starts waiting for them - the order a real broker cannot guarantee is not
 * what these cases are about, and no case waits on a timer except the two that are about the timeout itself.
 */
@ExtendWith(MockitoExtension.class)
class RabbitOutboxSenderUnitTests {

    private static final String EXCHANGE = "test-exchange";
    private static final long TIMEOUT_MILLIS = 5_000L;
    private static final Instant CREATED_AT = Instant.parse("2026-09-24T10:00:00Z");
    private static final OutboxEvent FIRST = event("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "type1", "payload1");
    private static final OutboxEvent SECOND = event("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "type2", "payload2");
    private static final OutboxEvent THIRD = event("cccccccc-cccc-cccc-cccc-cccccccccccc", "type3", "payload3");

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private Channel channel;

    @Test
    @DisplayName("UT constructor when rabbitTemplate is null should throw NullPointerException")
    void constructor_whenRabbitTemplateIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new RabbitOutboxSender(null, TIMEOUT_MILLIS))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("rabbitTemplate cannot be null");
    }

    @Test
    @DisplayName("UT sendEvents() when events is null should return an empty result without touching the broker")
    void sendEvents_whenEventsIsNull_shouldReturnEmptyResult() {
        // when
        SenderResult result = new RabbitOutboxSender(rabbitTemplate, TIMEOUT_MILLIS).sendEvents(EXCHANGE, null);

        // then
        assertThat(result.processedIds()).isEmpty();
        assertThat(result.failedIds()).isEmpty();
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("UT sendEvents() when events is empty should return an empty result without touching the broker")
    void sendEvents_whenEventsIsEmpty_shouldReturnEmptyResult() {
        // when
        SenderResult result = new RabbitOutboxSender(rabbitTemplate, TIMEOUT_MILLIS)
                .sendEvents(EXCHANGE, Collections.emptyList());

        // then
        assertThat(result.processedIds()).isEmpty();
        assertThat(result.failedIds()).isEmpty();
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("UT sendEvents() should publish each event persistently, routed by its type, with the outbox headers")
    void sendEvents_shouldPublishPersistentlyWithOutboxHeaders() throws Exception {
        // given
        RabbitOutboxSender sender = senderConfirming(1, listener -> listener.handleAck(1L, false));

        // when
        sender.sendEvents(EXCHANGE, List.of(FIRST));

        // then
        ArgumentCaptor<AMQP.BasicProperties> properties = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        verify(channel).basicPublish(eq(EXCHANGE), eq("type1"), eq(false), properties.capture(),
                eq("payload1".getBytes(StandardCharsets.UTF_8)));
        assertThat(properties.getValue().getDeliveryMode()).isEqualTo(2);
        assertThat(properties.getValue().getHeaders())
                .containsEntry("outbox_event_id", FIRST.getId().toString())
                .containsEntry("outbox_event_type", "type1")
                .containsEntry("outbox_event_payload_type", "application/json");
    }

    @Test
    @DisplayName("UT sendEvents() when every event is acked one by one should report all as processed")
    void sendEvents_whenAllAckedIndividually_shouldReportAllProcessed() throws Exception {
        // given
        RabbitOutboxSender sender = senderConfirming(2, listener -> {
            listener.handleAck(1L, false);
            listener.handleAck(2L, false);
        });

        // when
        SenderResult result = sender.sendEvents(EXCHANGE, List.of(FIRST, SECOND));

        // then
        verify(channel).confirmSelect();
        assertThat(result.processedIds()).containsExactlyInAnyOrder(FIRST.getId(), SECOND.getId());
        assertThat(result.failedIds()).isEmpty();
    }

    @Test
    @DisplayName("UT sendEvents() when the last tag is acked with multiple should report every earlier event as processed")
    void sendEvents_whenAckedWithMultiple_shouldReportAllProcessed() throws Exception {
        // given
        RabbitOutboxSender sender = senderConfirming(2, listener -> listener.handleAck(2L, true));

        // when
        SenderResult result = sender.sendEvents(EXCHANGE, List.of(FIRST, SECOND));

        // then
        assertThat(result.processedIds()).containsExactlyInAnyOrder(FIRST.getId(), SECOND.getId());
        assertThat(result.failedIds()).isEmpty();
    }

    @Test
    @DisplayName("UT sendEvents() when every event is nacked one by one should report all as failed")
    void sendEvents_whenAllNackedIndividually_shouldReportAllFailed() throws Exception {
        // given
        RabbitOutboxSender sender = senderConfirming(2, listener -> {
            listener.handleNack(1L, false);
            listener.handleNack(2L, false);
        });

        // when
        SenderResult result = sender.sendEvents(EXCHANGE, List.of(FIRST, SECOND));

        // then
        assertThat(result.failedIds()).containsExactlyInAnyOrder(FIRST.getId(), SECOND.getId());
        assertThat(result.processedIds()).isEmpty();
    }

    @Test
    @DisplayName("UT sendEvents() when the last tag is nacked with multiple should report every earlier event as failed")
    void sendEvents_whenNackedWithMultiple_shouldReportAllFailed() throws Exception {
        // given
        RabbitOutboxSender sender = senderConfirming(2, listener -> listener.handleNack(2L, true));

        // when
        SenderResult result = sender.sendEvents(EXCHANGE, List.of(FIRST, SECOND));

        // then
        assertThat(result.failedIds()).containsExactlyInAnyOrder(FIRST.getId(), SECOND.getId());
        assertThat(result.processedIds()).isEmpty();
    }

    @Test
    @DisplayName("UT sendEvents() when one event is acked and one nacked should split them by their confirms")
    void sendEvents_whenAckAndNack_shouldSplitByConfirm() throws Exception {
        // given
        RabbitOutboxSender sender = senderConfirming(2, listener -> {
            listener.handleAck(1L, false);
            listener.handleNack(2L, false);
        });

        // when
        SenderResult result = sender.sendEvents(EXCHANGE, List.of(FIRST, SECOND));

        // then
        assertThat(result.processedIds()).containsExactly(FIRST.getId());
        assertThat(result.failedIds()).containsExactly(SECOND.getId());
    }

    @Test
    @DisplayName("UT sendEvents() when publishing one event throws should report that event as failed and the rest by their confirms")
    void sendEvents_whenPublishThrowsForOne_shouldReportItFailed() throws Exception {
        // given
        RabbitOutboxSender sender = senderConfirming(2, listener -> listener.handleAck(1L, false));
        lenient().doThrow(new IOException("Publish failed")).when(channel)
                .basicPublish(eq(EXCHANGE), eq("type2"), anyBoolean(), any(AMQP.BasicProperties.class), any(byte[].class));

        // when
        SenderResult result = sender.sendEvents(EXCHANGE, List.of(FIRST, SECOND));

        // then
        assertThat(result.processedIds()).containsExactly(FIRST.getId());
        assertThat(result.failedIds()).containsExactly(SECOND.getId());
    }

    @Test
    @DisplayName("UT sendEvents() when the template cannot open a channel should report the whole batch as failed")
    void sendEvents_whenExecuteThrows_shouldReportAllFailed() {
        // given
        doThrow(new AmqpException("Connection failed")).when(rabbitTemplate).execute(any(ChannelCallback.class));

        // when
        SenderResult result = new RabbitOutboxSender(rabbitTemplate, TIMEOUT_MILLIS).sendEvents(EXCHANGE, List.of(FIRST));

        // then
        assertThat(result.failedIds()).containsExactly(FIRST.getId());
        assertThat(result.processedIds()).isEmpty();
    }

    @Test
    @DisplayName("UT sendEvents() when the timeout expires before every confirm should report the unconfirmed events as failed")
    void sendEvents_whenTimeoutExpires_shouldReportUnconfirmedFailed() throws Exception {
        // given
        RabbitOutboxSender sender = senderConfirming(2, 1_000L, listener -> listener.handleAck(1L, false));

        // when
        SenderResult result = sender.sendEvents(EXCHANGE, List.of(FIRST, SECOND));

        // then
        assertThat(result.processedIds()).containsExactly(FIRST.getId());
        assertThat(result.failedIds()).containsExactly(SECOND.getId());
    }

    @Test
    @DisplayName("UT sendEvents() when the timeout expires with a publish failure among them should report both unfinished events as failed")
    void sendEvents_whenTimeoutExpiresWithPublishFailure_shouldReportBothFailed() throws Exception {
        // given
        RabbitOutboxSender sender = senderConfirming(3, 1_000L, listener -> listener.handleAck(1L, false));
        lenient().doThrow(new IOException("Publish failed")).when(channel)
                .basicPublish(eq(EXCHANGE), eq("type2"), anyBoolean(), any(AMQP.BasicProperties.class), any(byte[].class));

        // when
        SenderResult result = sender.sendEvents(EXCHANGE, List.of(FIRST, SECOND, THIRD));

        // then
        assertThat(result.processedIds()).containsExactly(FIRST.getId());
        assertThat(result.failedIds()).containsExactlyInAnyOrder(SECOND.getId(), THIRD.getId());
    }

    @Test
    @DisplayName("UT sendEvents() when a confirm names an unknown tag or repeats one should count each event once")
    void sendEvents_whenUnknownOrRepeatedConfirm_shouldCountEachEventOnce() throws Exception {
        // given
        RabbitOutboxSender sender = senderConfirming(2, listener -> {
            listener.handleAck(999L, false);
            listener.handleAck(1L, false);
            listener.handleAck(1L, false);
            listener.handleAck(2L, true);
        });

        // when
        SenderResult result = sender.sendEvents(EXCHANGE, List.of(FIRST, SECOND));

        // then
        assertThat(result.processedIds()).containsExactlyInAnyOrder(FIRST.getId(), SECOND.getId());
        assertThat(result.failedIds()).isEmpty();
    }

    private RabbitOutboxSender senderConfirming(int events, Confirms confirms) throws IOException {
        return senderConfirming(events, TIMEOUT_MILLIS, confirms);
    }

    /**
     * Runs the channel callback against the mocked channel and then delivers {@code confirms} to the listener the
     * sender registered, still inside {@code execute} - so they arrive after every publish and before the wait.
     */
    private RabbitOutboxSender senderConfirming(int events, long timeoutMillis, Confirms confirms)
            throws IOException {
        Long[] tags = new Long[events - 1];
        for (int i = 0; i < tags.length; i++) {
            tags[i] = (long) i + 2;
        }
        when(channel.getNextPublishSeqNo()).thenReturn(1L, tags);
        AtomicReference<ConfirmListener> listener = new AtomicReference<>();
        doAnswer(invocation -> {
            listener.set(invocation.getArgument(0));
            return null;
        }).when(channel).addConfirmListener(any(ConfirmListener.class));
        doAnswer(invocation -> {
            ChannelCallback<?> callback = invocation.getArgument(0);
            Object result = callback.doInRabbit(channel);
            confirms.deliver(listener.get());
            return result;
        }).when(rabbitTemplate).execute(any(ChannelCallback.class));
        return new RabbitOutboxSender(rabbitTemplate, timeoutMillis);
    }

    private static OutboxEvent event(String id, String eventType, String payload) {
        return new OutboxEvent(UUID.fromString(id), eventType, "application/json", payload, CREATED_AT);
    }

    @FunctionalInterface
    private interface Confirms {
        void deliver(ConfirmListener listener) throws IOException;
    }
}
