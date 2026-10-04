package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class IdempotentConsumerMetricsDecoratorSupplierUnitTests {

    @Test
    @DisplayName("UT constructor should throw NPE when registry is null")
    void constructor_shouldThrowNPE_whenRegistryIsNull() {
        assertThatThrownBy(() -> new IdempotentConsumerMetricsDecoratorSupplier(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("UT supply() should throw NPE when consumer is null")
    void supply_shouldThrowNPE_whenConsumerIsNull() {
        MeterRegistry registry = mock(MeterRegistry.class);
        IdempotentConsumerMetricsDecoratorSupplier supplier = new IdempotentConsumerMetricsDecoratorSupplier(registry);

        assertThatThrownBy(() -> supplier.supply(null))
                .isInstanceOf(NullPointerException.class);
    }
}
