package io.github.dmitriyiliyov.oncebox.dlq.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxDlqControllerUnitTests {

    @Test
    @DisplayName("UT constructor when service is null should throw NullPointerException")
    void constructor_whenServiceIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new OutboxDlqController(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("service cannot be null");
    }
}
