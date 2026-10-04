package io.github.dmitriyiliyov.oncebox.metrics.inbox;

import io.github.dmitriyiliyov.oncebox.core.inbox.EventIdExtractor;
import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class IdempotentConsumerMetricsDecoratorUnitTests {

    @Mock
    private IdempotentConsumer delegate;

    @Mock
    private MeterRegistry registry;

    @Mock
    private Counter fails;

    private IdempotentConsumerMetricsDecorator tested;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(registry.counter(ArgumentMatchers.eq("inbox_events_total"), ArgumentMatchers.eq("type"), ArgumentMatchers.eq("failed")))
                .thenReturn(fails);
    }

    @Test
    @DisplayName("UT constructor should throw NPE when registry is null")
    void constructor_shouldThrowNPE_whenRegistryIsNull() {
        assertThrows(NullPointerException.class, () -> new IdempotentConsumerMetricsDecorator(null, delegate));
    }

    @Test
    @DisplayName("UT constructor should throw NPE when delegate is null")
    void constructor_shouldThrowNPE_whenDelegateIsNull() {
        assertThrows(NullPointerException.class, () -> new IdempotentConsumerMetricsDecorator(registry, null));
    }

    @Test
    @DisplayName("UT consume(UUID, Runnable) when throws should increment fails counter")
    void consumeUUIDRunnable_whenThrows_shouldIncrementFailsCounter() {
        tested = new IdempotentConsumerMetricsDecorator(registry, delegate);
        UUID id = UUID.randomUUID();
        Runnable runnable = () -> {};
        
        Mockito.doThrow(new RuntimeException()).when(delegate).consume(id, runnable);

        assertThrows(RuntimeException.class, () -> tested.consume(id, runnable));
        Mockito.verify(fails).increment();
    }

    @Test
    @DisplayName("UT consume(T, Extractor, Consumer) when throws should increment fails counter")
    void consumeTExtractorConsumer_whenThrows_shouldIncrementFailsCounter() {
        tested = new IdempotentConsumerMetricsDecorator(registry, delegate);
        String message = "msg";
        EventIdExtractor<String> extractor = m -> UUID.randomUUID();
        Consumer<String> consumer = m -> {};

        Mockito.doThrow(new RuntimeException()).when(delegate).consume(message, extractor, consumer);

        assertThrows(RuntimeException.class, () -> tested.consume(message, extractor, consumer));
        Mockito.verify(fails).increment();
    }

    @Test
    @DisplayName("UT consume(Set<UUID>, Consumer) when throws should increment fails counter by size")
    void consumeSetConsumer_whenThrows_shouldIncrementFailsCounterBySize() {
        tested = new IdempotentConsumerMetricsDecorator(registry, delegate);
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
        Consumer<Set<UUID>> consumer = m -> {};

        Mockito.doThrow(new RuntimeException()).when(delegate).consume(ids, consumer);

        assertThrows(RuntimeException.class, () -> tested.consume(ids, consumer));
        Mockito.verify(fails).increment(2.0);
    }

    @Test
    @DisplayName("UT consume(List<T>, Extractor, Consumer) when throws should increment fails counter by size")
    void consumeListExtractorConsumer_whenThrows_shouldIncrementFailsCounterBySize() {
        tested = new IdempotentConsumerMetricsDecorator(registry, delegate);
        List<String> messages = List.of("msg1", "msg2", "msg3");
        EventIdExtractor<String> extractor = m -> UUID.randomUUID();
        Consumer<List<String>> consumer = m -> {};

        Mockito.doThrow(new RuntimeException()).when(delegate).consume(messages, extractor, consumer);

        assertThrows(RuntimeException.class, () -> tested.consume(messages, extractor, consumer));
        Mockito.verify(fails).increment(3.0);
    }
}
