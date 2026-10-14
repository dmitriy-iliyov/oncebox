package io.github.dmitriyiliyov.oncebox.metrics.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxPropertiesHolder;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Arrays;
import java.util.Objects;

public final class DefaultOutboxMetrics implements OutboxMetrics {

    private final OutboxPropertiesHolder properties;
    private final MeterRegistry registry;
    private final OutboxMetricsService metricsService;
    private final EventStatus [] statuses = new EventStatus[] {EventStatus.PENDING, EventStatus.IN_PROCESS};

    public DefaultOutboxMetrics(OutboxPropertiesHolder properties,
                                MeterRegistry registry,
                                OutboxMetricsService metricsService) {
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
        this.metricsService = Objects.requireNonNull(metricsService, "metricsService cannot be null");
    }

    @Override
    public void register() {
        Gauge.builder("outbox_events", metricsService, OutboxMetricsService::count)
                .description("Total number of outbox events")
                .register(registry);

        Arrays.stream(statuses).forEach(status ->
                        Gauge.builder("outbox_events_by_status", metricsService,
                                        m -> metricsService.countByStatus(status))
                                .description("Number of outbox events by status")
                                .tag("status", status.name().toLowerCase())
                                .register(registry)
                );

        properties.getEventHolders().keySet().forEach(type ->
                Arrays.stream(statuses)
                        .forEach(status ->
                                Gauge.builder("outbox_events_by_event_type_and_status", metricsService,
                                                m -> metricsService.countByEventTypeAndStatus(type, status))
                                        .description("Number of outbox events by type and status")
                                        .tags("event_type", type, "status", status.name().toLowerCase())
                                        .register(registry)
                        )
        );
    }
}
