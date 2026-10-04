package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.metrics.outbox.OutboxMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MetricsPostApplicationReadyOnceboxInitializer implements PostApplicationReadyOnceboxInitializer {

    private static final Logger log = LoggerFactory.getLogger(MetricsPostApplicationReadyOnceboxInitializer.class);

    private final Map<String, OutboxMetrics> metrics;

    public MetricsPostApplicationReadyOnceboxInitializer(Map<String, OutboxMetrics> metrics) {
        this.metrics = metrics;
    }

    @Override
    public void init() {
        metrics.values().forEach(OutboxMetrics::register);
        log.debug("Oncebox successfully initialized with metrics observers {}", metrics.keySet());
    }
}
