package io.github.dmitriyiliyov.oncebox.core.outbox;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTask;
import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OutboxRecoverySchedulerUnitTests {

    @Mock
    OutboxPropertiesHolder.StuckRecoveryPropertiesHolder properties;

    @Mock
    OnceboxScheduleStrategy strategy;

    @Mock
    OutboxManager manager;

    @Mock
    ContinuableTaskDecorator decorator;

    @InjectMocks
    OutboxRecoveryScheduler tested;

    @Test
    @DisplayName("UT constructor when properties is null should throw NullPointerException")
    void constructor_whenPropertiesIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxRecoveryScheduler(null, strategy, manager, decorator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("properties cannot be null");
    }

    @Test
    @DisplayName("UT constructor when strategy is null should throw NullPointerException")
    void constructor_whenStrategyIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxRecoveryScheduler(properties, null, manager, decorator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("scheduleStrategy cannot be null");
    }

    @Test
    @DisplayName("UT constructor when manager is null should throw NullPointerException")
    void constructor_whenManagerIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxRecoveryScheduler(properties, strategy, null, decorator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("manager cannot be null");
    }

    @Test
    @DisplayName("UT constructor when decorator is null should throw NullPointerException")
    void constructor_whenDecoratorIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxRecoveryScheduler(properties, strategy, manager, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("taskDecorator cannot be null");
    }

    private boolean captureAndRun() {
        ArgumentCaptor<ContinuableTask> captor = ArgumentCaptor.forClass(ContinuableTask.class);
        verify(strategy).scheduleExecution(captor.capture());
        return captor.getValue().run();
    }

    @Test
    @DisplayName("UT schedule() should delegate execution to strategy")
    void schedule_shouldDelegateToStrategy() {
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());

        // when
        tested.schedule();

        // then
        verify(strategy).scheduleExecution(any());
    }

    @Test
    @DisplayName("UT schedule() when recovered count equals batch size, continuable should return true")
    void schedule_whenRecoveredCountEqualsBatchSize_continuableShouldReturnTrue() {
        // given
        int batchSize = 50;
        Duration maxProcessingTime = Duration.ofMinutes(5);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getMaxBatchProcessingTime()).thenReturn(maxProcessingTime);
        when(manager.recoverStuckBatch(maxProcessingTime, batchSize)).thenReturn(batchSize);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("UT schedule() when recovered count less than batch size, continuable should return false")
    void schedule_whenRecoveredCountLessThanBatchSize_continuableShouldReturnFalse() {
        // given
        int batchSize = 50;
        Duration maxProcessingTime = Duration.ofMinutes(5);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getMaxBatchProcessingTime()).thenReturn(maxProcessingTime);
        when(manager.recoverStuckBatch(maxProcessingTime, batchSize)).thenReturn(batchSize - 1);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("UT schedule() when recovered count is zero, continuable should return false")
    void schedule_whenRecoveredCountIsZero_continuableShouldReturnFalse() {
        // given
        int batchSize = 50;
        Duration maxProcessingTime = Duration.ofMinutes(5);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getMaxBatchProcessingTime()).thenReturn(maxProcessingTime);
        when(manager.recoverStuckBatch(maxProcessingTime, batchSize)).thenReturn(0);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("UT schedule() should pass maxBatchProcessingTime and batchSize to manager")
    void schedule_shouldPassCorrectParamsToManager() {
        // given
        int batchSize = 100;
        Duration maxProcessingTime = Duration.ofMinutes(10);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getMaxBatchProcessingTime()).thenReturn(maxProcessingTime);
        when(manager.recoverStuckBatch(any(), anyInt())).thenReturn(0);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());

        // when
        tested.schedule();
        captureAndRun();

        // then
        verify(manager).recoverStuckBatch(maxProcessingTime, batchSize);
    }

    @Test
    @DisplayName("UT schedule() when manager throws exception, continuable should return false and not rethrow")
    void schedule_whenManagerThrows_continuableShouldReturnFalseAndNotRethrow() {
        // given
        int batchSize = 50;
        Duration maxProcessingTime = Duration.ofMinutes(5);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getMaxBatchProcessingTime()).thenReturn(maxProcessingTime);
        when(manager.recoverStuckBatch(maxProcessingTime, batchSize))
                .thenThrow(new RuntimeException("DB error"));
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());

        // when
        tested.schedule();
        boolean result = assertDoesNotThrow(this::captureAndRun);

        // then
        assertFalse(result);
    }
}