package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTask;
import io.github.dmitriyiliyov.oncebox.core.ContinuableTaskDecorator;
import io.github.dmitriyiliyov.oncebox.metrics.ContinuableTaskTimeMeasureDecorator;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContinuableTaskTimeMeasureDecoratorSupplierUnitTests {

    SimpleMeterRegistry registry;
    ContinuableTaskTimeMeasureDecoratorSupplier tested;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        tested = new ContinuableTaskTimeMeasureDecoratorSupplier(registry);
    }

    @Test
    @DisplayName("UT supply() should return ContinuableTaskTimeMeasureDecorator instance")
    void supply_shouldReturnContinuableTaskTimeMeasureDecoratorInstance() {
        ContinuableTaskDecorator decorator = tested.supply("test_task");

        assertInstanceOf(ContinuableTaskTimeMeasureDecorator.class, decorator);
    }

    @Test
    @DisplayName("UT supply() should return new instance on each call")
    void supply_shouldReturnNewInstanceOnEachCall() {
        ContinuableTaskDecorator first  = tested.supply("test_task");
        ContinuableTaskDecorator second = tested.supply("test_task");

        assertNotSame(first, second);
    }

    @Test
    @DisplayName("UT supply() returned decorator should register timer with provided task type tag")
    void supply_returnedDecorator_shouldRegisterTimerWithCorrectTag() throws Exception {
        ContinuableTask decorated = tested.supply("recovery_task").decorate(() -> true);

        decorated.run();

        Timer timer = registry.find("oncebox_task_processing_duration")
                .tag("task_type", "recovery_task")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    @DisplayName("UT supply() decorators with different task types should register independent timers")
    void supply_differentTaskTypes_shouldRegisterIndependentTimers() throws Exception {
        ContinuableTask first  = tested.supply("task_a").decorate(() -> true);
        ContinuableTask second = tested.supply("task_b").decorate(() -> true);

        first.run();
        first.run();
        second.run();

        assertEquals(2, registry.find("oncebox_task_processing_duration")
                .tag("task_type", "task_a").timer().count());
        assertEquals(1, registry.find("oncebox_task_processing_duration")
                .tag("task_type", "task_b").timer().count());
    }
}