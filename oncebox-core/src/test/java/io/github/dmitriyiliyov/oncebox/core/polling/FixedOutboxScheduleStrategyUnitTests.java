package io.github.dmitriyiliyov.oncebox.core.polling;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FixedOutboxScheduleStrategyUnitTests {

    @Mock
    FixedPollingPropertiesHolder properties;

    @Mock
    ScheduledExecutorService executor;

    @Mock
    ContinuableTask task;

    @Mock
    OutboxScheduleStrategyListener listener;

    FixedOutboxScheduleStrategy tested;

    @BeforeEach
    void setUp() {
        tested = new FixedOutboxScheduleStrategy(properties, executor, listener);
    }

    @Test
    @DisplayName("UT constructor when properties is null should throw NullPointerException")
    void constructor_whenPropertiesIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new FixedOutboxScheduleStrategy(null, executor, listener))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("properties cannot be null");
    }

    @Test
    @DisplayName("UT constructor when executor is null should throw NullPointerException")
    void constructor_whenExecutorIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new FixedOutboxScheduleStrategy(properties, null, listener))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("executor cannot be null");
    }

    @Test
    @DisplayName("UT constructor when listener is null should throw NullPointerException")
    void constructor_whenListenerIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new FixedOutboxScheduleStrategy(properties, executor, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("listener cannot be null");
    }

    private Runnable captureScheduledRunnable() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).scheduleWithFixedDelay(captor.capture(), anyLong(), anyLong(), any());
        return captor.getValue();
    }

    @Test
    @DisplayName("UT scheduleExecution() should call scheduleWithFixedDelay on executor")
    void scheduleExecution_shouldCallScheduleWithFixedDelay() {
        // given
        when(properties.getInitialDelay()).thenReturn(Duration.ofMillis(100));
        when(properties.getFixedDelay()).thenReturn(Duration.ofMillis(500));

        // when
        tested.scheduleExecution(task);

        // then
        verify(executor).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        verify(listener).onDelayChanged(nullable(Long.class));
    }

    @Test
    @DisplayName("UT scheduleExecution() should pass initialDelay and fixedDelay in milliseconds to executor")
    void scheduleExecution_shouldPassCorrectDelaysToExecutor() {
        // given
        Duration initialDelay = Duration.ofSeconds(2);
        Duration fixedDelay = Duration.ofSeconds(5);
        when(properties.getInitialDelay()).thenReturn(initialDelay);
        when(properties.getFixedDelay()).thenReturn(fixedDelay);

        // when
        tested.scheduleExecution(task);

        // then
        verify(executor).scheduleWithFixedDelay(
                any(Runnable.class),
                eq(initialDelay.toMillis()),
                eq(fixedDelay.toMillis()),
                eq(TimeUnit.MILLISECONDS)
        );
        verify(listener).onDelayChanged(nullable(Long.class));
    }

    @Test
    @DisplayName("UT scheduleExecution() when runnable is invoked, should call task.run()")
    void scheduleExecution_whenRunnableInvoked_shouldCallTaskRun() {
        // given
        when(properties.getInitialDelay()).thenReturn(Duration.ofMillis(0));
        when(properties.getFixedDelay()).thenReturn(Duration.ofMillis(500));

        // when
        tested.scheduleExecution(task);
        captureScheduledRunnable().run();

        // then
        verify(task).run();
        verify(listener).onDelayChanged(nullable(Long.class));
    }

    @Test
    @DisplayName("UT scheduleExecution() when task throws exception, runnable should not rethrow")
    void scheduleExecution_whenTaskThrows_runnableShouldNotRethrow() {
        // given
        when(properties.getInitialDelay()).thenReturn(Duration.ofMillis(0));
        when(properties.getFixedDelay()).thenReturn(Duration.ofMillis(500));
        when(task.run()).thenThrow(new RuntimeException("task error"));

        // when
        tested.scheduleExecution(task);
        Runnable runnable = captureScheduledRunnable();

        // then
        assertDoesNotThrow(runnable::run);
        verify(listener).onDelayChanged(nullable(Long.class));
    }

    @Test
    @DisplayName("UT scheduleExecution() when task throws Error, runnable should not rethrow")
    void scheduleExecution_whenTaskThrowsError_runnableShouldNotRethrow() {
        // given
        when(properties.getInitialDelay()).thenReturn(Duration.ofMillis(0));
        when(properties.getFixedDelay()).thenReturn(Duration.ofMillis(500));
        when(task.run()).thenThrow(new Error("fatal error"));

        // when
        tested.scheduleExecution(task);
        Runnable runnable = captureScheduledRunnable();

        // then
        assertDoesNotThrow(runnable::run);
        verify(listener).onDelayChanged(nullable(Long.class));
    }

    @Test
    @DisplayName("UT scheduleExecution() should use MILLISECONDS as time unit")
    void scheduleExecution_shouldUseMillisecondsTimeUnit() {
        // given
        when(properties.getInitialDelay()).thenReturn(Duration.ofMillis(100));
        when(properties.getFixedDelay()).thenReturn(Duration.ofMillis(500));

        // when
        tested.scheduleExecution(task);

        // then
        verify(executor).scheduleWithFixedDelay(any(), anyLong(), anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(listener).onDelayChanged(nullable(Long.class));
    }

    @Test
    @DisplayName("UT scheduleExecution() when listener fails on the initial delay should still schedule the task")
    void scheduleExecution_whenListenerFailsOnInitialDelay_shouldStillScheduleTask() {
        // given
        givenDelays();
        doThrow(LISTENER_FAILURE).when(listener).onDelayChanged(anyLong());

        // when / then
        assertDoesNotThrow(() -> tested.scheduleExecution(task));
        verify(executor).scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(500L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("UT scheduleExecution() when listener fails before the task runs should still run the task without rethrowing")
    void scheduleExecution_whenListenerFailsOnStart_shouldStillRunTaskWithoutRethrowing() {
        // given
        givenDelays();
        doThrow(LISTENER_FAILURE).when(listener).onExecutionStarted();
        tested.scheduleExecution(task);
        Runnable runnable = captureScheduledRunnable();

        // when / then
        assertDoesNotThrow(runnable::run);
        verify(task).run();
    }

    @Test
    @DisplayName("UT scheduleExecution() when listener fails on a success should not report the run as failed")
    void scheduleExecution_whenListenerFailsOnSuccess_shouldNotReportRunAsFailed() {
        // given
        givenDelays();
        doThrow(LISTENER_FAILURE).when(listener).onExecutionSucceeded();
        tested.scheduleExecution(task);
        Runnable runnable = captureScheduledRunnable();

        // when / then
        assertDoesNotThrow(runnable::run);
        verify(listener, never()).onExecutionFailed();
    }

    @Test
    @DisplayName("UT scheduleExecution() when listener fails on a failed run should not rethrow, which would cancel the periodic task")
    void scheduleExecution_whenListenerFailsOnFailure_shouldNotRethrow() {
        // given
        givenDelays();
        when(task.run()).thenThrow(new RuntimeException("task error"));
        doThrow(LISTENER_FAILURE).when(listener).onExecutionFailed();
        tested.scheduleExecution(task);
        Runnable runnable = captureScheduledRunnable();

        // when / then
        assertDoesNotThrow(runnable::run);
        verify(listener).onExecutionFailed();
    }

    private static final RuntimeException LISTENER_FAILURE = new IllegalStateException("meter registry closed");

    private void givenDelays() {
        when(properties.getInitialDelay()).thenReturn(Duration.ofMillis(0));
        when(properties.getFixedDelay()).thenReturn(Duration.ofMillis(500));
    }
}
