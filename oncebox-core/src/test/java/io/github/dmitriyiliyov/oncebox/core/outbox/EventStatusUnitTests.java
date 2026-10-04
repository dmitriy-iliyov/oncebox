package io.github.dmitriyiliyov.oncebox.core.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.domain.EventStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventStatusUnitTests {

    @Test
    @DisplayName("UT fromString() when value is null should return PENDING")
    void fromString_whenValueIsNull_shouldReturnPending() {
        // given
        String value = null;

        // when
        EventStatus result = EventStatus.fromString(value);

        // then
        assertThat(result).isEqualTo(EventStatus.PENDING);
    }

    @Test
    @DisplayName("UT fromString() when value is valid lowercase should return corresponding enum")
    void fromString_whenValueIsValidLowercase_shouldReturnEnum() {
        // given
        String value = "processed";

        // when
        EventStatus result = EventStatus.fromString(value);

        // then
        assertThat(result).isEqualTo(EventStatus.PROCESSED);
    }

    @Test
    @DisplayName("UT fromString() when value is valid uppercase should return corresponding enum")
    void fromString_whenValueIsValidUppercase_shouldReturnEnum() {
        // given
        String value = "FAILED";

        // when
        EventStatus result = EventStatus.fromString(value);

        // then
        assertThat(result).isEqualTo(EventStatus.FAILED);
    }

    @Test
    @DisplayName("UT fromString() when value is invalid should throw IllegalArgumentException")
    void fromString_whenValueIsInvalid_shouldThrowException() {
        // given
        String value = "unknown";

        // when + then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> EventStatus.fromString(value));
        assertThat(ex.getMessage()).contains("Unknown EventStatus: unknown");
    }
}
