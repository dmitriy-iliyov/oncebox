package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.core.OnceboxScheduler;
import io.github.dmitriyiliyov.oncebox.core.inbox.*;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.OnceboxJob;
import io.github.dmitriyiliyov.oncebox.starter.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@ConditionalOnProperty(
        prefix = "oncebox.inbox",
        name = "enabled",
        havingValue = "true"
)
@Import({
        InboxKafkaAutoConfiguration.class,
        InboxRabbitAutoConfiguration.class,
        InboxMetricsAutoConfiguration.class,
        InboxCacheAutoConfiguration.class,
        InboxCacheMetricsAutoConfiguration.class
})
public class InboxAutoConfiguration {

    private final InboxProperties inboxProperties;

    public InboxAutoConfiguration(InboxProperties inboxProperties) {
        this.inboxProperties = inboxProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public InboxRepository inboxRepository(OnceboxRepositoryFactory repositoryFactory) {
        return repositoryFactory.createInboxRepository();
    }

    @Bean
    @ConditionalOnMissingBean(name = "inboxManager")
    public InboxManager inboxManager(InboxRepository repository) {
        return new DefaultInboxManager(repository);
    }

    @Bean(defaultCandidate = false)
    @ConditionalOnMissingBean(name = "inboxUndecoratedTransactionalIdempotentConsumer")
    public IdempotentConsumer inboxUndecoratedTransactionalIdempotentConsumer(
            TransactionTemplate transactionTemplate,
            InboxManager manager
    ) {
        return new TransactionalIdempotentConsumer(transactionTemplate, manager);
    }

    @Bean(defaultCandidate = false)
    @ConditionalOnMissingBean(name = "inboxUndecoratedNonTransactionalIdempotentConsumer")
    public IdempotentConsumer inboxUndecoratedNonTransactionalIdempotentConsumer(InboxManager manager) {
        return new NonTransactionalIdempotentConsumer(manager);
    }

    @Bean
    @TransactionalConsumer
    public IdempotentConsumer inboxTransactionalIdempotentConsumer(
            @Qualifier("inboxUndecoratedTransactionalIdempotentConsumer") IdempotentConsumer consumer,
            List<IdempotentConsumerDecoratorSupplier> decoratorSuppliers
    ) {
        return decorate(consumer, decoratorSuppliers);
    }

    @Bean
    @NonTransactionalConsumer
    public IdempotentConsumer inboxNonTransactionalIdempotentConsumer(
            @Qualifier("inboxUndecoratedNonTransactionalIdempotentConsumer") IdempotentConsumer consumer,
            List<IdempotentConsumerDecoratorSupplier> decoratorSuppliers
    ) {
        return decorate(consumer, decoratorSuppliers);
    }

    private IdempotentConsumer decorate(IdempotentConsumer consumer,
                                        List<IdempotentConsumerDecoratorSupplier> decoratorSuppliers) {
        IdempotentConsumer primaryConsumer = consumer;
        for (IdempotentConsumerDecoratorSupplier supplier : decoratorSuppliers) {
            primaryConsumer = supplier.supply(primaryConsumer);
        }
        return primaryConsumer;
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "oncebox.inbox.clean-up",
            name = "enabled",
            havingValue = "true"
    )
    public OnceboxScheduler inboxCleanUpScheduler(
            OnceboxProperties properties,
            @Qualifier("onceboxScheduledExecutorService") ScheduledExecutorService executor,
            InboxManager manager,
            OnceboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier,
            ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier,
            DistributedLockRepository lockRepository
    ) {
        return new InboxCleanUpScheduler(
                properties.getWorkerId(),
                inboxProperties.getCleanUp(),
                OnceboxScheduleStrategyFactory.create(
                        OnceboxTaskType.INBOX_CLEANUP.getValue(),
                        inboxProperties.getCleanUp().getPolling(),
                        executor,
                        scheduleStrategyListenerSupplier
                ),
                manager,
                lockRepository,
                continuableTaskDecoratorSupplier.supply(OnceboxTaskType.INBOX_CLEANUP.getValue())
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "oncebox.inbox.clean-up",
            name = "enabled",
            havingValue = "true"
    )
    public OnceboxJobCreateCommand inboxCleanUpJobCreateCommand(OnceboxProperties properties,
                                                                @Qualifier("onceboxJdbcTemplate") JdbcTemplate jdbcTemplate) {
        DistributedLockPropertiesResolver.LockDurations lockDurations = DistributedLockPropertiesResolver.resolve(
                properties.getDistributedLock(),
                inboxProperties.getCleanUp().getPolling()
        );
        return new DefaultOnceboxJobCreateCommand(
                jdbcTemplate,
                OnceboxJob.INBOX_CLEANUP.getJobName(),
                lockDurations.atLeastFor(),
                lockDurations.atMostFor()
        );
    }
}