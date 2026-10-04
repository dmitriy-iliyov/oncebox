package io.github.dmitriyiliyov.oncebox.core.inbox.cache;

import io.github.dmitriyiliyov.oncebox.core.inbox.EventIdExtractor;
import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotentConsumerCacheDecoratorUnitTests {

    @Mock
    private IdempotentConsumer delegate;

    @Mock
    private InboxCache cache;

    private IdempotentConsumerCacheDecorator decorator;

    @Test
    @DisplayName("UT constructor when delegate is null should throw NullPointerException")
    void constructor_whenDelegateIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new IdempotentConsumerCacheDecorator(null, cache))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("delegate cannot be null");
    }

    @Test
    @DisplayName("UT constructor when cache is null should throw NullPointerException")
    void constructor_whenCacheIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new IdempotentConsumerCacheDecorator(delegate, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("cache cannot be null");
    }

    @Test
    @DisplayName("UT consume(UUID, Runnable) when cache hit should return without executing delegate")
    void consumeRunnable_whenCacheHit_shouldReturnWithoutExecutingDelegate() {
        // given
        UUID id = UUID.randomUUID();
        Runnable operation = mock(Runnable.class);
        when(cache.isConsumed(id)).thenReturn(true);

        decorator = new IdempotentConsumerCacheDecorator(delegate, cache);

        // when
        decorator.consume(id, operation);

        // then
        Mockito.verify(cache).isConsumed(id);
        Mockito.verify(delegate, Mockito.never()).consume(Mockito.any(UUID.class), Mockito.any(Runnable.class));
        Mockito.verify(cache, Mockito.never()).consume(id);
    }

    @Test
    @DisplayName("UT consume(UUID, Runnable) when cache miss should execute delegate and cache id")
    void consumeRunnable_whenCacheMiss_shouldExecuteDelegateAndCacheId() {
        // given
        UUID id = UUID.randomUUID();
        Runnable operation = mock(Runnable.class);
        when(cache.isConsumed(id)).thenReturn(false);

        decorator = new IdempotentConsumerCacheDecorator(delegate, cache);

        // when
        decorator.consume(id, operation);

        // then
        Mockito.verify(cache).isConsumed(id);
        Mockito.verify(delegate).consume(id, operation);
        Mockito.verify(cache).consume(id);
    }

    @Test
    @DisplayName("UT consume(Message, Extractor, Consumer) when cache hit should return without executing delegate")
    @SuppressWarnings("unchecked")
    void consumeMessage_whenCacheHit_shouldReturnWithoutExecutingDelegate() {
        // given
        String message = "test-message";
        UUID id = UUID.randomUUID();
        EventIdExtractor<String> extractor = mock(EventIdExtractor.class);
        Consumer<String> operation = mock(Consumer.class);

        when(extractor.extract(message)).thenReturn(id);
        when(cache.isConsumed(id)).thenReturn(true);

        decorator = new IdempotentConsumerCacheDecorator(delegate, cache);

        // when
        decorator.consume(message, extractor, operation);

        // then
        Mockito.verify(extractor).extract(message);
        Mockito.verify(cache).isConsumed(id);
        Mockito.verify(delegate, Mockito.never()).consume(message, extractor, operation);
        Mockito.verify(cache, Mockito.never()).consume(id);
    }

    @Test
    @DisplayName("UT consume(Message, Extractor, Consumer) when cache miss should execute delegate and cache id")
    @SuppressWarnings("unchecked")
    void consumeMessage_whenCacheMiss_shouldExecuteDelegateAndCacheId() {
        // given
        String message = "test-message";
        UUID id = UUID.randomUUID();
        EventIdExtractor<String> extractor = mock(EventIdExtractor.class);
        Consumer<String> operation = mock(Consumer.class);

        when(extractor.extract(message)).thenReturn(id);
        when(cache.isConsumed(id)).thenReturn(false);

        decorator = new IdempotentConsumerCacheDecorator(delegate, cache);

        // when
        decorator.consume(message, extractor, operation);

        // then
        Mockito.verify(extractor).extract(message);
        Mockito.verify(cache).isConsumed(id);
        Mockito.verify(delegate).consume(message, extractor, operation);
        Mockito.verify(cache).consume(id);
    }

    @Test
    @DisplayName("UT consume(Set, Consumer) should delegate to underlying consumer directly without caching logic")
    @SuppressWarnings("unchecked")
    void consumeSet_shouldDelegateToUnderlyingConsumerDirectly() {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
        Consumer<Set<UUID>> operation = mock(Consumer.class);

        decorator = new IdempotentConsumerCacheDecorator(delegate, cache);

        // when
        decorator.consume(ids, operation);

        // then
        Mockito.verify(delegate).consume(ids, operation);
        Mockito.verifyNoInteractions(cache);
    }

    @Test
    @DisplayName("UT consume(List, Extractor, Consumer) should delegate to underlying consumer directly without caching logic")
    @SuppressWarnings("unchecked")
    void consumeList_shouldDelegateToUnderlyingConsumerDirectly() {
        // given
        List<String> messages = List.of("msg1", "msg2");
        EventIdExtractor<String> extractor = mock(EventIdExtractor.class);
        Consumer<List<String>> operation = mock(Consumer.class);

        decorator = new IdempotentConsumerCacheDecorator(delegate, cache);

        // when
        decorator.consume(messages, extractor, operation);

        // then
        Mockito.verify(delegate).consume(messages, extractor, operation);
        Mockito.verifyNoInteractions(cache);
    }
}