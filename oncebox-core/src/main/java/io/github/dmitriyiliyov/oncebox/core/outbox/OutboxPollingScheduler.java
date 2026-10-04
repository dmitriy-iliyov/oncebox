package io.github.dmitriyiliyov.oncebox.core.outbox;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTask;
import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.oncebox.core.OnceboxScheduler;
import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class OutboxPollingScheduler implements OnceboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPollingScheduler.class);

    private final OutboxPropertiesHolder.EventPropertiesHolder properties;
    private final OutboxProcessor processor;
    private final OnceboxScheduleStrategy scheduleStrategy;
    private final ContinuableTaskDecorator taskDecorator;

    public OutboxPollingScheduler(OutboxPropertiesHolder.EventPropertiesHolder properties,
                                  OnceboxScheduleStrategy scheduleStrategy,
                                  OutboxProcessor processor,
                                  ContinuableTaskDecorator taskDecorator) {
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
        this.scheduleStrategy = Objects.requireNonNull(scheduleStrategy, "scheduleStrategy cannot be null");
        this.processor = Objects.requireNonNull(processor, "processor cannot be null");
        this.taskDecorator = Objects.requireNonNull(taskDecorator, "taskDecorator cannot be null");
    }

    @Override
    public void schedule() {
        ContinuableTask task = () -> {
            int batchSize = properties.getBatchSize();
            int processedCount = 0;
            try {
                log.debug("Start processing outbox events, eventType={}", properties.getEventType());
                processedCount = processor.process(properties);
            } catch (Exception e) {
                log.error("Error process outbox events, eventType={}", properties.getEventType(), e);
            }
            return processedCount == batchSize;
        };
        scheduleStrategy.scheduleExecution(taskDecorator.decorate(task));
    }
}
