package io.github.dmitriyiliyov.oncebox.core.inbox;

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

/**
 * Deletes consumed inbox records past their {@code ttl}, one batch per run. A run holds the cluster-wide lock of
 * {@link OnceboxJob#INBOX_CLEANUP}, so only one instance cleans at a time; an instance that does not get the lock
 * skips the run and reports it as one that did not fill its batch.
 */
public final class InboxCleanUpScheduler implements OnceboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(InboxCleanUpScheduler.class);
    private static final OnceboxJob JOB = OnceboxJob.INBOX_CLEANUP;

    private final UUID workerId;
    private final OnceboxPropertiesHolder.CleanUpPropertiesHolder properties;
    private final OnceboxScheduleStrategy scheduleStrategy;
    private final InboxManager manager;
    private final DistributedLockRepository lock;
    private final ContinuableTaskDecorator taskDecorator;

    public InboxCleanUpScheduler(UUID workerId,
                                 OnceboxPropertiesHolder.CleanUpPropertiesHolder properties,
                                 OnceboxScheduleStrategy scheduleStrategy,
                                 InboxManager manager,
                                 DistributedLockRepository lock,
                                 ContinuableTaskDecorator taskDecorator) {
        this.workerId = Objects.requireNonNull(workerId, "workerId cannot be null");
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
        this.scheduleStrategy = Objects.requireNonNull(scheduleStrategy, "scheduleStrategy cannot be null");
        this.manager = Objects.requireNonNull(manager, "manager cannot be null");
        this.lock = Objects.requireNonNull(lock, "lock cannot be null");
        this.taskDecorator = Objects.requireNonNull(taskDecorator, "taskDecorator cannot be null");
    }

    @Override
    public void schedule() {
        ContinuableTask task = () -> {
            log.debug("Start clean up inbox events");
            if (!lock.tryLock(JOB.getJobName(), workerId)) {
                log.debug("Lock '{}' is held by another instance, skipping task execution", JOB.getJobName());
                return false;
            }
            try {
                int batchSize = properties.getBatchSize();
                int cleanedCount = manager.cleanBatchByTtl(properties.getTtl(), batchSize);
                log.debug("Successfully cleaned {} inbox events", cleanedCount);
                return cleanedCount == batchSize;
            } catch (Exception e) {
                log.error("Error when cleaning up inbox events, workerId={}", workerId, e);
                return false;
            } finally {
                lock.unlock(JOB.getJobName(), workerId);
            }
        };
        scheduleStrategy.scheduleExecution(taskDecorator.decorate(task));
    }
}
