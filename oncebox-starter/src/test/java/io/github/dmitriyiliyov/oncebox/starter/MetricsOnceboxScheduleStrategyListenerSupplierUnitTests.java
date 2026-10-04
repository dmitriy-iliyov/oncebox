package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategyListener;
import io.github.dmitriyiliyov.oncebox.metrics.MetricsOnceboxScheduleStrategyListener;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsOnceboxScheduleStrategyListenerSupplierUnitTests {

    SimpleMeterRegistry registry;
    MetricsOnceboxScheduleStrategyListenerSupplier tested;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        tested = new MetricsOnceboxScheduleStrategyListenerSupplier(registry);
    }

    @Test
    @DisplayName("UT supply() should return MetricsOnceboxScheduleStrategyListener instance")
    void supply_shouldReturnMetricsOnceboxScheduleStrategyListenerInstance() {
        OnceboxScheduleStrategyListener listener = tested.supply("test_task");

        assertInstanceOf(MetricsOnceboxScheduleStrategyListener.class, listener);
    }

    @Test
    @DisplayName("UT supply() should return new instance on each call")
    void supply_shouldReturnNewInstanceOnEachCall() {
        OnceboxScheduleStrategyListener first  = tested.supply("test_task");
        OnceboxScheduleStrategyListener second = tested.supply("test_task");

        assertNotSame(first, second);
    }

    @Test
    @DisplayName("UT supply() returned listener should register metrics with provided task type tag")
    void supply_returnedListener_shouldRegisterMetricsWithCorrectTag() {
        OnceboxScheduleStrategyListener listener = tested.supply("recovery_task");

        listener.onExecutionStarted();

        Counter counter = registry.find("oncebox_started_tasks")
                .tag("task_type", "recovery_task")
                .counter();
        assertNotNull(counter);
        assertEquals(1, counter.count());
    }

    @Test
    @DisplayName("UT supply() listeners with different task types should register independent metrics")
    void supply_differentTaskTypes_shouldRegisterIndependentMetrics() {
        OnceboxScheduleStrategyListener first  = tested.supply("task_a");
        OnceboxScheduleStrategyListener second = tested.supply("task_b");

        first.onExecutionStarted();
        first.onExecutionStarted();
        second.onExecutionStarted();

        assertEquals(2, registry.find("oncebox_started_tasks")
                .tag("task_type", "task_a").counter().count());
        assertEquals(1, registry.find("oncebox_started_tasks")
                .tag("task_type", "task_b").counter().count());
    }
}