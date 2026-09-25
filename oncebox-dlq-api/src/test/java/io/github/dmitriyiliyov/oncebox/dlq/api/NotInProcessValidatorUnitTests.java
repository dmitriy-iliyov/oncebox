package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotInProcessValidatorUnitTests {

    private final NotInProcessValidator tested = new NotInProcessValidator();

    @Test
    @DisplayName("UT isValid() when status is IN_PROCESS should reject it")
    void isValid_whenStatusIsInProcess_shouldReject() {
        // when / then
        assertThat(tested.isValid(DlqStatus.IN_PROCESS, null)).isFalse();
    }

    @Test
    @DisplayName("UT isValid() when status is any other constant should accept it")
    void isValid_whenStatusIsNotInProcess_shouldAccept() {
        // when / then
        assertThat(tested.isValid(DlqStatus.MOVED, null)).isTrue();
        assertThat(tested.isValid(DlqStatus.RESOLVED, null)).isTrue();
        assertThat(tested.isValid(DlqStatus.TO_RETRY, null)).isTrue();
    }

    @Test
    @DisplayName("UT isValid() when status is null should leave it to @NotNull")
    void isValid_whenStatusIsNull_shouldAccept() {
        // when / then
        assertThat(tested.isValid(null, null)).isTrue();
    }
}
