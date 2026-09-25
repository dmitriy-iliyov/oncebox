package io.github.dmitriyiliyov.oncebox.core.publisher;

import io.github.dmitriyiliyov.oncebox.core.OutboxPublisherPropertiesHolder;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.SenderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;

@ExtendWith(MockitoExtension.class)
class DefaultOutboxProcessorUnitTests {

    @Mock
    OutboxManager manager;

    @Mock
    OutboxSender sender;

    @Mock
    Clock clock;

    @InjectMocks
    DefaultOutboxProcessor tested;

    OutboxPublisherPropertiesHolder.EventPropertiesHolder properties;
    String eventType;
    String topic;
    int batchSize;
    int maxRetries;

    @BeforeEach
    void setUpProperties() {
        properties = mock(OutboxPublisherPropertiesHolder.EventPropertiesHolder.class);
        eventType = "test-event-type";
        topic = "test-topic";
        batchSize = 10;
        maxRetries = 1;

        lenient().when(properties.getEventType()).thenReturn("test-event-type");
        lenient().when(properties.getTopic()).thenReturn("test-topic");
        lenient().when(properties.getBatchSize()).thenReturn(10);
        lenient().when(properties.getMaxRetries()).thenReturn(1);
    }

    @Test
    @DisplayName("UT constructor when manager is null should throw NullPointerException")
    void constructor_whenManagerIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultOutboxProcessor(null, sender, clock))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("manager cannot be null");
    }

    @Test
    @DisplayName("UT constructor when sender is null should throw NullPointerException")
    void constructor_whenSenderIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultOutboxProcessor(manager, null, clock))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("sender cannot be null");
    }

    @Test
    @DisplayName("UT constructor when clock is null should throw NullPointerException")
    void constructor_whenClockIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultOutboxProcessor(manager, sender, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("clock cannot be null");
    }

    @Test
    @DisplayName("UT process() when properties non null, should send and finalize")
    void process_whenPropertiesNonNull_shouldSendAndFinalize() {
        // given
        OutboxEvent event1 = mock(OutboxEvent.class);
        OutboxEvent event2 = mock(OutboxEvent.class);
        List<OutboxEvent> events = List.of(event1, event2);
        UUID processedId = UUID.randomUUID();
        UUID failedId = UUID.randomUUID();
        Set<UUID> processedIds = Set.of(processedId);
        Set<UUID> failedIds = Set.of(failedId);
        SenderResult senderResult = new SenderResult(processedIds, failedIds);

        when(manager.loadBatch(eq(eventType), eq(batchSize), any(UUID.class))).thenReturn(events);
        when(sender.sendEvents(topic, events)).thenReturn(senderResult);

        // when
        tested.process(properties);

        // then
        verify(manager, times(1)).loadBatch(eq(eventType), eq(batchSize), any(UUID.class));
        verify(sender, times(1)).sendEvents(topic, events);
        verify(manager, times(1)).finalizeBatch(eq(events), eq(processedIds), eq(failedIds),
                eq(maxRetries), any(Function.class), any(UUID.class));
        verifyNoMoreInteractions(manager, sender);
    }

    @Test
    @DisplayName("UT process() should finalize the batch with the lock token it was loaded with")
    @SuppressWarnings("unchecked")
    void process_shouldFinalizeWithLockTokenBatchWasLoadedWith() {
        // given
        OutboxEvent event = mock(OutboxEvent.class);
        List<OutboxEvent> events = List.of(event);
        when(manager.loadBatch(eq(eventType), eq(batchSize), any(UUID.class))).thenReturn(events);
        when(sender.sendEvents(topic, events)).thenReturn(new SenderResult(Set.of(), Set.of()));

        // when
        tested.process(properties);

        // then
        ArgumentCaptor<UUID> loadToken = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<UUID> finalizeToken = ArgumentCaptor.forClass(UUID.class);
        verify(manager).loadBatch(eq(eventType), eq(batchSize), loadToken.capture());
        verify(manager).finalizeBatch(eq(events), any(), any(), eq(maxRetries), any(Function.class), finalizeToken.capture());
        assertThat(loadToken.getValue()).isNotNull();
        assertThat(finalizeToken.getValue()).isEqualTo(loadToken.getValue());
    }

    @Test
    @DisplayName("UT process() when sender throws, should finalize all as failed")
    void process_whenSenderThrows_shouldFinalizeAllAsFailed() {
        // given
        OutboxEvent event1 = mock(OutboxEvent.class);
        OutboxEvent event2 = mock(OutboxEvent.class);
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(event1.getId()).thenReturn(id1);
        when(event2.getId()).thenReturn(id2);
        List<OutboxEvent> events = List.of(event1, event2);

        when(manager.loadBatch(eq(eventType), eq(batchSize), any(UUID.class))).thenReturn(events);
        when(sender.sendEvents(topic, events)).thenThrow(RuntimeException.class);

        // when
        tested.process(properties);

        // then
        ArgumentCaptor<Set<UUID>> processedCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Set<UUID>> failedCaptor = ArgumentCaptor.forClass(Set.class);

        verify(manager).loadBatch(eq(eventType), eq(batchSize), any(UUID.class));
        verify(sender).sendEvents(topic, events);
        verify(manager).finalizeBatch(eq(events), processedCaptor.capture(), failedCaptor.capture(), eq(maxRetries), any(Function.class), any(UUID.class));

        assertThat(processedCaptor.getValue()).isNull();
        assertThat(failedCaptor.getValue()).containsExactlyInAnyOrder(id1, id2);
        verifyNoMoreInteractions(manager, sender);
    }

    @Test
    @DisplayName("UT process() when properties is null, should throw NPE")
    void process_whenPropertiesAreNull_shouldThrow() {
        assertThrows(NullPointerException.class, () -> tested.process(null));
        verifyNoInteractions(manager, sender);
    }

    @Test
    @DisplayName("UT process() when loaded events is null, should early returns")
    public void process_whenLoadedEventsIsNull_shouldEarlyReturns() {
        // given
        when(manager.loadBatch(eq(eventType), eq(batchSize), any(UUID.class))).thenReturn(null);

        // when
        tested.process(properties);

        // then
        verify(manager, times(1)).loadBatch(eq(eventType), eq(batchSize), any(UUID.class));
        verifyNoMoreInteractions(manager);
    }

    @Test
    @DisplayName("UT process() when loaded events is empty, should early returns")
    void process_whenLoadedEventsIsEmpty_shouldEarlyReturn() {
        when(manager.loadBatch(eq(eventType), eq(batchSize), any(UUID.class))).thenReturn(List.of());

        tested.process(properties);

        verify(manager).loadBatch(eq(eventType), eq(batchSize), any(UUID.class));
        verifyNoMoreInteractions(manager, sender);
    }

    @Test
    @DisplayName("UT process() when batch fails should make each retry wait multiplier times the previous one")
    void process_whenBatchFails_shouldGrowRetryDelayByMultiplier() {
        // given
        Function<Integer, Instant> nextRetryAt = captureNextRetryAt(3.0, 10_000L);

        // when
        Duration first = Duration.between(NOW, nextRetryAt.apply(1));
        Duration second = Duration.between(NOW, nextRetryAt.apply(2));

        // then
        assertThat(first).isPositive();
        assertThat(second).isEqualTo(first.multipliedBy(3));
    }

    @Test
    @DisplayName("UT process() when multiplier is fractional should grow the retry delay by the exact multiplier")
    void process_whenMultiplierIsFractional_shouldGrowRetryDelayByExactMultiplier() {
        // given
        Function<Integer, Instant> nextRetryAt = captureNextRetryAt(1.5, 10_000L);

        // when
        Duration first = Duration.between(NOW, nextRetryAt.apply(1));
        Duration second = Duration.between(NOW, nextRetryAt.apply(2));

        // then
        assertThat(second.toMillis()).isEqualTo(first.toMillis() * 3 / 2);
    }

    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");

    @SuppressWarnings("unchecked")
    private Function<Integer, Instant> captureNextRetryAt(double multiplier, long delay) {
        OutboxEvent event = mock(OutboxEvent.class);
        when(event.getId()).thenReturn(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        when(manager.loadBatch(eq(eventType), eq(batchSize), any(UUID.class))).thenReturn(List.of(event));
        when(sender.sendEvents(topic, List.of(event))).thenThrow(new RuntimeException("broker down"));
        when(properties.backoffMultiplier()).thenReturn(multiplier);
        when(properties.backoffDelay()).thenReturn(delay);
        when(clock.instant()).thenReturn(NOW);

        tested.process(properties);

        ArgumentCaptor<Function<Integer, Instant>> captor = ArgumentCaptor.forClass(Function.class);
        verify(manager).finalizeBatch(anyList(), any(), anySet(), anyInt(), captor.capture(), any(UUID.class));
        return captor.getValue();
    }
}
