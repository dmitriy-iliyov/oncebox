package io.github.dmitriyiliyov.oncebox.starter.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.OnceboxScheduler;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.OnceboxJob;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxManager;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.*;
import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategy;
import io.github.dmitriyiliyov.oncebox.starter.*;
import io.github.dmitriyiliyov.oncebox.starter.outbox.OutboxProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ConditionalOnProperty(
        prefix = "oncebox.outbox.dlq",
        name = "enabled",
        havingValue = "true"
)
@Import({
        OutboxDlqMetricsAutoConfiguration.class,
        OutboxDlqApiAutoConfiguration.class,
        OutboxDlqApiMetricsAutoConfiguration.class
})
public class OutboxDlqAutoConfiguration {

    private final OutboxProperties outboxProperties;

    public OutboxDlqAutoConfiguration(OutboxProperties outboxProperties) {
        this.outboxProperties = outboxProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqRepository outboxDlqRepository(OnceboxRepositoryFactory repositoryFactory) {
        return repositoryFactory.createOutboxDlqRepository();
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxDlqManager")
    public OutboxDlqManager outboxDlqManager(OutboxDlqRepository repository, Clock clock) {
        return new DefaultOutboxDlqManager(repository, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqHandler outboxDlqHandler() {
        return new LogOutboxDlqHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqEventMapper outboxDlqEventMapper(Clock clock) {
        return new DefaultOutboxDlqEventMapper(clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqTransfer outboxDlqTransfer(TransactionTemplate transactionTemplate,
                                               OutboxManager manager,
                                               OutboxDlqManager dlqManager,
                                               OutboxDlqEventMapper mapper,
                                               OutboxDlqHandler handler) {
        return new DefaultOutboxDlqTransfer(transactionTemplate, manager, dlqManager, mapper, handler);
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxDlqTransferToScheduler")
    public OnceboxScheduler outboxDlqTransferToScheduler(ScheduledExecutorService executor,
                                                         OutboxDlqTransfer transfer,
                                                         OnceboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier,
                                                        ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier) {
        OutboxProperties.DlqProperties dlqProperties = outboxProperties.getDlq();
        String taskType = OnceboxTaskType.OUTBOX_DLQ_TRANSFER_TO.getValue();
        OnceboxScheduleStrategy scheduleStrategy = OnceboxScheduleStrategyFactory.create(
                taskType,
                dlqProperties.getTransferTo().getPolling(),
                executor,
                scheduleStrategyListenerSupplier
        );
        return new OutboxDlqTransferScheduler(
                () -> outboxProperties.getDlq().getTransferTo(),
                scheduleStrategy,
                transfer::transferToDlq,
                continuableTaskDecoratorSupplier.supply(OnceboxTaskType.OUTBOX_DLQ_TRANSFER_TO.getValue()),
                OutboxDlqTransferScheduler.LogMessage.transferTo()
        );
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxDlqTransferFromScheduler")
    public OnceboxScheduler outboxDlqTransferFromScheduler(ScheduledExecutorService executor,
                                                          OutboxDlqTransfer transfer,
                                                          OnceboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier,
                                                          ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier) {
        OutboxProperties.DlqProperties dlqProperties = outboxProperties.getDlq();
        OnceboxScheduleStrategy scheduleStrategy = OnceboxScheduleStrategyFactory.create(
                OnceboxTaskType.OUTBOX_DLQ_TRANSFER_FROM.getValue(),
                dlqProperties.getTransferFrom().getPolling(),
                executor,
                scheduleStrategyListenerSupplier
        );
        return new OutboxDlqTransferScheduler(
                () -> outboxProperties.getDlq().getTransferFrom(),
                scheduleStrategy,
                transfer::transferFromDlq,
                continuableTaskDecoratorSupplier.supply(OnceboxTaskType.OUTBOX_DLQ_TRANSFER_FROM.getValue()),
                OutboxDlqTransferScheduler.LogMessage.transferFrom()
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "oncebox.outbox.clean-up",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    @ConditionalOnMissingBean(name = "outboxDlqCleanUpScheduler")
    public OnceboxScheduler outboxDlqCleanUpScheduler(OnceboxProperties properties,
                                                     ScheduledExecutorService executor,
                                                     OutboxDlqManager manager,
                                                     DistributedLockRepository lockRepository,
                                                     OnceboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier,
                                                     ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier) {
        OnceboxProperties.CleanUpProperties cleanUpProperties = outboxProperties.getDlq().getCleanUp();
        OnceboxScheduleStrategy scheduleStrategy = OnceboxScheduleStrategyFactory.create(
                OnceboxTaskType.OUTBOX_DLQ_CLEANUP.getValue(),
                cleanUpProperties.getPolling(),
                executor,
                scheduleStrategyListenerSupplier
        );
        return new OutboxDlqCleanUpScheduler(
                properties.getWorkerId(),
                cleanUpProperties,
                scheduleStrategy,
                continuableTaskDecoratorSupplier.supply(OnceboxTaskType.OUTBOX_DLQ_CLEANUP.getValue()),
                manager,
                lockRepository
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "oncebox.outbox.clean-up",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public OnceboxJobCreateCommand outboxDlqCleanUpJobCreateCommand(OnceboxProperties properties,
                                                                    @Qualifier("onceboxJdbcTemplate") JdbcTemplate jdbcTemplate,
                                                                    Clock clock) {
        DistributedLockPropertiesResolver.LockDurations lockDurations = DistributedLockPropertiesResolver.resolve(
                properties.getDistributedLock(),
                outboxProperties.getDlq().getCleanUp().getPolling()
        );
        return new DefaultOnceboxJobCreateCommand(
                jdbcTemplate,
                clock,
                OnceboxJob.OUTBOX_DLQ_CLEANUP.getJobName(),
                lockDurations.atLeastFor(),
                lockDurations.atMostFor()
        );
    }
}
