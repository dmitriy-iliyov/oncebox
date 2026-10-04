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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OutboxPollingSchedulerUnitTests {

    @Mock
    OutboxPropertiesHolder.EventPropertiesHolder properties;

    @Mock
    OnceboxScheduleStrategy strategy;

    @Mock
    OutboxProcessor processor;

    @Mock
    ContinuableTaskDecorator decorator;

    @InjectMocks
    OutboxPollingScheduler tested;

    @Test
    @DisplayName("UT constructor when properties is null should throw NullPointerException")
    void constructor_whenPropertiesIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxPollingScheduler(null, strategy, processor, decorator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("properties cannot be null");
    }

    @Test
    @DisplayName("UT constructor when strategy is null should throw NullPointerException")
    void constructor_whenStrategyIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxPollingScheduler(properties, null, processor, decorator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("scheduleStrategy cannot be null");
    }

    @Test
    @DisplayName("UT constructor when processor is null should throw NullPointerException")
    void constructor_whenProcessorIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxPollingScheduler(properties, strategy, null, decorator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("processor cannot be null");
    }

    @Test
    @DisplayName("UT constructor when decorator is null should throw NullPointerException")
    void constructor_whenDecoratorIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxPollingScheduler(properties, strategy, processor, null))
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
        // when
        tested.schedule();

        // then
        verify(strategy).scheduleExecution(any());
    }

    @Test
    @DisplayName("UT schedule() when processed count equals batch size, continuable should return true")
    void schedule_whenProcessedCountEqualsBatchSize_continuableShouldReturnTrue() {
        // given
        int batchSize = 50;
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(processor.process(properties)).thenReturn(batchSize);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("UT schedule() when processed count less than batch size, continuable should return false")
    void schedule_whenProcessedCountLessThanBatchSize_continuableShouldReturnFalse() {
        // given
        int batchSize = 50;
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(processor.process(properties)).thenReturn(batchSize - 1);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("UT schedule() when processed count is zero, continuable should return false")
    void schedule_whenProcessedCountIsZero_continuableShouldReturnFalse() {
        // given
        int batchSize = 50;
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(processor.process(properties)).thenReturn(0);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("UT schedule() should pass properties to processor")
    void schedule_shouldPassPropertiesToProcessor() {
        // given
        when(properties.getBatchSize()).thenReturn(50);
        when(processor.process(properties)).thenReturn(0);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());

        // when
        tested.schedule();
        captureAndRun();

        // then
        verify(processor).process(properties);
    }

    @Test
    @DisplayName("UT schedule() when processor throws exception, continuable should return false and not rethrow")
    void schedule_whenProcessorThrows_continuableShouldReturnFalseAndNotRethrow() {
        // given
        when(properties.getBatchSize()).thenReturn(50);
        when(processor.process(properties)).thenThrow(new RuntimeException("DB error"));
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());

        // when
        tested.schedule();
        boolean result = assertDoesNotThrow(this::captureAndRun);

        // then
        assertFalse(result);
    }
}