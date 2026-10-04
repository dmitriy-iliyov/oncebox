package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.OnceboxScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SchedulersPostApplicationReadyOnceboxInitializer implements PostApplicationReadyOnceboxInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchedulersPostApplicationReadyOnceboxInitializer.class);

    private final Map<String, OnceboxScheduler> schedulers;

    public SchedulersPostApplicationReadyOnceboxInitializer(Map<String, OnceboxScheduler> schedulers) {
        this.schedulers = schedulers;
    }

    @Override
    public void init() {
        schedulers.values().forEach(OnceboxScheduler::schedule);
        log.debug("Oncebox successfully initialized with schedulers {}", schedulers.keySet());
    }
}
