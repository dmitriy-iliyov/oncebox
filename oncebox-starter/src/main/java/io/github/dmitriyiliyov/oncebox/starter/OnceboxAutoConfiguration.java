package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.OnceboxScheduler;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.starter.inbox.InboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.outbox.OutboxProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@AutoConfiguration(after = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        TaskSchedulingAutoConfiguration.class
})
@EnableConfigurationProperties(OnceboxProperties.class)
@Import(OnceboxMetricsAutoConfiguration.class)
public class OnceboxAutoConfiguration {

    private final OnceboxProperties properties;

    public OnceboxAutoConfiguration(OnceboxProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "oncebox.outbox",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public OutboxProperties outboxProperties() {
        return properties.getOutbox();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "oncebox.inbox",
            name = "enabled",
            havingValue = "true"
    )
    public InboxProperties inboxProperties() {
        return properties.getInbox();
    }

    @Bean(defaultCandidate = false)
    @ConditionalOnMissingBean(name = "onceboxJdbcTemplate")
    public JdbcTemplate onceboxJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnDatabaseType(type = DatabaseType.POSTGRESQL)
    public OnceboxRepositoryFactory postgreSqlOnceboxRepositoryFactory(
            @Qualifier("onceboxJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new PostgreSqlOnceboxRepositoryFactory(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnDatabaseType(type = DatabaseType.MYSQL)
    public OnceboxRepositoryFactory mySqlOnceboxRepositoryFactory(
            @Qualifier("onceboxJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new MySqlOnceboxRepositoryFactory(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnDatabaseType(type = DatabaseType.ORACLE)
    public OnceboxRepositoryFactory oracleOnceboxRepositoryFactory(
            @Qualifier("onceboxJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        return new OracleOnceboxRepositoryFactory(jdbcTemplate);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "oncebox.tables",
            name = "auto-create",
            havingValue = "true",
            matchIfMissing = true
    )
    public DataSourceInitializer onceboxDataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setEnabled(true);
        dataSourceInitializer.setDataSource(dataSource);
        dataSourceInitializer.setDatabasePopulator(OnceboxDatabasePopulatorFactory.create(properties, dataSource));
        return dataSourceInitializer;
    }

    @Bean
    @ConditionalOnAnyCleanUpEnabled
    @ConditionalOnMissingBean
    public DistributedLockRepository onceboxDistributedLockRepository(OnceboxRepositoryFactory repositoryFactory) {
        return repositoryFactory.createDistributedLockRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public OnceboxScheduleStrategyListenerSupplier noopOnceboxScheduleStrategyListenerSupplier() {
        return new NoopOnceboxScheduleStrategyListenerSupplier();
    }

    @Bean
    @ConditionalOnMissingBean
    public ContinuableTaskDecoratorSupplier noopOnceboxContinuableTaskDecoratorSupplier() {
        return new NoopContinuableTaskDecoratorSupplier();
    }

    @Bean
    public ScheduledExecutorService onceboxScheduledExecutorService() {
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory("oncebox-thrd-");
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                properties.getThreadPoolSize(),
                threadFactory
        );
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorServiceShutdownHook onceboxScheduledExecutorServiceShutdownHook(
            @Qualifier("onceboxScheduledExecutorService") ScheduledExecutorService service
    ) {
        return new DefaultScheduledExecutorServiceShutdownHook(service);
    }

    @Bean
    public ApplicationRunner onceboxJobsInitializer(List<OnceboxJobCreateCommand> commands) {
        return args -> {
            for (OnceboxJobCreateCommand command : commands) {
                command.create();
            }
        };
    }

    @Bean
    public PostApplicationReadyOnceboxInitializer schedulersOnceboxInitializer(Map<String, OnceboxScheduler> schedulers) {
        return new SchedulersPostApplicationReadyOnceboxInitializer(schedulers);
    }

    @Bean
    public PostApplicationReadyOnceboxInitializer compositeOnceboxInitializer(
            OnceboxProperties properties,
            List<PostApplicationReadyOnceboxInitializer> initializers
    ) {
        return new CompositePostApplicationReadyOnceboxInitializer(properties, initializers);
    }
}
 