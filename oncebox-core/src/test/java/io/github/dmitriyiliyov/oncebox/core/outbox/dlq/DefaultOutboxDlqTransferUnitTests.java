package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DefaultOutboxDlqTransferUnitTests {

    @Mock
    TransactionTemplate transactionTemplate;

    @Mock
    OutboxManager manager;

    @Mock
    OutboxDlqManager dlqManager;

    @Mock
    OutboxDlqHandler handler;

    @Mock
    OutboxDlqEventMapper eventMapper;

    @InjectMocks
    DefaultOutboxDlqTransfer tested;

    @BeforeEach
    void setupTx() {
        lenient().doAnswer(invocation -> {
            Consumer<?> action = invocation.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        lenient().doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        }).when(transactionTemplate).execute(any());
    }

    @Test
    @DisplayName("UT constructor when transactionTemplate is null should throw NullPointerException")
    void constructor_whenTransactionTemplateIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultOutboxDlqTransfer(null, manager, dlqManager, eventMapper, handler))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("transactionTemplate cannot be null");
    }

    @Test
    @DisplayName("UT constructor when manager is null should throw NullPointerException")
    void constructor_whenManagerIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultOutboxDlqTransfer(transactionTemplate, null, dlqManager, eventMapper, handler))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("manager cannot be null");
    }

    @Test
    @DisplayName("UT constructor when dlqManager is null should throw NullPointerException")
    void constructor_whenDlqManagerIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultOutboxDlqTransfer(transactionTemplate, manager, null, eventMapper, handler))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("dlqManager cannot be null");
    }

    @Test
    @DisplayName("UT constructor when eventMapper is null should throw NullPointerException")
    void constructor_whenEventMapperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultOutboxDlqTransfer(transactionTemplate, manager, dlqManager, null, handler))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventMapper cannot be null");
    }

    @Test
    @DisplayName("UT constructor when handler is null should throw NullPointerException")
    void constructor_whenHandlerIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new DefaultOutboxDlqTransfer(transactionTemplate, manager, dlqManager, eventMapper, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("handler cannot be null");
    }

    @Test
    @DisplayName("UT transferToDlq() when events is empty, should not transfer and not call handler")
    void transferToDlq_whenEventsIsEmpty_shouldNotTransferAndNotCallHandler() {
        // given
        int batchSize = 50;
        when(manager.loadBatch(EventStatus.FAILED, batchSize)).thenReturn(List.of());

        // when
        tested.transferToDlq(batchSize);

        // then
        verify(manager).loadBatch(EventStatus.FAILED, batchSize);
        verify(eventMapper, never()).toDlqEvents(anyList());
        verify(dlqManager, never()).saveBatch(anyList());
        verify(manager, never()).deleteBatch(anySet());
        verify(handler, never()).handle(anyList());
    }

    @Test
    @DisplayName("UT transferToDlq() when events exist, should map, transfer and call handler")
    void transferToDlq_whenEventsExist_shouldMapTransferAndCallHandler() {
        // given
        int batchSize = 50;
        UUID eventId = UUID.randomUUID();

        OutboxEvent event = new OutboxEvent(
                eventId, EventStatus.FAILED, "type", "type", "{}", 1,
                Instant.now(), Instant.now(), Instant.now()
        );

        OutboxDlqEvent dlqEvent = new OutboxDlqEvent(
                eventId, EventStatus.FAILED, "type", "type", "{}", 1,
                Instant.now(), Instant.now(), Instant.now(), DlqStatus.MOVED, Instant.now()
        );

        when(manager.loadBatch(EventStatus.FAILED, batchSize)).thenReturn(List.of(event));
        when(eventMapper.toDlqEvents(List.of(event))).thenReturn(List.of(dlqEvent));

        // when
        tested.transferToDlq(batchSize);

        // then
        verify(manager).loadBatch(EventStatus.FAILED, batchSize);
        verify(eventMapper).toDlqEvents(List.of(event));
        verify(dlqManager).saveBatch(List.of(dlqEvent));
        verify(manager).deleteBatch(Set.of(eventId));
        verify(handler).handle(List.of(event));
    }

    @Test
    @DisplayName("UT transferToDlq() should pass mapped dlq events to saveBatch")
    void transferToDlq_shouldPassMappedDlqEventsToSaveBatch() {
        // given
        int batchSize = 50;

        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), EventStatus.FAILED, "t", "t", "{}", 1,
                Instant.now(), Instant.now(), Instant.now()
        );

        OutboxDlqEvent dlqEvent = new OutboxDlqEvent(
                event.getId(), EventStatus.FAILED, "t", "t", "{}", 1,
                Instant.now(), Instant.now(), Instant.now(), DlqStatus.MOVED, Instant.now()
        );

        when(manager.loadBatch(any(EventStatus.class), anyInt())).thenReturn(List.of(event));
        when(eventMapper.toDlqEvents(anyList())).thenReturn(List.of(dlqEvent));

        // when
        tested.transferToDlq(batchSize);

        // then
        verify(dlqManager).saveBatch(List.of(dlqEvent));
    }

    @Test
    @DisplayName("UT transferToDlq() when exception in transaction, should not call handler")
    void transferToDlq_whenExceptionInTransaction_shouldNotCallHandler() {
        // given
        int batchSize = 50;

        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), EventStatus.FAILED, "OrderCreated", "com.example.OrderCreatedEvent",
                "{\"orderId\":\"123\"}", 3, Instant.now(), Instant.now(), Instant.now()
        );

        doAnswer(invocation -> {
            Consumer<?> action = invocation.getArgument(0);
            when(manager.loadBatch(EventStatus.FAILED, batchSize)).thenReturn(List.of(event));
            when(eventMapper.toDlqEvents(anyList())).thenReturn(List.of());
            doThrow(new RuntimeException("Database error")).when(dlqManager).saveBatch(anyList());
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        // when + then
        assertThatThrownBy(() -> tested.transferToDlq(batchSize))
                .isInstanceOf(RuntimeException.class);

        verify(manager).loadBatch(EventStatus.FAILED, batchSize);
        verify(dlqManager).saveBatch(anyList());
        verify(manager, never()).deleteBatch(anySet());
        verify(handler, never()).handle(anyList());
    }

    @Test
    @DisplayName("UT transferToDlq() when handler throws exception, should not affect transaction")
    void transferToDlq_whenHandlerThrows_shouldNotAffectTransaction() {
        // given
        int batchSize = 50;

        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), EventStatus.FAILED, "t", "t", "{}", 1,
                Instant.now(), Instant.now(), Instant.now()
        );

        when(manager.loadBatch(any(EventStatus.class), anyInt())).thenReturn(List.of(event));
        when(eventMapper.toDlqEvents(anyList())).thenReturn(List.of());
        doThrow(new RuntimeException()).when(handler).handle(anyList());

        // when + then
        assertThatCode(() -> tested.transferToDlq(batchSize)).doesNotThrowAnyException();

        verify(eventMapper).toDlqEvents(anyList());
        verify(dlqManager).saveBatch(anyList());
        verify(manager).deleteBatch(anySet());
        verify(handler).handle(List.of(event));
    }

    @Test
    @DisplayName("UT transferFromDlq() when dlq events is null, should not transfer")
    void transferFromDlq_whenDlqEventsIsNull_shouldNotTransferFrom() {
        // given
        int batchSize = 50;
        when(dlqManager.loadAndLockBatch(DlqStatus.TO_RETRY, batchSize)).thenReturn(null);

        // when
        tested.transferFromDlq(batchSize);

        // then
        verify(dlqManager).loadAndLockBatch(DlqStatus.TO_RETRY, batchSize);
        verify(eventMapper, never()).toOutboxEvents(anyList());
        verify(manager, never()).saveBatchWithCreatedAt(anyList());
        verify(dlqManager, never()).deleteBatch(anySet());
    }

    @Test
    @DisplayName("UT transferFromDlq() when dlq events is empty, should not transfer")
    void transferFromDlq_whenDlqEventsIsEmpty_shouldNotTransferFrom() {
        // given
        int batchSize = 50;
        when(dlqManager.loadAndLockBatch(DlqStatus.TO_RETRY, batchSize)).thenReturn(List.of());

        // when
        tested.transferFromDlq(batchSize);

        // then
        verify(dlqManager).loadAndLockBatch(DlqStatus.TO_RETRY, batchSize);
        verify(eventMapper, never()).toOutboxEvents(anyList());
        verify(manager, never()).saveBatchWithCreatedAt(anyList());
        verify(dlqManager, never()).deleteBatch(anySet());
    }

    @Test
    @DisplayName("UT transferFromDlq() when dlq events exist, should map, save and delete")
    void transferFromDlq_whenDlqEventsExist_shouldMapSaveAndDelete() {
        // given
        int batchSize = 50;
        UUID eventId = UUID.randomUUID();

        OutboxDlqEvent dlqEvent = new OutboxDlqEvent(
                eventId, EventStatus.FAILED, "type", "type", "{}", 5,
                Instant.now(), Instant.now(), Instant.now(), DlqStatus.TO_RETRY, Instant.now()
        );

        OutboxEvent outboxEvent = new OutboxEvent(
                eventId, EventStatus.PENDING, "type", "type", "{}", -1,
                Instant.now(), Instant.now(), Instant.now()
        );

        when(dlqManager.loadAndLockBatch(DlqStatus.TO_RETRY, batchSize)).thenReturn(List.of(dlqEvent));
        when(eventMapper.toOutboxEvents(List.of(dlqEvent))).thenReturn(List.of(outboxEvent));

        // when
        tested.transferFromDlq(batchSize);

        // then
        verify(eventMapper).toOutboxEvents(List.of(dlqEvent));
        verify(manager).saveBatchWithCreatedAt(List.of(outboxEvent));
        verify(dlqManager).deleteBatch(Set.of(eventId));
    }

    @Test
    @DisplayName("UT transferFromDlq() should pass mapped outbox events to saveBatchWithCreatedAt")
    void transferFromDlq_shouldPassMappedOutboxEventsToSaveBatch() {
        // given
        int batchSize = 50;

        OutboxDlqEvent dlqEvent = new OutboxDlqEvent(
                UUID.randomUUID(), EventStatus.FAILED, "t", "t", "{}", 1,
                Instant.now(), Instant.now(), Instant.now(), DlqStatus.TO_RETRY, Instant.now()
        );

        OutboxEvent outboxEvent = new OutboxEvent(
                dlqEvent.getId(), EventStatus.PENDING, "t", "t", "{}", -1,
                Instant.now(), Instant.now(), Instant.now()
        );

        when(dlqManager.loadAndLockBatch(any(), anyInt())).thenReturn(List.of(dlqEvent));
        when(eventMapper.toOutboxEvents(anyList())).thenReturn(List.of(outboxEvent));

        // when
        tested.transferFromDlq(batchSize);

        // then
        verify(manager).saveBatchWithCreatedAt(List.of(outboxEvent));
    }

    @Test
    @DisplayName("UT transferFromDlq() when exception occurs, should not delete from dlq")
    void transferFromDlq_whenExceptionOccurs_shouldNotDeleteFromDlq() {
        // given
        int batchSize = 50;

        OutboxDlqEvent dlqEvent = new OutboxDlqEvent(
                UUID.randomUUID(), EventStatus.FAILED, "type", "type", "{}", 1,
                Instant.now(), Instant.now(), Instant.now(), DlqStatus.TO_RETRY, Instant.now()
        );

        when(dlqManager.loadAndLockBatch(any(), anyInt())).thenReturn(List.of(dlqEvent));
        when(eventMapper.toOutboxEvents(anyList())).thenReturn(List.of());
        doThrow(new RuntimeException()).when(manager).saveBatchWithCreatedAt(anyList());

        // when + then
        assertThatThrownBy(() -> tested.transferFromDlq(batchSize))
                .isInstanceOf(RuntimeException.class);

        verify(manager).saveBatchWithCreatedAt(anyList());
        verify(dlqManager, never()).deleteBatch(anySet());
    }

    @Test
    @DisplayName("UT transferFromDlq() when transaction returns null, should return 0")
    void transferFromDlq_whenTransactionReturnsNull_shouldReturnZero() {
        // given
        lenient().doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return null;
        }).when(transactionTemplate).execute(any());

        // when
        int result = tested.transferFromDlq(50);

        // then
        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("UT transferFromDlq() when transaction returns count, should return it")
    void transferFromDlq_whenTransactionReturnsCount_shouldReturnIt() {
        // given
        lenient().doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return 3;
        }).when(transactionTemplate).execute(any());

        // when
        int result = tested.transferFromDlq(50);

        // then
        assertThat(result).isEqualTo(3);
    }
}