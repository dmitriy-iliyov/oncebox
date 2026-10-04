package io.github.dmitriyiliyov.oncebox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompositePostApplicationReadyOnceboxInitializerUnitTests {

    @Test
    @DisplayName("UT init() when ONLY outbox is enabled should log outbox properties")
    void init_whenOnlyOutboxEnabled_shouldLogOutboxProperties() {
        OnceboxProperties properties = mock(OnceboxProperties.class, RETURNS_DEEP_STUBS);
        when(properties.getOutbox().isEnabled()).thenReturn(true);
        when(properties.getInbox().isEnabled()).thenReturn(false);
        when(properties.toStringWithOutbox()).thenReturn("outbox-props");

        CompositePostApplicationReadyOnceboxInitializer composite =
                new CompositePostApplicationReadyOnceboxInitializer(properties, Collections.emptyList());

        composite.init();

        verify(properties, times(1)).toStringWithOutbox();
        verify(properties, never()).toStringWithInbox();
    }

    @Test
    @DisplayName("UT init() when ONLY inbox is enabled should log inbox properties")
    void init_whenOnlyInboxEnabled_shouldLogInboxProperties() {
        OnceboxProperties properties = mock(OnceboxProperties.class, RETURNS_DEEP_STUBS);
        when(properties.getOutbox().isEnabled()).thenReturn(false);
        when(properties.getInbox().isEnabled()).thenReturn(true);
        when(properties.toStringWithInbox()).thenReturn("inbox-props");

        CompositePostApplicationReadyOnceboxInitializer composite =
                new CompositePostApplicationReadyOnceboxInitializer(properties, Collections.emptyList());

        composite.init();

        verify(properties, never()).toStringWithOutbox();
        verify(properties, times(1)).toStringWithInbox();
    }

    @Test
    @DisplayName("UT init() when BOTH are enabled should log base properties")
    void init_whenBothEnabled_shouldLogBaseProperties() {
        OnceboxProperties properties = mock(OnceboxProperties.class, RETURNS_DEEP_STUBS);
        when(properties.getOutbox().isEnabled()).thenReturn(true);
        when(properties.getInbox().isEnabled()).thenReturn(true);

        CompositePostApplicationReadyOnceboxInitializer composite =
                new CompositePostApplicationReadyOnceboxInitializer(properties, Collections.emptyList());

        composite.init();

        verify(properties, never()).toStringWithOutbox();
        verify(properties, never()).toStringWithInbox();
    }

    @Test
    @DisplayName("UT init() when BOTH are disabled should log base properties")
    void init_whenBothDisabled_shouldLogBaseProperties() {
        OnceboxProperties properties = mock(OnceboxProperties.class, RETURNS_DEEP_STUBS);
        when(properties.getOutbox().isEnabled()).thenReturn(false);
        when(properties.getInbox().isEnabled()).thenReturn(false);

        CompositePostApplicationReadyOnceboxInitializer composite =
                new CompositePostApplicationReadyOnceboxInitializer(properties, Collections.emptyList());

        composite.init();

        verify(properties, never()).toStringWithOutbox();
        verify(properties, never()).toStringWithInbox();
    }
}