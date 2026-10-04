package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTask;
import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxPropertiesHolder;
import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Function;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxDlqTransferSchedulerUnitTests {

    @Mock
    OutboxPropertiesHolder.DlqPropertiesHolder.TransferPropertiesHolder transferProperties;

    @Mock
    OnceboxScheduleStrategy strategy;

    @Mock
    Function<Integer, Integer> transferApplier;

    @Mock
    ContinuableTaskDecorator decorator;

    OutboxDlqTransferScheduler tested;

    @BeforeEach
    void setUp() {
        tested = new OutboxDlqTransferScheduler(
                () -> transferProperties,
                strategy,
                transferApplier,
                ContinuableTaskDecorator.identity(),
                OutboxDlqTransferScheduler.LogMessage.transferTo()
        );
    }

    @Test
    @DisplayName("UT constructor when propertiesSupplier is null should throw NullPointerException")
    void constructor_whenPropertiesSupplierIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxDlqTransferScheduler(null, strategy, transferApplier, decorator, OutboxDlqTransferScheduler.LogMessage.transferTo()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("propertiesSupplier cannot be null");
    }

    @Test
    @DisplayName("UT constructor when properties is null should throw NullPointerException")
    void constructor_whenPropertiesIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxDlqTransferScheduler(() -> null, strategy, transferApplier, decorator, OutboxDlqTransferScheduler.LogMessage.transferTo()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("properties cannot be null");
    }

    @Test
    @DisplayName("UT constructor when strategy is null should throw NullPointerException")
    void constructor_whenStrategyIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxDlqTransferScheduler(() -> transferProperties, null, transferApplier, decorator, OutboxDlqTransferScheduler.LogMessage.transferTo()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("scheduleStrategy cannot be null");
    }

    @Test
    @DisplayName("UT constructor when transferApplier is null should throw NullPointerException")
    void constructor_whenTransferApplierIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxDlqTransferScheduler(() -> transferProperties, strategy, null, decorator, OutboxDlqTransferScheduler.LogMessage.transferTo()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("transferApplier cannot be null");
    }

    @Test
    @DisplayName("UT constructor when continuableTaskDecorator is null should throw NullPointerException")
    void constructor_whenContinuableTaskDecoratorIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxDlqTransferScheduler(() -> transferProperties, strategy, transferApplier, null, OutboxDlqTransferScheduler.LogMessage.transferTo()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("taskDecorator cannot be null");
    }

    @Test
    @DisplayName("UT constructor when logMessage is null should throw NullPointerException")
    void constructor_whenLogMessageIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxDlqTransferScheduler(() -> transferProperties, strategy, transferApplier, decorator, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("logMessage cannot be null");
    }

    private ContinuableTask captureTask() {
        ArgumentCaptor<ContinuableTask> captor = ArgumentCaptor.forClass(ContinuableTask.class);
        verify(strategy).scheduleExecution(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("UT schedule() should delegate to strategy")
    void schedule_shouldDelegateToStrategy() {
        tested.schedule();

        verify(strategy).scheduleExecution(any());
    }

    @Test
    @DisplayName("UT schedule() should delegate to strategy exactly once")
    void schedule_shouldDelegateToStrategyExactlyOnce() {
        tested.schedule();

        verify(strategy, times(1)).scheduleExecution(any());
        verifyNoMoreInteractions(strategy);
    }

    @Test
    @DisplayName("UT schedule() when transferred count equals batch size, task should return true")
    void schedule_whenTransferredCountEqualsBatchSize_taskShouldReturnTrue() {
        int batchSize = 50;
        when(transferProperties.getBatchSize()).thenReturn(batchSize);
        when(transferApplier.apply(batchSize)).thenReturn(batchSize);

        tested.schedule();
        boolean result = captureTask().run();

        assertTrue(result);
    }

    @Test
    @DisplayName("UT schedule() when transferred count less than batch size, task should return false")
    void schedule_whenTransferredCountLessThanBatchSize_taskShouldReturnFalse() {
        int batchSize = 50;
        when(transferProperties.getBatchSize()).thenReturn(batchSize);
        when(transferApplier.apply(batchSize)).thenReturn(batchSize - 1);

        tested.schedule();
        boolean result = captureTask().run();

        assertFalse(result);
    }

    @Test
    @DisplayName("UT schedule() when transferred count is zero, task should return false")
    void schedule_whenTransferredCountIsZero_taskShouldReturnFalse() {
        int batchSize = 50;
        when(transferProperties.getBatchSize()).thenReturn(batchSize);
        when(transferApplier.apply(batchSize)).thenReturn(0);

        tested.schedule();
        boolean result = captureTask().run();

        assertFalse(result);
    }

    @Test
    @DisplayName("UT schedule() should pass batchSize from properties to transferApplier")
    void schedule_shouldPassBatchSizeFromPropertiesToApplier() {
        int batchSize = 100;
        when(transferProperties.getBatchSize()).thenReturn(batchSize);
        when(transferApplier.apply(batchSize)).thenReturn(0);

        tested.schedule();
        captureTask().run();

        verify(transferApplier).apply(batchSize);
    }

    @Test
    @DisplayName("UT schedule() when transferApplier throws, task should return false and not rethrow")
    void schedule_whenApplierThrows_taskShouldReturnFalseAndNotRethrow() {
        int batchSize = 50;
        when(transferProperties.getBatchSize()).thenReturn(batchSize);
        when(transferApplier.apply(batchSize)).thenThrow(new RuntimeException("DB error"));

        tested.schedule();
        boolean result = assertDoesNotThrow(() -> captureTask().run());

        assertFalse(result);
    }

    @Test
    @DisplayName("UT schedule() should apply decorator to task before passing to strategy")
    void schedule_shouldApplyDecoratorToTask() {
        tested = new OutboxDlqTransferScheduler(
                () -> transferProperties,
                strategy,
                transferApplier,
                decorator,
                OutboxDlqTransferScheduler.LogMessage.transferTo()
        );
        ContinuableTask decoratedTask = mock(ContinuableTask.class);
        when(decorator.decorate(any())).thenReturn(decoratedTask);

        tested.schedule();

        verify(decorator).decorate(any());
        verify(strategy).scheduleExecution(decoratedTask);
    }

    @Test
    @DisplayName("UT LogMessage.transferTo() should return non-null messages")
    void logMessage_transferTo_shouldReturnNonNullMessages() {
        OutboxDlqTransferScheduler.LogMessage message = OutboxDlqTransferScheduler.LogMessage.transferTo();

        assertNotNull(message.onStart());
        assertNotNull(message.onException());
    }

    @Test
    @DisplayName("UT LogMessage.transferFrom() should return non-null messages")
    void logMessage_transferFrom_shouldReturnNonNullMessages() {
        OutboxDlqTransferScheduler.LogMessage message = OutboxDlqTransferScheduler.LogMessage.transferFrom();

        assertNotNull(message.onStart());
        assertNotNull(message.onException());
    }

    @Test
    @DisplayName("UT LogMessage.transferTo() and transferFrom() should have different messages")
    void logMessage_transferToAndTransferFrom_shouldHaveDifferentMessages() {
        OutboxDlqTransferScheduler.LogMessage transferTo   = OutboxDlqTransferScheduler.LogMessage.transferTo();
        OutboxDlqTransferScheduler.LogMessage transferFrom = OutboxDlqTransferScheduler.LogMessage.transferFrom();

        assertNotEquals(transferTo.onStart(),     transferFrom.onStart());
        assertNotEquals(transferTo.onException(), transferFrom.onException());
    }
}