package io.github.dmitriyiliyov.oncebox.core.outbox;

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
public class OutboxCleanUpSchedulerUnitTests {

    @Mock
    OnceboxPropertiesHolder.CleanUpPropertiesHolder properties;

    @Mock
    OnceboxScheduleStrategy strategy;

    @Mock
    OutboxManager manager;

    @Mock
    DistributedLockRepository lock;

    @Mock
    ContinuableTaskDecorator decorator;

    OutboxCleanUpScheduler tested;

    private final UUID workerId = UUID.randomUUID();
    private final String jobName = OnceboxJob.OUTBOX_PROCESSED_CLEANUP.getJobName();

    @BeforeEach
    void setUp() {
        tested = new OutboxCleanUpScheduler(
                workerId, properties, strategy, manager, lock, decorator
        );
    }

    @Test
    @DisplayName("UT constructor when workerId is null should throw NullPointerException")
    void constructor_whenWorkerIdIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxCleanUpScheduler(null, properties, strategy, manager, lock, decorator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("workerId cannot be null");
    }

    @Test
    @DisplayName("UT constructor when cleanupProperties is null should throw NullPointerException")
    void constructor_whenCleanupPropertiesIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxCleanUpScheduler(workerId, null, strategy, manager, lock, decorator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cleanupProperties cannot be null");
    }

    @Test
    @DisplayName("UT constructor when strategy is null should throw NullPointerException")
    void constructor_whenStrategyIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxCleanUpScheduler(workerId, properties, null, manager, lock, decorator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("scheduleStrategy cannot be null");
    }

    @Test
    @DisplayName("UT constructor when manager is null should throw NullPointerException")
    void constructor_whenManagerIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxCleanUpScheduler(workerId, properties, strategy, null, lock, decorator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("manager cannot be null");
    }

    @Test
    @DisplayName("UT constructor when lock is null should throw NullPointerException")
    void constructor_whenLockIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxCleanUpScheduler(workerId, properties, strategy, manager, null, decorator))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("lock cannot be null");
    }

    @Test
    @DisplayName("UT constructor when continuableTaskDecorator is null should throw NullPointerException")
    void constructor_whenContinuableTaskDecoratorIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxCleanUpScheduler(workerId, properties, strategy, manager, lock, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("taskDecorator cannot be null");
    }

    private boolean captureAndRun() {
        ArgumentCaptor<ContinuableTask> captor = ArgumentCaptor.forClass(ContinuableTask.class);
        verify(strategy).scheduleExecution(captor.capture());
        return captor.getValue().run();
    }

    @Test
    void schedule_shouldDelegateToStrategy() {
        // given
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());

        // when
        tested.schedule();

        // then
        verify(strategy).scheduleExecution(any());
    }

    @Test
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
    void schedule_whenDeletedCountEqualsBatchSize_shouldReturnTrueAndUnlock() {
        // given
        int batchSize = 50;
        Duration ttl = Duration.ofDays(7);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());
        when(lock.tryLock(jobName, workerId)).thenReturn(true);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getTtl()).thenReturn(ttl);
        when(manager.deleteProcessedBatch(ttl, batchSize)).thenReturn(batchSize);

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertTrue(result);
        verify(lock).unlock(jobName, workerId);
    }

    @Test
    void schedule_whenDeletedCountLessThanBatchSize_shouldReturnFalseAndUnlock() {
        // given
        int batchSize = 50;
        Duration ttl = Duration.ofDays(7);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());
        when(lock.tryLock(jobName, workerId)).thenReturn(true);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getTtl()).thenReturn(ttl);
        when(manager.deleteProcessedBatch(ttl, batchSize)).thenReturn(batchSize - 1);

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertFalse(result);
        verify(lock).unlock(jobName, workerId);
    }

    @Test
    void schedule_whenDeletedCountIsZero_shouldReturnFalseAndUnlock() {
        // given
        int batchSize = 50;
        Duration ttl = Duration.ofDays(7);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());
        when(lock.tryLock(jobName, workerId)).thenReturn(true);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getTtl()).thenReturn(ttl);
        when(manager.deleteProcessedBatch(ttl, batchSize)).thenReturn(0);

        // when
        tested.schedule();
        boolean result = captureAndRun();

        // then
        assertFalse(result);
        verify(lock).unlock(jobName, workerId);
    }

    @Test
    void schedule_shouldPassCorrectThresholdToManager() {
        // given
        int batchSize = 100;
        Duration ttl = Duration.ofHours(48);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());
        when(lock.tryLock(jobName, workerId)).thenReturn(true);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getTtl()).thenReturn(ttl);
        when(manager.deleteProcessedBatch(any(), anyInt())).thenReturn(0);

        // when
        tested.schedule();
        captureAndRun();

        // then
        verify(manager).deleteProcessedBatch(ttl, batchSize);
        verify(lock).unlock(jobName, workerId);
    }

    @Test
    void schedule_whenManagerThrowsException_shouldReturnFalseAndNotRethrowAndUnlock() {
        // given
        int batchSize = 50;
        Duration ttl = Duration.ofDays(7);
        when(decorator.decorate(any(ContinuableTask.class))).then(returnsFirstArg());
        when(lock.tryLock(jobName, workerId)).thenReturn(true);
        when(properties.getBatchSize()).thenReturn(batchSize);
        when(properties.getTtl()).thenReturn(ttl);
        when(manager.deleteProcessedBatch(ttl, batchSize)).thenThrow(new RuntimeException());

        // when
        tested.schedule();
        boolean result = assertDoesNotThrow(this::captureAndRun);

        // then
        assertFalse(result);
        verify(lock).unlock(jobName, workerId);
    }
}