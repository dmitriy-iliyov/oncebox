package io.github.dmitriyiliyov.oncebox.core.outbox;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTask;
import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.oncebox.core.OnceboxPropertiesHolder;
import io.github.dmitriyiliyov.oncebox.core.OnceboxScheduler;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.OnceboxJob;
import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

public final class OutboxCleanUpScheduler implements OnceboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanUpScheduler.class);
    private static final OnceboxJob JOB = OnceboxJob.OUTBOX_PROCESSED_CLEANUP;

    private final UUID workerId;
    private final OnceboxPropertiesHolder.CleanUpPropertiesHolder properties;
    private final OnceboxScheduleStrategy scheduleStrategy;
    private final OutboxManager manager;
    private final DistributedLockRepository lock;
    private final ContinuableTaskDecorator taskDecorator;

    public OutboxCleanUpScheduler(UUID workerId,
                                  OnceboxPropertiesHolder.CleanUpPropertiesHolder cleanupProperties,
                                  OnceboxScheduleStrategy scheduleStrategy,
                                  OutboxManager manager,
                                  DistributedLockRepository lock,
                                  ContinuableTaskDecorator taskDecorator) {
        this.workerId = Objects.requireNonNull(workerId, "workerId cannot be null");
        this.properties = Objects.requireNonNull(cleanupProperties, "cleanupProperties cannot be null");
        this.scheduleStrategy = Objects.requireNonNull(scheduleStrategy, "scheduleStrategy cannot be null");
        this.manager = Objects.requireNonNull(manager, "manager cannot be null");
        this.lock = Objects.requireNonNull(lock, "lock cannot be null");
        this.taskDecorator = Objects.requireNonNull(taskDecorator, "taskDecorator cannot be null");
    }

    @Override
    public void schedule() {
        ContinuableTask task = () -> {
            log.debug("Start clean up processed outbox events");
            if (!lock.tryLock(JOB.getJobName(), workerId)) {
                log.debug("Lock '{}' is held by another instance, skipping task execution", JOB.getJobName());
                return false;
            }
            try {
                int batchSize = properties.getBatchSize();
                int deletedCount = manager.deleteProcessedBatch(properties.getTtl(), batchSize);
                log.debug("Successfully cleaned {} processed outbox events", deletedCount);
                return deletedCount == batchSize;
            } catch (Exception e) {
                log.error("Error when cleaning up processed outbox events, workerId={}", workerId, e);
                return false;
            } finally {
                lock.unlock(JOB.getJobName(), workerId);
            }
        };
        scheduleStrategy.scheduleExecution(taskDecorator.decorate(task));
    }
}