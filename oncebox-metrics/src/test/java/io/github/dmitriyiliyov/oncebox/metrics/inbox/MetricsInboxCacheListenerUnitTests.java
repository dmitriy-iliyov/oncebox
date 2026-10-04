package io.github.dmitriyiliyov.oncebox.metrics.inbox;

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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class MetricsInboxCacheListenerUnitTests {

    @Mock
    private MeterRegistry registry;

    @Mock
    private Counter hits;

    @Mock
    private Counter misses;

    private MetricsInboxCacheListener tested;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(registry.counter(ArgumentMatchers.eq("inbox_cache_action_total"), ArgumentMatchers.eq("action_type"), ArgumentMatchers.eq("hit")))
                .thenReturn(hits);
        Mockito.lenient().when(registry.counter(ArgumentMatchers.eq("inbox_cache_action_total"), ArgumentMatchers.eq("action_type"), ArgumentMatchers.eq("miss")))
                .thenReturn(misses);
    }

    @Test
    @DisplayName("UT constructor should throw NPE when registry is null")
    void constructor_shouldThrowNPE_whenRegistryIsNull() {
        assertThrows(NullPointerException.class, () -> new MetricsInboxCacheListener(null));
    }

    @Test
    @DisplayName("UT onHit() should increment hits counter")
    void onHit_shouldIncrementHitsCounter() {
        tested = new MetricsInboxCacheListener(registry);
        tested.onHit();

        Mockito.verify(hits).increment();
    }

    @Test
    @DisplayName("UT onMiss() should increment misses counter")
    void onMiss_shouldIncrementMissesCounter() {
        tested = new MetricsInboxCacheListener(registry);
        tested.onMiss();

        Mockito.verify(misses).increment();
    }
}
