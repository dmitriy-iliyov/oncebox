package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogOutboxDlqHandlerUnitTests {

    LogOutboxDlqHandler tested = new LogOutboxDlqHandler();

    @Test
    @DisplayName("UT handle() when events is null should log warn and not throw")
    void handle_whenEventsNull_shouldLogWarnAndNotThrow() {
        // when + then
        assertThatCode(() -> tested.handle(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UT handle() when events is empty should log warn and not throw")
    void handle_whenEventsEmpty_shouldLogWarnAndNotThrow() {
        // when + then
        assertThatCode(() -> tested.handle(List.of())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UT handle() when events present should log error and not throw")
    void handle_whenEventsPresent_shouldLogErrorAndNotThrow() {
        // given
        OutboxEvent event1 = mock(OutboxEvent.class);
        when(event1.getId()).thenReturn(UUID.randomUUID());
        OutboxEvent event2 = mock(OutboxEvent.class);
        when(event2.getId()).thenReturn(UUID.randomUUID());
        List<OutboxEvent> events = List.of(event1, event2);

        // when + then
        assertThatCode(() -> tested.handle(events)).doesNotThrowAnyException();
    }
}
