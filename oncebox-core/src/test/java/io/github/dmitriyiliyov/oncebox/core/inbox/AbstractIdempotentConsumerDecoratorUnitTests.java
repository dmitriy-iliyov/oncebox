package io.github.dmitriyiliyov.oncebox.core.inbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AbstractIdempotentConsumerDecoratorUnitTests {

    private static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private IdempotentConsumer delegate;

    @Mock
    private EventIdExtractor<String> idExtractor;

    private PassThroughDecorator tested;

    @BeforeEach
    void setUp() {
        tested = new PassThroughDecorator(delegate);
    }

    @Test
    @DisplayName("UT constructor when delegate is null should throw NullPointerException")
    void constructor_whenDelegateIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new PassThroughDecorator(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("delegate cannot be null");
    }

    @Test
    @DisplayName("UT consume(UUID, Runnable) should pass the same arguments to the delegate")
    void consumeRunnable_whenNotOverridden_shouldPassSameArgumentsToDelegate() {
        // given
        Runnable operation = () -> { };

        // when
        tested.consume(EVENT_ID, operation);

        // then
        verify(delegate).consume(EVENT_ID, operation);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    @DisplayName("UT consume(Message, Extractor, Consumer) should pass the same arguments to the delegate")
    void consumeMessage_whenNotOverridden_shouldPassSameArgumentsToDelegate() {
        // given
        Consumer<String> operation = message -> { };

        // when
        tested.consume("message", idExtractor, operation);

        // then
        verify(delegate).consume("message", idExtractor, operation);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    @DisplayName("UT consume(Set, Consumer) should pass the same arguments to the delegate")
    void consumeSet_whenNotOverridden_shouldPassSameArgumentsToDelegate() {
        // given
        Set<UUID> ids = Set.of(EVENT_ID);
        Consumer<Set<UUID>> operation = newIds -> { };

        // when
        tested.consume(ids, operation);

        // then
        verify(delegate).consume(ids, operation);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    @DisplayName("UT consume(List, Extractor, Consumer) should pass the same arguments to the delegate")
    void consumeList_whenNotOverridden_shouldPassSameArgumentsToDelegate() {
        // given
        List<String> messages = List.of("message");
        Consumer<List<String>> operation = newMessages -> { };

        // when
        tested.consume(messages, idExtractor, operation);

        // then
        verify(delegate).consume(messages, idExtractor, operation);
        verifyNoMoreInteractions(delegate);
    }

    private static final class PassThroughDecorator extends AbstractIdempotentConsumerDecorator {

        PassThroughDecorator(IdempotentConsumer delegate) {
            super(delegate);
        }
    }
}
