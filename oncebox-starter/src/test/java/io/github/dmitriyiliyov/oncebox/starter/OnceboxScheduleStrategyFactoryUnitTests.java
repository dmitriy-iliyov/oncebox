package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.polling.AdaptiveOnceboxScheduleStrategy;
import io.github.dmitriyiliyov.oncebox.core.polling.FixedOnceboxScheduleStrategy;
import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategy;
import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategyListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OnceboxScheduleStrategyFactoryUnitTests {

    @Test
    @DisplayName("UT create() should return FixedOnceboxScheduleStrategy when type is FIXED")
    public void create_whenTypeIsFixed_shouldReturnFixedStrategy() {
        // given
        String taskType = "taskType";
        OnceboxProperties.PollingProperties properties = new OnceboxProperties.PollingProperties();
        properties.setType(PollingType.FIXED);
        ScheduledExecutorService executorMock = mock(ScheduledExecutorService.class);

        OnceboxScheduleStrategyListenerSupplier listenerSupplier = mock(OnceboxScheduleStrategyListenerSupplier.class);
        when(listenerSupplier.supply(taskType)).thenReturn(mock(OnceboxScheduleStrategyListener.class));

        // when
        OnceboxScheduleStrategy strategy = OnceboxScheduleStrategyFactory.create(taskType, properties, executorMock, listenerSupplier);

        // then
        assertNotNull(strategy);
        assertTrue(strategy instanceof FixedOnceboxScheduleStrategy,
                "Strategy should be instance of FixedOnceboxScheduleStrategy");
    }

    @Test
    @DisplayName("UT create() should return AdaptiveOnceboxScheduleStrategy when type is ADAPTIVE")
    public void create_whenTypeIsAdaptive_shouldReturnAdaptiveStrategy() {
        // given
        String taskType = "taskType";
        OnceboxProperties.PollingProperties properties = new OnceboxProperties.PollingProperties();
        properties.setType(PollingType.ADAPTIVE);
        properties.setInitialDelay(Duration.ofMinutes(1));
        properties.setMinFixedDelay(Duration.ofSeconds(1));
        properties.setMaxFixedDelay(Duration.ofSeconds(10));
        properties.setMultiplier(2.0);
        
        ScheduledExecutorService executorMock = mock(ScheduledExecutorService.class);

        OnceboxScheduleStrategyListenerSupplier listenerSupplier = mock(OnceboxScheduleStrategyListenerSupplier.class);
        when(listenerSupplier.supply(taskType)).thenReturn(mock(OnceboxScheduleStrategyListener.class));

        // when
        OnceboxScheduleStrategy strategy = OnceboxScheduleStrategyFactory.create(taskType, properties, executorMock, listenerSupplier);

        // then
        assertNotNull(strategy);
        assertTrue(strategy instanceof AdaptiveOnceboxScheduleStrategy,
                "Strategy should be instance of AdaptiveOnceboxScheduleStrategy");
    }

    @Test
    @DisplayName("UT create() should throw IllegalStateException when type is null (unreachable branch)")
    public void create_whenTypeIsNull_shouldThrowException() {
        // given
        OnceboxProperties.PollingProperties properties = new OnceboxProperties.PollingProperties();
        properties.setType(null);
        ScheduledExecutorService executorMock = mock(ScheduledExecutorService.class);

        // when
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> OnceboxScheduleStrategyFactory.create("taskType", properties, executorMock, mock(OnceboxScheduleStrategyListenerSupplier.class)));

        // then
        assertEquals("Reached unreachable branch during creating OnceboxScheduleStrategy", e.getMessage());
    }
}