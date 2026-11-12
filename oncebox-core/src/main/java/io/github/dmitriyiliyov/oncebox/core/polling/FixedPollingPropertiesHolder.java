package io.github.dmitriyiliyov.oncebox.core.polling;

import java.time.Duration;

/**
 * Settings of {@link FixedOnceboxScheduleStrategy}: runs follow each other at the same delay, whatever they
 * found.
 */
public interface FixedPollingPropertiesHolder {

    /**
     * The initial delay before the first processing.
     */
    Duration getInitialDelay();

    /**
     * The fixed delay between subsequent processing operations.
     */
    Duration getFixedDelay();
}

