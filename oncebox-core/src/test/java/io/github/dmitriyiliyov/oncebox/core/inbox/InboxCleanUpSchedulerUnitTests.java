package io.github.dmitriyiliyov.oncebox.core.inbox;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTask;
import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.oncebox.core.OnceboxPropertiesHolder;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.OnceboxJob;
import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InboxCleanUpSchedulerUnitTests {

    @Mock
    OnceboxPropertiesHolder.CleanUpPropertiesHolder properties;

    @Mock
    OnceboxScheduleStrategy strategy;

    @Mock
    InboxManager manager;

    @Mock
    DistributedLockRepository lock;

    @Mock
    ContinuableTaskDecorator decorator;

    InboxCleanUpScheduler tested;

    private final UUID workerId = UUID.randomUUID();
    private final String jobName = OnceboxJob.INBOX_CLEANUP.getJobName();

    @BeforeEach
    void setUp() {
        tested = new InboxCleanUpScheduler(
                workerId, properties, strategy, manager, lock, decorator
        );
    }

    @Test
    @DisplayName("UT InboxCleanUpScheduler() when workerId is null should throw NullPointerException")
    void constructor_whenWorkerIdIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new InboxCleanUpScheduler(null, properties, strategy, manager, lock, decorator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("workerId cannot be null");
    }

    @Test
    @DisplayName("UT InboxCleanUpScheduler() when properties is null should throw NullPointerException")
    void constructor_whenPropertiesIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new InboxCleanUpScheduler(workerId, null, strategy, manager, lock, decorator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("properties cannot be null");
    }

    @Test
    @DisplayName("UT InboxCleanUpScheduler() when strategy is null should throw NullPointerException")
    void constructor_whenStrategyIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new InboxCleanUpScheduler(workerId, properties, null, manager, lock, decorator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("scheduleStrategy cannot be null");
    }

    @Test
    @DisplayName("UT InboxCleanUpScheduler() when manager is null should throw NullPointerException")
    void constructor_whenManagerIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new InboxCleanUpScheduler(workerId, properties, strategy, null, lock, decorator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("manager cannot be null");
    }

    @Test
    @DisplayName("UT InboxCleanUpScheduler() when lock is null should throw NullPointerException")
    void constructor_whenLockIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new InboxCleanUpScheduler(workerId, properties, strategy, manager, null, decorator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("lock cannot be null");
    }

    @Test
    @DisplayName("UT InboxCleanUpScheduler() when decorator is null should throw NullPointerException")
    void constructor_whenDecoratorIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new InboxCleanUpScheduler(workerId, properties, strategy, manager, lock, null))
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
    @DisplayName("UT schedule() when lock acquired by another instance, should return false and not execute")
    void schedule_whenLockNotAcquired_shouldReturnFalseAndNotExecute() {
        // given
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());
        when(lock.tryLock(jobName, workerId)).thenReturn(false);

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertFalse(result);
        verifyNoInteractions(properties);
        verifyNoInteractions(manager);
        verify(lock, never()).unlock(anyString(), any(UUID.class));
    }

    @Test
    @DisplayName("UT schedule() when cleaned count equals batch size, should return true and unlock")
    void schedule_whenCleanedCountEqualsBatchSize_shouldReturnTrueAndUnlock() {
        // given
        int batchSize = 50;
        Duration ttl = Duration.ofDays(7);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());
        when(lock.tryLock(jobName, workerId)).thenReturn(true);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getTtl()).thenReturn(ttl);
        when(manager.cleanBatchByTtl(ttl, batchSize)).thenReturn(batchSize);

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertTrue(result);
        verify(lock).unlock(jobName, workerId);
    }

    @Test
    @DisplayName("UT schedule() when cleaned count less than batch size, should return false and unlock")
    void schedule_whenCleanedCountLessThanBatchSize_shouldReturnFalseAndUnlock() {
        // given
        int batchSize = 50;
        Duration ttl = Duration.ofDays(7);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());
        when(lock.tryLock(jobName, workerId)).thenReturn(true);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getTtl()).thenReturn(ttl);
        when(manager.cleanBatchByTtl(ttl, batchSize)).thenReturn(batchSize - 1);

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertFalse(result);
        verify(lock).unlock(jobName, workerId);
    }

    @Test
    @DisplayName("UT schedule() when cleaned count is zero, should return false and unlock")
    void schedule_whenCleanedCountIsZero_shouldReturnFalseAndUnlock() {
        // given
        int batchSize = 50;
        Duration ttl = Duration.ofDays(7);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());
        when(lock.tryLock(jobName, workerId)).thenReturn(true);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getTtl()).thenReturn(ttl);
        when(manager.cleanBatchByTtl(ttl, batchSize)).thenReturn(0);

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertFalse(result);
        verify(lock).unlock(jobName, workerId);
    }

    @Test
    @DisplayName("UT schedule() should pass correct params to manager")
    void schedule_shouldPassCorrectParamsToManager() {
        // given
        int batchSize = 100;
        Duration ttl = Duration.ofHours(48);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());
        when(lock.tryLock(jobName, workerId)).thenReturn(true);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getTtl()).thenReturn(ttl);
        when(manager.cleanBatchByTtl(any(), anyInt())).thenReturn(0);

        // when
        tested.schedule();
        captureAndRun();

        // then
        verify(manager).cleanBatchByTtl(ttl, batchSize);
    }

    @Test
    @DisplayName("UT schedule() when manager throws exception, should return false, not rethrow and ALWAYS unlock")
    void schedule_whenManagerThrows_shouldReturnFalseAndNotRethrowAndUnlock() {
        // given
        int batchSize = 50;
        Duration ttl = Duration.ofDays(7);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());
        when(lock.tryLock(jobName, workerId)).thenReturn(true);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getTtl()).thenReturn(ttl);
        when(manager.cleanBatchByTtl(ttl, batchSize)).thenThrow(new RuntimeException("DB error"));

        // when
        tested.schedule();
        boolean result = assertDoesNotThrow(this::captureAndRun);

        // then
        assertFalse(result);
        verify(lock).unlock(jobName, workerId);
    }
}
