package io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxPropertiesHolder;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OutboxDlqMetricsUnitTests {

    @Mock
    OutboxDlqMetricsService metricsService;

    @Mock
    OutboxPropertiesHolder properties;

    SimpleMeterRegistry registry;

    OutboxDlqMetrics tested;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        Map<String, OutboxPropertiesHolder.EventPropertiesHolder> eventProps = Map.of(
                "test-event", Mockito.mock(OutboxPropertiesHolder.EventPropertiesHolder.class)
        );
        lenient().when(properties.getEventHolders()).thenReturn(eventProps);
        tested = new OutboxDlqMetrics(properties, registry, metricsService);
    }

    @Test
    @DisplayName("UT constructor should throw NPE when properties is null")
    void constructor_shouldThrowNPE_whenPropertiesIsNull() {
        assertThrows(NullPointerException.class, () -> new OutboxDlqMetrics(null, registry, metricsService));
    }

    @Test
    @DisplayName("UT constructor should throw NPE when registry is null")
    void constructor_shouldThrowNPE_whenRegistryIsNull() {
        assertThrows(NullPointerException.class, () -> new OutboxDlqMetrics(properties, null, metricsService));
    }

    @Test
    @DisplayName("UT constructor should throw NPE when metricsService is null")
    void constructor_shouldThrowNPE_whenMetricsServiceIsNull() {
        assertThrows(NullPointerException.class, () -> new OutboxDlqMetrics(properties, registry, null));
    }

    @Test
    @DisplayName("UT register() should register total events gauge")
    void register_shouldRegisterTotalEventsGauge() {
        // given
        Mockito.when(metricsService.count()).thenReturn(15L);

        // when
        tested.register();

        // then
        Gauge gauge = registry.find("outbox_dlq_events").gauge();
        Assertions.assertNotNull(gauge);
        Assertions.assertEquals(15.0, gauge.value());
    }

    @Test
    @DisplayName("UT register() should register events by status gauges")
    void register_shouldRegisterEventsByStatusGauges() {
        // given
        Mockito.when(metricsService.countByStatus(DlqStatus.MOVED)).thenReturn(7L);
        Mockito.when(metricsService.countByStatus(DlqStatus.IN_PROCESS)).thenReturn(4L);
        Mockito.when(metricsService.countByStatus(DlqStatus.TO_RETRY)).thenReturn(2L);

        // when
        tested.register();

        // then
        Gauge movedGauge = registry.find("outbox_dlq_events_by_status")
                .tag("status", "moved")
                .gauge();
        Assertions.assertNotNull(movedGauge);
        Assertions.assertEquals(7.0, movedGauge.value());

        Gauge inProcessGauge = registry.find("outbox_dlq_events_by_status")
                .tag("status", "in_process")
                .gauge();
        Assertions.assertNotNull(inProcessGauge);
        Assertions.assertEquals(4.0, inProcessGauge.value());

        Gauge toRetryGauge = registry.find("outbox_dlq_events_by_status")
                .tag("status", "to_retry")
                .gauge();
        Assertions.assertNotNull(toRetryGauge);
        Assertions.assertEquals(2.0, toRetryGauge.value());
    }

    @Test
    @DisplayName("UT register() should register events by type and status gauges")
    void register_shouldRegisterEventsByTypeAndStatusGauges() {
        // given
        Mockito.when(metricsService.countByEventTypeAndStatus("test-event", DlqStatus.MOVED)).thenReturn(3L);
        Mockito.when(metricsService.countByEventTypeAndStatus("test-event", DlqStatus.IN_PROCESS)).thenReturn(2L);
        Mockito.when(metricsService.countByEventTypeAndStatus("test-event", DlqStatus.TO_RETRY)).thenReturn(1L);

        // when
        tested.register();

        // then
        Gauge movedGauge = registry.find("outbox_dlq_events_by_event_type_and_status")
                .tag("event_type", "test-event")
                .tag("status", "moved")
                .gauge();
        Assertions.assertNotNull(movedGauge);
        Assertions.assertEquals(3.0, movedGauge.value());

        Gauge inProcessGauge = registry.find("outbox_dlq_events_by_event_type_and_status")
                .tag("event_type", "test-event")
                .tag("status", "in_process")
                .gauge();
        Assertions.assertNotNull(inProcessGauge);
        Assertions.assertEquals(2.0, inProcessGauge.value());

        Gauge toRetryGauge = registry.find("outbox_dlq_events_by_event_type_and_status")
                .tag("event_type", "test-event")
                .tag("status", "to_retry")
                .gauge();
        Assertions.assertNotNull(toRetryGauge);
        Assertions.assertEquals(1.0, toRetryGauge.value());
    }
}
