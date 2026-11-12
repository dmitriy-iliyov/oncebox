package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.OnceboxScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

public class SchedulersPostApplicationReadyOnceboxInitializer implements PostApplicationReadyOnceboxInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchedulersPostApplicationReadyOnceboxInitializer.class);

    private final Map<String, OnceboxScheduler> schedulers;

    public SchedulersPostApplicationReadyOnceboxInitializer(Map<String, OnceboxScheduler> schedulers) {
        this.schedulers = Objects.requireNonNull(schedulers, "schedulers cannot be null");
    }

    @Override
    public void init() {
        schedulers.values().forEach(OnceboxScheduler::schedule);
        log.debug("Oncebox successfully initialized with schedulers {}", schedulers.keySet());
    }
}
