package io.github.dmitriyiliyov.oncebox.core.polling;

/**
 * The settings of both strategies in one type, so that one configuration object feeds whichever strategy is chosen.
 */
public interface PollingPropertiesHolder extends AdaptivePollingPropertiesHolder, FixedPollingPropertiesHolder { }
