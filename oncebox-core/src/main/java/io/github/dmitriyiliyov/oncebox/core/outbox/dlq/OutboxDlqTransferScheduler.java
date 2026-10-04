package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTask;
import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.oncebox.core.OnceboxScheduler;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxPropertiesHolder;
import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class OutboxDlqTransferScheduler implements OnceboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqTransferScheduler.class);

    private final OutboxPropertiesHolder.DlqPropertiesHolder.TransferPropertiesHolder properties;
    private final OnceboxScheduleStrategy scheduleStrategy;
    private final Function<Integer, Integer> transferApplier;
    private final ContinuableTaskDecorator taskDecorator;
    private final LogMessage logMessage;

    public OutboxDlqTransferScheduler(Supplier<OutboxPropertiesHolder.DlqPropertiesHolder.TransferPropertiesHolder> propertiesSupplier,
                                      OnceboxScheduleStrategy strategy,
                                      Function<Integer, Integer> transferApplier,
                                      ContinuableTaskDecorator taskDecorator,
                                      LogMessage logMessage) {
        Objects.requireNonNull(propertiesSupplier, "propertiesSupplier cannot be null");
        this.properties = Objects.requireNonNull(propertiesSupplier.get(), "properties cannot be null");
        this.scheduleStrategy = Objects.requireNonNull(strategy, "scheduleStrategy cannot be null");
        this.transferApplier = Objects.requireNonNull(transferApplier, "transferApplier cannot be null");
        this.taskDecorator = Objects.requireNonNull(taskDecorator, "taskDecorator cannot be null");
        this.logMessage = Objects.requireNonNull(logMessage, "logMessage cannot be null");
    }

    @Override
    public void schedule() {
        ContinuableTask task = () -> {
            int batchSize = properties.getBatchSize();
            int transferredCount = 0;
            try {
                log.debug(logMessage.onStart());
                transferredCount = transferApplier.apply(batchSize);
            } catch (Exception e) {
                log.error(logMessage.onException(), e);
            }
            return transferredCount == batchSize;
        };
        scheduleStrategy.scheduleExecution(taskDecorator.decorate(task));
    }

    public record LogMessage(
            String onStart,
            String onException
    ) {
        public static LogMessage transferTo() {
            return new LogMessage(
                    "Start transferring outbox events to DLQ",
                    "Error transferring outbox events to DLQ"
            );
        }

        public static LogMessage transferFrom() {
            return new LogMessage(
                    "Start transferring outbox events from DLQ",
                    "Error transferring outbox events from DLQ"
            );
        }
    }
}