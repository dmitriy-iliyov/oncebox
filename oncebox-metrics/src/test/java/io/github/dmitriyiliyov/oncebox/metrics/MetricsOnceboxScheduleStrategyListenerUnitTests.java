package io.github.dmitriyiliyov.oncebox.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsOnceboxScheduleStrategyListenerUnitTests {

    SimpleMeterRegistry registry;
    MetricsOnceboxScheduleStrategyListener tested;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        tested = new MetricsOnceboxScheduleStrategyListener("test_task", registry);
    }

    private Counter findCounter(String name) {
        return registry.find(name).tag("task_type", "test_task").counter();
    }

    private Gauge findGauge(String name) {
        return registry.find(name).tag("task_type", "test_task").gauge();
    }

    @Test
    @DisplayName("UT constructor should throw NPE when taskType is null")
    void constructor_shouldThrowNPE_whenTaskTypeIsNull() {
        assertThrows(NullPointerException.class, () -> new MetricsOnceboxScheduleStrategyListener(null, registry));
    }

    @Test
    @DisplayName("UT constructor should throw IAE when taskType is empty")
    void constructor_shouldThrowIAE_whenTaskTypeIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new MetricsOnceboxScheduleStrategyListener("", registry));
    }

    @Test
    @DisplayName("UT constructor should throw IAE when taskType is blank")
    void constructor_shouldThrowIAE_whenTaskTypeIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new MetricsOnceboxScheduleStrategyListener("   ", registry));
    }

    @Test
    @DisplayName("UT constructor should throw NPE when registry is null")
    void constructor_shouldThrowNPE_whenRegistryIsNull() {
        assertThrows(NullPointerException.class, () -> new MetricsOnceboxScheduleStrategyListener("test_task", null));
    }

    @Test
    @DisplayName("UT constructor should register all counters and gauge with correct task_type tag")
    void constructor_shouldRegisterAllMetrics() {
        assertNotNull(findCounter("oncebox_started_tasks"));
        assertNotNull(findCounter("oncebox_skipped_tasks"));
        assertNotNull(findCounter("oncebox_succeeded_tasks"));
        assertNotNull(findCounter("oncebox_failed_tasks"));
        assertNotNull(findGauge("oncebox_polling_delay"));
    }

    @Test
    @DisplayName("UT onExecutionStarted() should increment started counter")
    void onExecutionStarted_shouldIncrementStartedCounter() {
        tested.onExecutionStarted();
        tested.onExecutionStarted();

        assertEquals(2, findCounter("oncebox_started_tasks").count());
    }

    @Test
    @DisplayName("UT onExecutionSkipped() should increment skipped counter")
    void onExecutionSkipped_shouldIncrementSkippedCounter() {
        tested.onExecutionSkipped();

        assertEquals(1, findCounter("oncebox_skipped_tasks").count());
    }

    @Test
    @DisplayName("UT onExecutionSucceeded() should increment succeeded counter")
    void onExecutionSucceeded_shouldIncrementSucceededCounter() {
        tested.onExecutionSucceeded();

        assertEquals(1, findCounter("oncebox_succeeded_tasks").count());
    }

    @Test
    @DisplayName("UT onExecutionFailed() should increment failed counter")
    void onExecutionFailed_shouldIncrementFailedCounter() {
        tested.onExecutionFailed();

        assertEquals(1, findCounter("oncebox_failed_tasks").count());
    }

    @Test
    @DisplayName("UT counters should be independent of each other")
    void counters_shouldBeIndependentOfEachOther() {
        tested.onExecutionStarted();
        tested.onExecutionSkipped();
        tested.onExecutionSucceeded();
        tested.onExecutionFailed();

        assertEquals(1, findCounter("oncebox_started_tasks").count());
        assertEquals(1, findCounter("oncebox_skipped_tasks").count());
        assertEquals(1, findCounter("oncebox_succeeded_tasks").count());
        assertEquals(1, findCounter("oncebox_failed_tasks").count());
    }

    @Test
    @DisplayName("UT onDelayChanged() should update gauge to new value")
    void onDelayChanged_shouldUpdateGauge() {
        tested.onDelayChanged(500L);

        assertEquals(500.0, findGauge("oncebox_polling_delay").value());
    }

    @Test
    @DisplayName("UT onDelayChanged() should reflect latest value after multiple updates")
    void onDelayChanged_shouldReflectLatestValue() {
        tested.onDelayChanged(500L);
        tested.onDelayChanged(1000L);
        tested.onDelayChanged(250L);

        assertEquals(250.0, findGauge("oncebox_polling_delay").value());
    }

    @Test
    @DisplayName("UT onDelayChanged() initial gauge value should be zero")
    void gauge_initialValueShouldBeZero() {
        assertEquals(0.0, findGauge("oncebox_polling_delay").value());
    }

    @Test
    @DisplayName("UT two listeners with different task types should not share metrics")
    void twoListeners_withDifferentTaskTypes_shouldNotShareMetrics() {
        MetricsOnceboxScheduleStrategyListener other =
                new MetricsOnceboxScheduleStrategyListener("other_task", registry);

        tested.onExecutionStarted();
        tested.onExecutionStarted();
        other.onExecutionStarted();

        double testedCount = registry.find("oncebox_started_tasks")
                .tag("task_type", "test_task").counter().count();
        double otherCount = registry.find("oncebox_started_tasks")
                .tag("task_type", "other_task").counter().count();

        assertEquals(2, testedCount);
        assertEquals(1, otherCount);
    }
}
