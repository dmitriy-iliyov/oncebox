package io.github.dmitriyiliyov.oncebox.core.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.domain.OutboxEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class OutboxEventCrossClassEqualityUnitTests {

    @Test
    @DisplayName("UT OutboxEvent and OutboxDlqEvent should never be equal even with identical IDs")
    void outboxEvent_and_outboxDlqEvent_areNeverEqual() {
        UUID sharedId = UUID.randomUUID();
        Instant now = Instant.now();

        OutboxEvent standardEvent = new OutboxEvent(
                sharedId, "TYPE", "JSON", "{}", now
        );

        OutboxDlqEvent dlqEvent = new OutboxDlqEvent(
                sharedId, EventStatus.FAILED, "TYPE", "JSON", "{}",
                0, now, now, now, Mockito.mock(DlqStatus.class), now
        );

        assertNotEquals(standardEvent, dlqEvent);
        assertNotEquals(dlqEvent, standardEvent);

        assertFalse(standardEvent.equals(dlqEvent));
        assertFalse(dlqEvent.equals(standardEvent));
    }
}
