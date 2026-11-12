package io.github.dmitriyiliyov.oncebox.core.polling;

import java.time.Duration;

/**
 * Settings of {@link AdaptiveOnceboxScheduleStrategy}: the delay starts at the minimum, is multiplied after
 * every run that returned {@code false} - one that did not fill its batch or failed - never exceeds the maximum,
 * and drops back to the minimum after a run that returned {@code true}.
 */
public interface AdaptivePollingPropertiesHolder {

    /**
     * The initial delay before the first processing.
     */
    Duration getInitialDelay();

    /**
     * The min fixed delay between subsequent processing operations.
     */
    Duration getMinFixedDelay();

    /**
     * The max fixed delay between subsequent processing operations.
     */
    Duration getMaxFixedDelay();

    /**
     * The factor the delay is multiplied by after a run that returned {@code false}.
     */
    Double getMultiplier();
}
