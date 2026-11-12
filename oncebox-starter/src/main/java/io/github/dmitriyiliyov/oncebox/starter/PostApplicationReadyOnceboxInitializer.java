package io.github.dmitriyiliyov.oncebox.starter;

/**
 * Work oncebox defers until the application is ready, such as starting its schedulers, so that no background
 * task runs against a context that is still starting.
 */
public interface PostApplicationReadyOnceboxInitializer {
    void init();
}
