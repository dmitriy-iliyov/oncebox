package io.github.dmitriyiliyov.oncebox.core.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventCrossClassEqualityUnitTests {

    private static final UUID ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    @DisplayName("UT equals() when an outbox event and a DLQ event share an id should be false both ways")
    void equals_whenOutboxAndDlqEventShareId_shouldBeFalseBothWays() {
        // given
        OutboxEvent outboxEvent = new OutboxEvent(ID, "TYPE", "JSON", "{}");
        OutboxDlqEvent dlqEvent = new OutboxDlqEvent(ID, EventStatus.FAILED, "TYPE", "JSON", "{}",
                0, NOW, NOW, NOW, DlqStatus.MOVED, NOW);

        // when / then
        assertThat(outboxEvent).isNotEqualTo(dlqEvent);
        assertThat(dlqEvent).isNotEqualTo(outboxEvent);
    }
}
