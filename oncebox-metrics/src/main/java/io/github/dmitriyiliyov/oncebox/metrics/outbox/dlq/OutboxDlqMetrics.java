package io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxPropertiesHolder;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.OutboxMetrics;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Arrays;
import java.util.Objects;

public final class OutboxDlqMetrics implements OutboxMetrics {

    private final OutboxPropertiesHolder properties;
    private final MeterRegistry registry;
    private final OutboxDlqMetricsService metricsService;
    private final DlqStatus [] statuses = new DlqStatus[] {DlqStatus.MOVED, DlqStatus.IN_PROCESS, DlqStatus.TO_RETRY};

    public OutboxDlqMetrics(OutboxPropertiesHolder properties,
                            MeterRegistry registry,
                            OutboxDlqMetricsService metricsService) {
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
        this.metricsService = Objects.requireNonNull(metricsService, "metricsService cannot be null");
    }

    @Override
    public void register() {
        Gauge.builder("outbox_dlq_events", metricsService, OutboxDlqMetricsService::count)
                .description("Total number of outbox DLQ events")
                .register(registry);

        Arrays.stream(statuses).forEach(status ->
                        Gauge.builder("outbox_dlq_events_by_status", metricsService,
                                        m -> metricsService.countByStatus(status))
                                .description("Number of outbox DLQ events by status")
                                .tag("status", status.name().toLowerCase())
                                .register(registry)
                );

        properties.getEventHolders().keySet().forEach(type ->
                Arrays.stream((statuses)).forEach(status ->
                                Gauge.builder("outbox_dlq_events_by_event_type_and_status", metricsService,
                                                m -> metricsService.countByEventTypeAndStatus(type, status))
                                        .description("Number of outbox DLQ events by type and status")
                                        .tags("event_type", type, "status", status.name().toLowerCase())
                                        .register(registry)
                        )
        );
    }
}
