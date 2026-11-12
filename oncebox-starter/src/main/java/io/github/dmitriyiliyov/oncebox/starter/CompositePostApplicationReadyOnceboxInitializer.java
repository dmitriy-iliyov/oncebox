package io.github.dmitriyiliyov.oncebox.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.Objects;

public class CompositePostApplicationReadyOnceboxInitializer implements PostApplicationReadyOnceboxInitializer {

    private static final Logger log = LoggerFactory.getLogger(CompositePostApplicationReadyOnceboxInitializer.class);

    private final OnceboxProperties properties;
    private final List<PostApplicationReadyOnceboxInitializer> initializers;

    public CompositePostApplicationReadyOnceboxInitializer(OnceboxProperties properties,
                                                          List<PostApplicationReadyOnceboxInitializer> initializers) {
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
        this.initializers = Objects.requireNonNull(initializers, "initializers cannot be null");
    }

    @EventListener(ApplicationReadyEvent.class)
    @Override
    public void init() {
        initializers.forEach(PostApplicationReadyOnceboxInitializer::init);

        boolean isOutboxEnabled = properties.getOutbox().isEnabled();
        boolean isInboxEnabled = properties.getInbox().isEnabled();

        if (isOutboxEnabled && !isInboxEnabled) {
            log.debug(LogUtils.prettyPrint(properties.toStringWithOutbox()));
        } else if (!isOutboxEnabled && isInboxEnabled) {
            log.debug(LogUtils.prettyPrint(properties.toStringWithInbox()));
        } else {
            log.debug(LogUtils.prettyPrint(properties));
        }
    }
}
