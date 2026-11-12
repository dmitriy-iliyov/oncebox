package io.github.dmitriyiliyov.oncebox.core.outbox;


import io.github.dmitriyiliyov.oncebox.core.ContinuableTask;
import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.oncebox.core.OnceboxScheduler;
import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Returns outbox events stuck in processing longer than {@code max-batch-processing-time} to pending, one batch per
 * run, so that events of an instance that died mid-send are sent again. It runs on every instance without a job
 * lock.
 *
 * @see OutboxManager#recoverStuckBatch(java.time.Duration, int)
 */
public final class OutboxRecoveryScheduler implements OnceboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRecoveryScheduler.class);

    private final OutboxPropertiesHolder.StuckRecoveryPropertiesHolder properties;
    private final OnceboxScheduleStrategy scheduleStrategy;
    private final OutboxManager manager;
    private final ContinuableTaskDecorator taskDecorator;

    public OutboxRecoveryScheduler(OutboxPropertiesHolder.StuckRecoveryPropertiesHolder properties,
                                   OnceboxScheduleStrategy scheduleStrategy,
                                   OutboxManager manager,
                                   ContinuableTaskDecorator taskDecorator) {
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
        this.scheduleStrategy = Objects.requireNonNull(scheduleStrategy, "scheduleStrategy cannot be null");
        this.manager = Objects.requireNonNull(manager, "manager cannot be null");
        this.taskDecorator = Objects.requireNonNull(taskDecorator, "taskDecorator cannot be null");
    }

    @Override
    public void schedule() {
        ContinuableTask continuableTask = () -> {
            int batchSize = properties.getBatchSize();
            int recoveredCount = 0;
            try {
                log.debug("Start recovering stuck outbox events");
                recoveredCount = manager.recoverStuckBatch(properties.getMaxBatchProcessingTime(), batchSize);
            } catch (Exception e) {
                log.error("Error process recover stuck outbox events", e);
            }
            return recoveredCount == batchSize;
        };
        scheduleStrategy.scheduleExecution(taskDecorator.decorate(continuableTask));
    }
}
