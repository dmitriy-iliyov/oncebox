package io.github.dmitriyiliyov.oncebox.core.polling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SafeOutboxScheduleStrategyListenerDecoratorUnitTests {

    private static final RuntimeException LISTENER_FAILURE = new IllegalStateException("meter registry closed");

    @Mock
    OutboxScheduleStrategyListener delegate;

    SafeOutboxScheduleStrategyListenerDecorator tested;

    @BeforeEach
    void setUp() {
        tested = new SafeOutboxScheduleStrategyListenerDecorator(delegate);
    }

    @Test
    @DisplayName("UT constructor when delegate is null should throw NullPointerException")
    void constructor_whenDelegateIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new SafeOutboxScheduleStrategyListenerDecorator(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("delegate cannot be null");
    }

    @Test
    @DisplayName("UT onExecutionStarted() should pass the call to the delegate")
    void onExecutionStarted_shouldPassCallToDelegate() {
        // when
        tested.onExecutionStarted();

        // then
        verify(delegate).onExecutionStarted();
    }

    @Test
    @DisplayName("UT onExecutionStarted() when the delegate throws should not throw")
    void onExecutionStarted_whenDelegateThrows_shouldNotThrow() {
        // given
        doThrow(LISTENER_FAILURE).when(delegate).onExecutionStarted();

        // when / then
        assertThatCode(tested::onExecutionStarted).doesNotThrowAnyException();
        verify(delegate).onExecutionStarted();
    }

    @Test
    @DisplayName("UT onExecutionSkipped() should pass the call to the delegate")
    void onExecutionSkipped_shouldPassCallToDelegate() {
        // when
        tested.onExecutionSkipped();

        // then
        verify(delegate).onExecutionSkipped();
    }

    @Test
    @DisplayName("UT onExecutionSkipped() when the delegate throws should not throw")
    void onExecutionSkipped_whenDelegateThrows_shouldNotThrow() {
        // given
        doThrow(LISTENER_FAILURE).when(delegate).onExecutionSkipped();

        // when / then
        assertThatCode(tested::onExecutionSkipped).doesNotThrowAnyException();
        verify(delegate).onExecutionSkipped();
    }

    @Test
    @DisplayName("UT onExecutionSucceeded() should pass the call to the delegate")
    void onExecutionSucceeded_shouldPassCallToDelegate() {
        // when
        tested.onExecutionSucceeded();

        // then
        verify(delegate).onExecutionSucceeded();
    }

    @Test
    @DisplayName("UT onExecutionSucceeded() when the delegate throws should not throw")
    void onExecutionSucceeded_whenDelegateThrows_shouldNotThrow() {
        // given
        doThrow(LISTENER_FAILURE).when(delegate).onExecutionSucceeded();

        // when / then
        assertThatCode(tested::onExecutionSucceeded).doesNotThrowAnyException();
        verify(delegate).onExecutionSucceeded();
    }

    @Test
    @DisplayName("UT onExecutionFailed() should pass the call to the delegate")
    void onExecutionFailed_shouldPassCallToDelegate() {
        // when
        tested.onExecutionFailed();

        // then
        verify(delegate).onExecutionFailed();
    }

    @Test
    @DisplayName("UT onExecutionFailed() when the delegate throws should not throw")
    void onExecutionFailed_whenDelegateThrows_shouldNotThrow() {
        // given
        doThrow(LISTENER_FAILURE).when(delegate).onExecutionFailed();

        // when / then
        assertThatCode(tested::onExecutionFailed).doesNotThrowAnyException();
        verify(delegate).onExecutionFailed();
    }

    @Test
    @DisplayName("UT onDelayChanged() should pass the delay to the delegate")
    void onDelayChanged_shouldPassDelayToDelegate() {
        // when
        tested.onDelayChanged(250L);

        // then
        verify(delegate).onDelayChanged(250L);
    }

    @Test
    @DisplayName("UT onDelayChanged() when the delegate throws should not throw")
    void onDelayChanged_whenDelegateThrows_shouldNotThrow() {
        // given
        doThrow(LISTENER_FAILURE).when(delegate).onDelayChanged(anyLong());

        // when / then
        assertThatCode(() -> tested.onDelayChanged(250L)).doesNotThrowAnyException();
        verify(delegate).onDelayChanged(250L);
    }

    @Test
    @DisplayName("UT onExecutionStarted() when the delegate throws an Error should not throw")
    void onExecutionStarted_whenDelegateThrowsError_shouldNotThrow() {
        // given
        doThrow(new AssertionError("broken listener")).when(delegate).onExecutionStarted();

        // when / then
        assertThatCode(tested::onExecutionStarted).doesNotThrowAnyException();
    }
}
