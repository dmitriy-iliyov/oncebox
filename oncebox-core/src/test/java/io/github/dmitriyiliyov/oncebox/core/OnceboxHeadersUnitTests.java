package io.github.dmitriyiliyov.oncebox.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The header names are wire contract between a publisher and its consumers, which may run another version.
 */
class OnceboxHeadersUnitTests {

    @Test
    @DisplayName("UT getValue() should keep the header names consumers read")
    void getValue_shouldKeepWireHeaderNames() {
        // when / then
        assertThat(OnceboxHeaders.EVENT_TYPE.getValue()).isEqualTo("oncebox_event_type");
        assertThat(OnceboxHeaders.EVENT_ID.getValue()).isEqualTo("oncebox_event_id");
        assertThat(OnceboxHeaders.EVENT_PAYLOAD_TYPE.getValue()).isEqualTo("oncebox_event_payload_type");
    }
}
