package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.core.inbox.cache.InboxCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class IdempotentConsumerCacheDecoratorSupplierUnitTests {

    @Test
    @DisplayName("UT constructor should throw NPE when cache is null")
    void constructor_shouldThrowNPE_whenCacheIsNull() {
        assertThatThrownBy(() -> new IdempotentConsumerCacheDecoratorSupplier(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("UT supply() should throw NPE when consumer is null")
    void supply_shouldThrowNPE_whenConsumerIsNull() {
        InboxCache cache = mock(InboxCache.class);
        IdempotentConsumerCacheDecoratorSupplier supplier = new IdempotentConsumerCacheDecoratorSupplier(cache);

        assertThatThrownBy(() -> supplier.supply(null))
                .isInstanceOf(NullPointerException.class);
    }
}
