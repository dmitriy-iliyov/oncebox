package io.github.dmitriyiliyov.oncebox.core.publisher;

import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The header names are wire contract between a publisher and its consumers, which may run another version.
 */
class OutboxHeadersUnitTests {

    @Test
    @DisplayName("UT getValue() should keep the header names consumers read")
    void getValue_shouldKeepWireHeaderNames() {
        // when / then
        assertThat(OutboxHeaders.EVENT_TYPE.getValue()).isEqualTo("outbox_event_type");
        assertThat(OutboxHeaders.EVENT_ID.getValue()).isEqualTo("outbox_event_id");
        assertThat(OutboxHeaders.EVENT_PAYLOAD_TYPE.getValue()).isEqualTo("outbox_event_payload_type");
    }
}
