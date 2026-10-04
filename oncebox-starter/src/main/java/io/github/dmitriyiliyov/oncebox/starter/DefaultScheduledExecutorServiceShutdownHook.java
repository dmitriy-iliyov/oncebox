package io.github.dmitriyiliyov.oncebox.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

import static io.github.dmitriyiliyov.oncebox.starter.OnceboxScheduledExecutorServiceConstants.TIMEOUT;
import static io.github.dmitriyiliyov.oncebox.starter.OnceboxScheduledExecutorServiceConstants.TIME_UNIT;

public class DefaultScheduledExecutorServiceShutdownHook implements ScheduledExecutorServiceShutdownHook {

    private static final Logger log = LoggerFactory.getLogger(DefaultScheduledExecutorServiceShutdownHook.class);
    private final ScheduledExecutorService shutdownTarget;

    public DefaultScheduledExecutorServiceShutdownHook(ScheduledExecutorService shutdownTarget) {
        this.shutdownTarget = Objects.requireNonNull(shutdownTarget, "shutdownTarget cannot be null");
    }

    @Override
    public void shutdown() {
        shutdownTarget.shutdown();
        try {
            if (shutdownTarget.awaitTermination(TIMEOUT, TIME_UNIT)) {
                log.debug("Oncebox's ScheduledExecutorService shutdown gracefully");
            } else {
                log.warn(
                        "Oncebox's ScheduledExecutorService didn't terminate within {} {}, forcing shutdown",
                        TIMEOUT, TIME_UNIT
                );
                shutdownTarget.shutdownNow();
            }
        } catch (InterruptedException e) {
            shutdownTarget.shutdownNow();
            log.error("Oncebox's ScheduledExecutorService shutdown was interrupted, forcing shutdown", e);
            Thread.currentThread().interrupt();
        }
    }
}
