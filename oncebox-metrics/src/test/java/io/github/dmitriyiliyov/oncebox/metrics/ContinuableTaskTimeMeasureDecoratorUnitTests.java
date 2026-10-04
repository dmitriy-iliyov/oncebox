package io.github.dmitriyiliyov.oncebox.metrics;

import io.github.dmitriyiliyov.oncebox.core.ContinuableTask;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContinuableTaskTimeMeasureDecoratorUnitTests {

    SimpleMeterRegistry registry;
    ContinuableTaskTimeMeasureDecorator tested;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        tested = new ContinuableTaskTimeMeasureDecorator(registry, "test_task");
    }

    @Test
    @DisplayName("UT constructor should throw NPE when registry is null")
    void constructor_shouldThrowNPE_whenRegistryIsNull() {
        assertThrows(NullPointerException.class, () -> new ContinuableTaskTimeMeasureDecorator(null, "test_task"));
    }

    @Test
    @DisplayName("UT constructor should throw NPE when taskType is null")
    void constructor_shouldThrowNPE_whenTaskTypeIsNull() {
        assertThrows(NullPointerException.class, () -> new ContinuableTaskTimeMeasureDecorator(registry, null));
    }

    @Test
    @DisplayName("UT decorate() when task returns true, decorated task should return true")
    void decorate_whenTaskReturnsTrue_shouldReturnTrue() throws Exception {
        ContinuableTask decorated = tested.decorate(() -> true);

        assertTrue(decorated.run());
    }

    @Test
    @DisplayName("UT decorate() when task returns false, decorated task should return false")
    void decorate_whenTaskReturnsFalse_shouldReturnFalse() throws Exception {
        ContinuableTask decorated = tested.decorate(() -> false);

        assertFalse(decorated.run());
    }

    @Test
    @DisplayName("UT decorate() after task run, timer should record exactly one measurement")
    void decorate_afterRun_timerShouldRecordOneMeasurement() throws Exception {
        ContinuableTask decorated = tested.decorate(() -> true);

        decorated.run();

        Timer timer = registry.find("oncebox_task_processing_duration")
                .tag("task_type", "test_task")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    @DisplayName("UT decorate() after multiple runs, timer should record each execution")
    void decorate_afterMultipleRuns_timerShouldRecordEachExecution() throws Exception {
        ContinuableTask decorated = tested.decorate(() -> true);

        decorated.run();
        decorated.run();
        decorated.run();

        Timer timer = registry.find("oncebox_task_processing_duration")
                .tag("task_type", "test_task")
                .timer();
        assertNotNull(timer);
        assertEquals(3, timer.count());
    }

    @Test
    @DisplayName("UT decorate() timer should be registered with correct task_type tag")
    void decorate_timerShouldBeRegisteredWithCorrectTag() throws Exception {
        tested = new ContinuableTaskTimeMeasureDecorator(registry, "recovery_task");
        ContinuableTask decorated = tested.decorate(() -> true);

        decorated.run();

        Timer timer = registry.find("oncebox_task_processing_duration")
                .tag("task_type", "recovery_task")
                .timer();
        assertNotNull(timer);
    }

    @Test
    @DisplayName("UT decorate() when task throws, exception should be rethrown")
    void decorate_whenTaskThrows_exceptionShouldBeRethrown() {
        ContinuableTask decorated = tested.decorate(() -> { throw new RuntimeException("failure"); });

        assertThrows(RuntimeException.class, decorated::run);
    }

    @Test
    @DisplayName("UT decorate() when task throws, timer should still record the execution")
    void decorate_whenTaskThrows_timerShouldStillRecord() {
        ContinuableTask decorated = tested.decorate(() -> { throw new RuntimeException("failure"); });

        assertThrows(RuntimeException.class, decorated::run);

        Timer timer = registry.find("oncebox_task_processing_duration")
                .tag("task_type", "test_task")
                .timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }
}
