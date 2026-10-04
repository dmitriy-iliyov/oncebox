package io.github.dmitriyiliyov.oncebox.starter;

import java.util.concurrent.TimeUnit;

/**
 * Constants used when shutting down the oncebox ScheduledExecutorService.
 */
public final class OnceboxScheduledExecutorServiceConstants {

    static final long TIMEOUT = 60;
    static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    private OnceboxScheduledExecutorServiceConstants() {}
}
