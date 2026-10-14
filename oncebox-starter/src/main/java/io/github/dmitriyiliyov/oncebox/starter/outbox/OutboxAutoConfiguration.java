package io.github.dmitriyiliyov.oncebox.starter.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.aop.OutboxPublishAspect;
import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.oncebox.core.OnceboxScheduler;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.OnceboxJob;
import io.github.dmitriyiliyov.oncebox.core.outbox.*;
import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategy;
import io.github.dmitriyiliyov.oncebox.starter.*;
import io.github.dmitriyiliyov.oncebox.starter.outbox.dlq.OutboxDlqAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ConditionalOnProperty(
        prefix = "oncebox.outbox",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Import({
        OutboxPollingSchedulerRegistrar.class,
        OutboxDlqAutoConfiguration.class,
        OutboxKafkaAutoConfiguration.class,
        OutboxRabbitAutoConfiguration.class,
        OutboxMetricsAutoConfiguration.class
})
public class OutboxAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OutboxAutoConfiguration.class);
    private final OutboxProperties outboxProperties;

    public OutboxAutoConfiguration(OutboxProperties outboxProperties) {
        this.outboxProperties = outboxProperties;
        log.debug("OutboxAutoConfiguration successfully created");
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxRepository outboxRepository(OnceboxRepositoryFactory repositoryFactory) {
        return repositoryFactory.createOutboxRepository();
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxManager")
    public OutboxManager outboxManager(OutboxRepository repository) {
        return new DefaultOutboxManager(repository);
    }

    @Bean
    @ConditionalOnMissingBean
    public UuidGenerator outboxUuidGenerator() {
        return new UuidV7Generator();
    }

    @Bean
    @ConditionalOnClass(ObjectMapper.class)
    @ConditionalOnMissingBean
    public OutboxSerializer outboxSerializer(ObjectMapper mapper, UuidGenerator uuidGenerator) {
        return new JacksonOutboxSerializer(mapper, uuidGenerator);
    }

    @Bean
    public OutboxPublisher outboxPublisher(OutboxSerializer serializer, OutboxManager manager) {
        return new DefaultOutboxPublisher(outboxProperties, serializer, manager);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxProcessor outboxProcessor(OutboxManager manager, OutboxSender sender) {
        return new DefaultOutboxProcessor(manager, sender);
    }

    @Bean
    public OutboxPublishAspect outboxPublishAspect(OutboxPublisher publisher) {
        return new OutboxPublishAspect(publisher);
    }

    @Bean
    @ConditionalOnMissingBean(name = "outboxRecoveryScheduler")
    public OnceboxScheduler outboxRecoveryScheduler(ScheduledExecutorService executor,
                                                   OnceboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier,
                                                   OutboxManager manager,
                                                   ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier) {
        OutboxProperties.StuckRecoveryProperties stuckRecoveryProperties = outboxProperties.getStuckRecovery();
        OnceboxScheduleStrategy strategy = OnceboxScheduleStrategyFactory.create(
                OnceboxTaskType.OUTBOX_STUCK_RECOVERY.getValue(),
                stuckRecoveryProperties.getPolling(),
                executor,
                scheduleStrategyListenerSupplier
        );
        ContinuableTaskDecorator continuableTaskDecorator = continuableTaskDecoratorSupplier.supply(OnceboxTaskType.OUTBOX_STUCK_RECOVERY.getValue());
        return new OutboxRecoveryScheduler(stuckRecoveryProperties, strategy, manager, continuableTaskDecorator);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "oncebox.outbox.clean-up",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    @ConditionalOnMissingBean(name = "outboxCleanUpScheduler")
    public OnceboxScheduler outboxCleanUpScheduler(OnceboxProperties properties,
                                                  ScheduledExecutorService executor,
                                                  OnceboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier,
                                                  OutboxManager manager,
                                                  DistributedLockRepository lockRepository,
                                                  ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier) {
        OnceboxProperties.CleanUpProperties cleanUpProperties = outboxProperties.getCleanUp();
        OnceboxScheduleStrategy strategy = OnceboxScheduleStrategyFactory.create(
                OnceboxTaskType.OUTBOX_PROCESSED_CLEANUP.getValue(),
                cleanUpProperties.getPolling(),
                executor,
                scheduleStrategyListenerSupplier
        );
        ContinuableTaskDecorator continuableTaskDecorator = continuableTaskDecoratorSupplier.supply(OnceboxTaskType.OUTBOX_PROCESSED_CLEANUP.getValue());
        return new OutboxCleanUpScheduler(
                properties.getWorkerId(), cleanUpProperties, strategy, manager, lockRepository, continuableTaskDecorator
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "oncebox.outbox.clean-up",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public OnceboxJobCreateCommand outboxCleanUpJobCreateCommand(OnceboxProperties properties,
                                                                 @Qualifier("onceboxJdbcTemplate") JdbcTemplate jdbcTemplate) {
        DistributedLockPropertiesResolver.LockDurations lockDurations = DistributedLockPropertiesResolver.resolve(
                properties.getDistributedLock(),
                outboxProperties.getCleanUp().getPolling()
        );
        return new DefaultOnceboxJobCreateCommand(
                jdbcTemplate,
                OnceboxJob.OUTBOX_PROCESSED_CLEANUP.getJobName(),
                lockDurations.atLeastFor(),
                lockDurations.atMostFor()
        );
    }
}
