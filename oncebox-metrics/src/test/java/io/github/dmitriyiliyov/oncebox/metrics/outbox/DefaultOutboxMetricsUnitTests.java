package io.github.dmitriyiliyov.oncebox.metrics.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxPropertiesHolder;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultOutboxMetricsUnitTests {

    @Mock
    OutboxMetricsService metricsService;

    @Mock
    OutboxPropertiesHolder properties;

    SimpleMeterRegistry registry;

    DefaultOutboxMetrics tested;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        Map<String, OutboxPropertiesHolder.EventPropertiesHolder> eventProps = Map.of(
                "test-event", mock(OutboxPropertiesHolder.EventPropertiesHolder.class)
        );
        lenient().when(properties.getEventHolders()).thenReturn(eventProps);
        tested = new DefaultOutboxMetrics(properties, registry, metricsService);
    }

    @Test
    @DisplayName("UT constructor should throw NPE when properties is null")
    void constructor_shouldThrowNPE_whenPropertiesIsNull() {
        assertThatThrownBy(() -> new DefaultOutboxMetrics(null, registry, metricsService))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("UT constructor should throw NPE when registry is null")
    void constructor_shouldThrowNPE_whenRegistryIsNull() {
        assertThatThrownBy(() -> new DefaultOutboxMetrics(properties, null, metricsService))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("UT constructor should throw NPE when metricsService is null")
    void constructor_shouldThrowNPE_whenMetricsServiceIsNull() {
        assertThatThrownBy(() -> new DefaultOutboxMetrics(properties, registry, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("UT register() should register total events gauge")
    void register_shouldRegisterTotalEventsGauge() {
        // given
        when(metricsService.count()).thenReturn(10L);

        // when
        tested.register();

        // then
        Gauge gauge = registry.find("outbox_events").gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("UT register() should register events by status gauges")
    void register_shouldRegisterEventsByStatusGauges() {
        // given
        when(metricsService.countByStatus(EventStatus.PENDING)).thenReturn(5L);
        when(metricsService.countByStatus(EventStatus.IN_PROCESS)).thenReturn(3L);

        // when
        tested.register();

        // then
        Gauge pendingGauge = registry.find("outbox_events_by_status")
                .tag("status", "pending")
                .gauge();
        assertThat(pendingGauge).isNotNull();
        assertThat(pendingGauge.value()).isEqualTo(5.0);

        Gauge inProcessGauge = registry.find("outbox_events_by_status")
                .tag("status", "in_process")
                .gauge();
        assertThat(inProcessGauge).isNotNull();
        assertThat(inProcessGauge.value()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("UT register() should register events by type and status gauges")
    void register_shouldRegisterEventsByTypeAndStatusGauges() {
        // given
        when(metricsService.countByEventTypeAndStatus("test-event", EventStatus.PENDING)).thenReturn(2L);
        when(metricsService.countByEventTypeAndStatus("test-event", EventStatus.IN_PROCESS)).thenReturn(1L);

        // when
        tested.register();

        // then
        Gauge pendingGauge = registry.find("outbox_events_by_event_type_and_status")
                .tag("event_type", "test-event")
                .tag("status", "pending")
                .gauge();
        assertThat(pendingGauge).isNotNull();
        assertThat(pendingGauge.value()).isEqualTo(2.0);

        Gauge inProcessGauge = registry.find("outbox_events_by_event_type_and_status")
                .tag("event_type", "test-event")
                .tag("status", "in_process")
                .gauge();
        assertThat(inProcessGauge).isNotNull();
        assertThat(inProcessGauge.value()).isEqualTo(1.0);
    }
}
