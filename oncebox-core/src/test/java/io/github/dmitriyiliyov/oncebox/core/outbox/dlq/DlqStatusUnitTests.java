package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DlqStatusUnitTests {

    @Test
    @DisplayName("UT fromString() when value is null should throw NullPointerException")
    void fromString_whenValueIsNull_shouldThrowNullPointerException() {
        // given
        String value = null;

        // when + then
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> DlqStatus.fromString(value));
        assertThat(ex.getMessage()).contains("value cannot be null");
    }

    @Test
    @DisplayName("UT fromString() when value is valid lowercase should return corresponding enum")
    void fromString_whenValueIsValidLowercase_shouldReturnEnum() {
        // given
        String value = "moved";

        // when
        DlqStatus result = DlqStatus.fromString(value);

        // then
        assertThat(result).isEqualTo(DlqStatus.MOVED);
    }

    @Test
    @DisplayName("UT fromString() when value is valid uppercase should return corresponding enum")
    void fromString_whenValueIsValidUppercase_shouldReturnEnum() {
        // given
        String value = "IN_PROCESS";

        // when
        DlqStatus result = DlqStatus.fromString(value);

        // then
        assertThat(result).isEqualTo(DlqStatus.IN_PROCESS);
    }

    @Test
    @DisplayName("UT fromString() when value is valid mixed case should return corresponding enum")
    void fromString_whenValueIsValidMixedCase_shouldReturnEnum() {
        // given
        String value = "To_Retry";

        // when
        DlqStatus result = DlqStatus.fromString(value);

        // then
        assertThat(result).isEqualTo(DlqStatus.TO_RETRY);
    }

    @Test
    @DisplayName("UT fromString() when value is RESOLVED should return RESOLVED enum")
    void fromString_whenValueIsResolved_shouldReturnResolvedEnum() {
        // given
        String value = "resolved";

        // when
        DlqStatus result = DlqStatus.fromString(value);

        // then
        assertThat(result).isEqualTo(DlqStatus.RESOLVED);
    }

    @Test
    @DisplayName("UT fromString() when value is invalid should throw IllegalArgumentException")
    void fromString_whenValueIsInvalid_shouldThrowException() {
        // given
        String value = "unknown";

        // when + then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DlqStatus.fromString(value));
        assertThat(ex.getMessage()).contains("Unknown DlqStatus");
    }

    @Test
    @DisplayName("UT fromString() when value is empty should throw IllegalArgumentException")
    void fromString_whenValueIsEmpty_shouldThrowException() {
        // given
        String value = "";

        // when + then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> DlqStatus.fromString(value));
        assertThat(ex.getMessage()).contains("Unknown DlqStatus");
    }
}