package io.github.dmitriyiliyov.oncebox.core.inbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultIdempotentConsumerUnitTests {

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private InboxManager inboxManager;

    private DefaultIdempotentConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new DefaultIdempotentConsumer(transactionTemplate, inboxManager);
    }

    @Test
    @DisplayName("UT constructor when transactionTemplate is null should throw NullPointerException")
    void constructor_whenTransactionTemplateIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultIdempotentConsumer(null, inboxManager))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("transactionTemplate cannot be null");
    }

    @Test
    @DisplayName("UT constructor when inboxManager is null should throw NullPointerException")
    void constructor_whenInboxManagerIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultIdempotentConsumer(transactionTemplate, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("inboxManager cannot be null");
    }

    private void mockTransactionTemplateExecute() {
        doAnswer(invocation -> {
            java.util.function.Consumer<?> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    @DisplayName("consume(UUID, Runnable): should execute operation when event is not consumed")
    void consumeRunnable_whenEventNotConsumed_shouldExecute() {
        UUID eventId = UUID.randomUUID();
        Runnable operation = mock(Runnable.class);
        when(inboxManager.tryConsume(eventId)).thenReturn(true);
        mockTransactionTemplateExecute();

        consumer.consume(eventId, operation);

        verify(inboxManager).tryConsume(eventId);
        verify(operation).run();
    }

    @Test
    @DisplayName("consume(UUID, Runnable): should NOT execute operation when event is already consumed")
    void consumeRunnable_whenEventAlreadyConsumed_shouldNotExecute() {
        UUID eventId = UUID.randomUUID();
        Runnable operation = mock(Runnable.class);
        when(inboxManager.tryConsume(eventId)).thenReturn(false);
        mockTransactionTemplateExecute();

        consumer.consume(eventId, operation);

        verify(operation, never()).run();
    }

    @Test
    @DisplayName("consume(UUID, Runnable): should throw NullPointerException when eventId or operation is null")
    void consumeRunnable_nullArguments_shouldThrowException() {
        assertThatThrownBy(() -> consumer.consume(null, mock(Runnable.class)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("eventId cannot be null");

        assertThatThrownBy(() -> consumer.consume(UUID.randomUUID(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("operation cannot be null");
    }

    @Test
    @DisplayName("consume(UUID, Runnable): should rethrow exception and log when transaction fails")
    void consumeRunnable_whenExceptionThrown_shouldRethrow() {
        UUID eventId = UUID.randomUUID();
        RuntimeException ex = new RuntimeException("DB Error");
        doThrow(ex).when(transactionTemplate).executeWithoutResult(any());

        assertThatThrownBy(() -> consumer.consume(eventId, mock(Runnable.class)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB Error");
    }

    @Test
    @DisplayName("consume(T, Extractor, Consumer): should execute operation when event is not consumed")
    void consumeSingle_whenEventNotConsumed_shouldExecute() {
        String message = "test-message";
        UUID eventId = UUID.randomUUID();
        EventIdExtractor<String> extractor = mock(EventIdExtractor.class);
        Consumer<String> operation = mock(Consumer.class);

        when(extractor.extract(message)).thenReturn(eventId);
        when(inboxManager.tryConsume(eventId)).thenReturn(true);
        mockTransactionTemplateExecute();

        consumer.consume(message, extractor, operation);

        verify(operation).accept(message);
    }

    @Test
    @DisplayName("consume(T, Extractor, Consumer): should NOT execute operation when event is already consumed")
    void consumeSingle_whenEventAlreadyConsumed_shouldNotExecute() {
        String message = "test-message";
        UUID eventId = UUID.randomUUID();
        EventIdExtractor<String> extractor = mock(EventIdExtractor.class);
        Consumer<String> operation = mock(Consumer.class);

        when(extractor.extract(message)).thenReturn(eventId);
        when(inboxManager.tryConsume(eventId)).thenReturn(false);
        mockTransactionTemplateExecute();

        consumer.consume(message, extractor, operation);

        verify(operation, never()).accept(any());
    }

    @Test
    @DisplayName("consume(T, Extractor, Consumer): should throw NullPointerException for missing arguments or extracted ID")
    void consumeSingle_nullArguments_shouldThrowException() {
        String message = "test-message";
        EventIdExtractor<String> extractor = mock(EventIdExtractor.class);
        Consumer<String> operation = mock(Consumer.class);

        assertThatThrownBy(() -> consumer.consume(null, extractor, operation))
                .isInstanceOf(NullPointerException.class).hasMessage("message cannot be null");

        assertThatThrownBy(() -> consumer.consume(message, null, operation))
                .isInstanceOf(NullPointerException.class).hasMessage("idExtractor cannot be null");

        assertThatThrownBy(() -> consumer.consume(message, extractor, null))
                .isInstanceOf(NullPointerException.class).hasMessage("operation cannot be null");

        when(extractor.extract(message)).thenReturn(null);
        assertThatThrownBy(() -> consumer.consume(message, extractor, operation))
                .isInstanceOf(NullPointerException.class).hasMessage("eventId cannot be null");
    }

    @Test
    @DisplayName("consume(T, Extractor, Consumer): should rethrow exception when transaction fails")
    void consumeSingle_whenExceptionThrown_shouldRethrow() {
        String message = "test-message";
        UUID eventId = UUID.randomUUID();
        EventIdExtractor<String> extractor = mock(EventIdExtractor.class);
        Consumer<String> operation = mock(Consumer.class);

        when(extractor.extract(message)).thenReturn(eventId);
        RuntimeException ex = new RuntimeException("DB Error");
        doThrow(ex).when(transactionTemplate).executeWithoutResult(any());

        assertThatThrownBy(() -> consumer.consume(message, extractor, operation))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB Error");
    }

    @Test
    @DisplayName("consume(Set, Consumer): should do nothing if set is null or empty")
    void consumeSet_nullOrEmpty_shouldReturnEarly() {
        Consumer<Set<UUID>> operation = mock(Consumer.class);

        consumer.consume((Set<UUID>) null, operation);
        consumer.consume(Collections.emptySet(), operation);

        verify(transactionTemplate, never()).executeWithoutResult(any());
        verify(operation, never()).accept(any());
    }

    @Test
    @DisplayName("consume(Set, Consumer): should throw NullPointerException if operation is null")
    void consumeSet_nullOperation_shouldThrowException() {
        assertThatThrownBy(() -> consumer.consume(Set.of(UUID.randomUUID()), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("operation cannot be null");
    }

    @Test
    @DisplayName("consume(Set, Consumer): should execute operation with all IDs if none are consumed")
    void consumeSet_noDuplicates_shouldExecuteWithAll() {
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
        Consumer<Set<UUID>> operation = mock(Consumer.class);

        when(inboxManager.tryConsumeAndGetDuplicates(ids)).thenReturn(Collections.emptySet());
        mockTransactionTemplateExecute();

        consumer.consume(ids, operation);

        verify(operation).accept(ids);
    }

    @Test
    @DisplayName("consume(Set, Consumer): should execute operation with only unconsumed IDs")
    void consumeSet_partialDuplicates_shouldExecuteWithValid() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2);
        Consumer<Set<UUID>> operation = mock(Consumer.class);

        when(inboxManager.tryConsumeAndGetDuplicates(ids)).thenReturn(Set.of(id1));
        mockTransactionTemplateExecute();

        consumer.consume(ids, operation);

        ArgumentCaptor<Set<UUID>> captor = ArgumentCaptor.forClass(Set.class);
        verify(operation).accept(captor.capture());
        assertThat(captor.getValue()).containsExactly(id2);
    }

    @Test
    @DisplayName("consume(Set, Consumer): should NOT execute operation if all IDs are duplicates")
    void consumeSet_allDuplicates_shouldNotExecute() {
        UUID id1 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1);
        Consumer<Set<UUID>> operation = mock(Consumer.class);

        when(inboxManager.tryConsumeAndGetDuplicates(ids)).thenReturn(Set.of(id1));
        mockTransactionTemplateExecute();

        consumer.consume(ids, operation);

        verify(operation, never()).accept(any());
    }

    @Test
    @DisplayName("consume(Set, Consumer): should rethrow exception when transaction fails")
    void consumeSet_whenExceptionThrown_shouldRethrow() {
        Set<UUID> ids = Set.of(UUID.randomUUID());
        Consumer<Set<UUID>> operation = mock(Consumer.class);

        RuntimeException ex = new RuntimeException("DB Error");
        doThrow(ex).when(transactionTemplate).executeWithoutResult(any());

        assertThatThrownBy(() -> consumer.consume(ids, operation))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("consume(List, Extractor, Consumer): should do nothing if list is null or empty")
    void consumeList_nullOrEmpty_shouldReturnEarly() {
        EventIdExtractor<String> extractor = mock(EventIdExtractor.class);
        Consumer<List<String>> operation = mock(Consumer.class);

        consumer.consume((List<String>) null, extractor, operation);
        consumer.consume(Collections.emptyList(), extractor, operation);

        verify(transactionTemplate, never()).executeWithoutResult(any());
    }

    @Test
    @DisplayName("consume(List, Extractor, Consumer): should throw NullPointerException if extractor or operation is null")
    void consumeList_nullArguments_shouldThrowException() {
        List<String> messages = List.of("msg");
        EventIdExtractor<String> extractor = mock(EventIdExtractor.class);
        Consumer<List<String>> operation = mock(Consumer.class);

        EventIdExtractor<String> nullExtractor = null;
        assertThatThrownBy(() -> consumer.consume(messages, nullExtractor, operation))
                .isInstanceOf(NullPointerException.class).hasMessage("idExtractor cannot be null");

        assertThatThrownBy(() -> consumer.consume(messages, extractor, null))
                .isInstanceOf(NullPointerException.class).hasMessage("operation cannot be null");
    }

    @Test
    @DisplayName("consume(List, Extractor, Consumer): should resolve duplicate IDs in the same batch by keeping the first occurrence")
    void consumeList_sameIdInBatch_shouldKeepExisting() {
        UUID sharedId = UUID.randomUUID();
        String msg1 = "msg1";
        String msg2 = "msg2";
        List<String> messages = List.of(msg1, msg2);

        EventIdExtractor<String> extractor = mock(EventIdExtractor.class);
        Consumer<List<String>> operation = mock(Consumer.class);

        when(extractor.extract(msg1)).thenReturn(sharedId);
        when(extractor.extract(msg2)).thenReturn(sharedId);

        when(inboxManager.tryConsumeAndGetDuplicates(Set.of(sharedId))).thenReturn(Collections.emptySet());
        mockTransactionTemplateExecute();

        consumer.consume(messages, extractor, operation);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(operation).accept(captor.capture());

        assertThat(captor.getValue()).containsExactly("msg1");
    }

    @Test
    @DisplayName("consume(List, Extractor, Consumer): should execute operation with unconsumed messages only")
    void consumeList_partialDuplicates_shouldExecuteWithValid() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<String> messages = List.of("msg1", "msg2");

        EventIdExtractor<String> extractor = mock(EventIdExtractor.class);
        Consumer<List<String>> operation = mock(Consumer.class);

        when(extractor.extract("msg1")).thenReturn(id1);
        when(extractor.extract("msg2")).thenReturn(id2);

        when(inboxManager.tryConsumeAndGetDuplicates(Set.of(id1, id2))).thenReturn(Set.of(id1));
        mockTransactionTemplateExecute();

        consumer.consume(messages, extractor, operation);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(operation).accept(captor.capture());
        assertThat(captor.getValue()).containsExactly("msg2");
    }

    @Test
    @DisplayName("consume(List, Extractor, Consumer): should NOT execute operation if all messages are duplicates")
    void consumeList_allDuplicates_shouldNotExecute() {
        UUID id1 = UUID.randomUUID();
        List<String> messages = List.of("msg1");

        EventIdExtractor<String> extractor = mock(EventIdExtractor.class);
        Consumer<List<String>> operation = mock(Consumer.class);

        when(extractor.extract("msg1")).thenReturn(id1);
        when(inboxManager.tryConsumeAndGetDuplicates(Set.of(id1))).thenReturn(Set.of(id1));
        mockTransactionTemplateExecute();

        consumer.consume(messages, extractor, operation);

        verify(operation, never()).accept(any());
    }

    @Test
    @DisplayName("consume(List, Extractor, Consumer): should rethrow exception when transaction fails")
    void consumeList_whenExceptionThrown_shouldRethrow() {
        UUID id1 = UUID.randomUUID();
        List<String> messages = List.of("msg1");

        EventIdExtractor<String> extractor = mock(EventIdExtractor.class);
        Consumer<List<String>> operation = mock(Consumer.class);

        when(extractor.extract("msg1")).thenReturn(id1);

        RuntimeException ex = new RuntimeException("DB Error");
        doThrow(ex).when(transactionTemplate).executeWithoutResult(any());

        assertThatThrownBy(() -> consumer.consume(messages, extractor, operation))
                .isInstanceOf(RuntimeException.class);
    }
}