package io.github.dmitriyiliyov.oncebox.starter.outbox;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.oncebox.core.OnceboxScheduler;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxCleanUpScheduler;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxManager;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxRecoveryScheduler;
import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategy;
import io.github.dmitriyiliyov.oncebox.starter.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxAutoConfigurationUnitTests {

    private OutboxProperties props;
    private OutboxAutoConfiguration config;

    @Mock
    private ScheduledExecutorService executor;

    @Mock
    private OnceboxScheduleStrategyListenerSupplier scheduleStrategyListenerSupplier;

    @Mock
    private ContinuableTaskDecoratorSupplier continuableTaskDecoratorSupplier;

    @BeforeEach
    void setUp() {
        props = mock(OutboxProperties.class);
        config = new OutboxAutoConfiguration(props);
    }

    @Test
    @DisplayName("UT outboxRecoveryScheduler creates OutboxRecoveryScheduler")
    void outboxRecoveryScheduler_createsScheduler(@Mock OutboxManager manager) {
        OutboxProperties.StuckRecoveryProperties stuckRecoveryProperties = mock(OutboxProperties.StuckRecoveryProperties.class);
        when(stuckRecoveryProperties.getPolling()).thenReturn(mock(OnceboxProperties.PollingProperties.class));
        when(props.getStuckRecovery()).thenReturn(stuckRecoveryProperties);
        when(continuableTaskDecoratorSupplier.supply(anyString())).thenReturn(mock(ContinuableTaskDecorator.class));

        try (MockedStatic<OnceboxScheduleStrategyFactory> strategyFactory = mockStatic(OnceboxScheduleStrategyFactory.class)) {
            strategyFactory.when(() -> OnceboxScheduleStrategyFactory.create(any(), any(), any(), any()))
                    .thenReturn(mock(OnceboxScheduleStrategy.class));

            OnceboxScheduler scheduler = config.outboxRecoveryScheduler(
                    executor, scheduleStrategyListenerSupplier, manager, continuableTaskDecoratorSupplier
            );

            assertThat(scheduler).isInstanceOf(OutboxRecoveryScheduler.class);
        }
    }

    @Test
    @DisplayName("UT outboxCleanUpScheduler creates OutboxCleanUpScheduler")
    void outboxCleanUpScheduler_createsScheduler(@Mock OnceboxProperties outboxProperties,
                                                 @Mock OutboxManager manager,
                                                 @Mock DistributedLockRepository lockRepository) {
        OnceboxProperties.CleanUpProperties cleanUpProperties = mock(OnceboxProperties.CleanUpProperties.class);
        when(cleanUpProperties.getPolling()).thenReturn(mock(OnceboxProperties.PollingProperties.class));
        when(props.getCleanUp()).thenReturn(cleanUpProperties);
        when(outboxProperties.getWorkerId()).thenReturn(UUID.randomUUID());
        when(continuableTaskDecoratorSupplier.supply(anyString())).thenReturn(mock(ContinuableTaskDecorator.class));

        try (MockedStatic<OnceboxScheduleStrategyFactory> strategyFactory = mockStatic(OnceboxScheduleStrategyFactory.class)) {
            strategyFactory.when(() -> OnceboxScheduleStrategyFactory.create(any(), any(), any(), any()))
                    .thenReturn(mock(OnceboxScheduleStrategy.class));

            OnceboxScheduler scheduler = config.outboxCleanUpScheduler(
                    outboxProperties, executor, scheduleStrategyListenerSupplier, manager, lockRepository, continuableTaskDecoratorSupplier
            );

            assertThat(scheduler).isInstanceOf(OutboxCleanUpScheduler.class);
        }
    }

    @Test
    @DisplayName("UT outboxCleanUpJobCreateCommand creates DefaultOnceboxJobCreateCommand")
    void outboxCleanUpJobCreateCommand_createsCommand(@Mock OnceboxProperties outboxProperties,
                                                      @Mock JdbcTemplate jdbcTemplate) {
        OnceboxProperties.CleanUpProperties cleanUpProperties = mock(OnceboxProperties.CleanUpProperties.class);
        when(cleanUpProperties.getPolling()).thenReturn(mock(OnceboxProperties.PollingProperties.class));
        when(props.getCleanUp()).thenReturn(cleanUpProperties);
        when(outboxProperties.getDistributedLock()).thenReturn(mock(OnceboxProperties.DistributedLockProperties.class));

        try (MockedStatic<DistributedLockPropertiesResolver> resolver = mockStatic(DistributedLockPropertiesResolver.class)) {
            DistributedLockPropertiesResolver.LockDurations lockDurations = mock(DistributedLockPropertiesResolver.LockDurations.class);
            when(lockDurations.atLeastFor()).thenReturn(Duration.ofSeconds(1).toMillis());
            when(lockDurations.atMostFor()).thenReturn(Duration.ofSeconds(5).toMillis());
            resolver.when(() -> DistributedLockPropertiesResolver.resolve(any(), any())).thenReturn(lockDurations);

            OnceboxJobCreateCommand command = config.outboxCleanUpJobCreateCommand(outboxProperties, jdbcTemplate);

            assertThat(command).isInstanceOf(DefaultOnceboxJobCreateCommand.class);
        }
    }
}