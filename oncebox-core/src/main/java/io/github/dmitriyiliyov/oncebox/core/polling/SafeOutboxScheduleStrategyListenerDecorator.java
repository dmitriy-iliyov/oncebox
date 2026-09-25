package io.github.dmitriyiliyov.oncebox.core.polling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A listener whose failure stays its own: an exception thrown by the delegate is logged and dropped, so it neither
 * counts as a failure of the task nor keeps the next execution from being scheduled. The schedule strategies wrap
 * every listener they are given in it.
 */
public class SafeOutboxScheduleStrategyListenerDecorator implements OutboxScheduleStrategyListener {

    private static final Logger log = LoggerFactory.getLogger(SafeOutboxScheduleStrategyListenerDecorator.class);
    private final OutboxScheduleStrategyListener delegate;

    public SafeOutboxScheduleStrategyListenerDecorator(OutboxScheduleStrategyListener delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    }

    @Override
    public void onExecutionStarted() {
        wrap(delegate::onExecutionStarted);
    }

    @Override
    public void onExecutionSkipped() {
        wrap(delegate::onExecutionSkipped);
    }

    @Override
    public void onExecutionSucceeded() {
        wrap(delegate::onExecutionSucceeded);
    }

    @Override
    public void onExecutionFailed() {
        wrap(delegate::onExecutionFailed);
    }

    @Override
    public void onDelayChanged(long delay) {
        wrap(() -> delegate.onDelayChanged(delay));
    }

    private void wrap(Runnable call) {
        try {
            call.run();
        } catch (Throwable t) {
            log.error("Schedule strategy listener failed", t);
        }
    }
}
