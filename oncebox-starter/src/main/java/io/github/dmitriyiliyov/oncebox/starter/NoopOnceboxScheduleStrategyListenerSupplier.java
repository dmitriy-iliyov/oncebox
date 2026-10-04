package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.polling.OnceboxScheduleStrategyListener;

public class NoopOnceboxScheduleStrategyListenerSupplier implements OnceboxScheduleStrategyListenerSupplier {
    @Override
    public OnceboxScheduleStrategyListener supply(String taskType) {
        return OnceboxScheduleStrategyListener.NOOP;
    }
}
