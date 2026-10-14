package io.github.dmitriyiliyov.oncebox.inbox.cache;

import io.github.dmitriyiliyov.oncebox.core.inbox.cache.InboxCacheListener;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultInboxCacheUnitTests {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private InboxCacheListener cacheListener;

    private DefaultInboxCache decorator;

    @Test
    @DisplayName("UT constructor when cacheManager is null should throw NullPointerException")
    void constructor_whenCacheManagerIsNull_shouldThrowNullPointerException() {
        Assertions.assertThatThrownBy(() -> new DefaultInboxCache(null, "cache", cacheListener))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cacheManager cannot be null");
    }

    @Test
    @DisplayName("UT constructor when cacheName is null should throw NullPointerException")
    void constructor_whenCacheNameIsNull_shouldThrowNullPointerException() {
        Assertions.assertThatThrownBy(() -> new DefaultInboxCache(cacheManager, null, cacheListener))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cacheName cannot be null");
    }

    @Test
    @DisplayName("UT constructor when cacheListener is null should throw NullPointerException")
    void constructor_whenCacheListenerIsNull_shouldThrowNullPointerException() {
        when(cacheManager.getCache("cache")).thenReturn(mock(Cache.class));
        Assertions.assertThatThrownBy(() -> new DefaultInboxCache(cacheManager, "cache", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cacheListener cannot be null");
    }

    @Test
    @DisplayName("UT constructor when cache is null should throw NullPointerException")
    void constructor_whenCacheIsNull_shouldThrowNullPointerException() {
        Assertions.assertThatThrownBy(() -> new DefaultInboxCache(cacheManager, "cache", cacheListener))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cache with cacheName 'cache' cannot be null");
    }

    @Test
    @DisplayName("UT constructor when cacheName is blank should throw IllegalArgumentException")
    void constructor_whenCacheNameIsBlank_shouldThrowIllegalArgumentException() {
        Assertions.assertThatThrownBy(() -> new DefaultInboxCache(cacheManager, "   ", cacheListener))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cacheName cannot be empty or blank");
    }

    @Test
    @DisplayName("UT constructor when cacheName is empty should throw IllegalArgumentException")
    void constructor_whenCacheNameIsEmpty_shouldThrowIllegalArgumentException() {
        Assertions.assertThatThrownBy(() -> new DefaultInboxCache(
                cacheManager, "", cacheListener))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cacheName cannot be empty or blank");
    }

    @Test
    @DisplayName("UT isConsumed() when cache hit should increment hits counter and return true")
    void isConsumed_whenCacheHit_shouldIncrementHitsCounterAndReturnTrue() {
        // given
        UUID id = UUID.randomUUID();
        String cacheName = "test-cache";

        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(cache.get(id.toString(), String.class)).thenReturn("");

        decorator = new DefaultInboxCache(cacheManager, cacheName, cacheListener);

        // when
        boolean result = decorator.isConsumed(id);

        // then
        Assertions.assertThat(result).isTrue();
        Mockito.verify(cache).get(id.toString(), String.class);
        Mockito.verify(cacheListener).onHit();
        Mockito.verify(cacheListener, Mockito.never()).onMiss();
    }

    @Test
    @DisplayName("UT isConsumed() when cache miss should increment misses counter and delegate")
    void isConsumed_whenCacheMiss_shouldIncrementMissesCounterAndDelegate() {
        // given
        UUID id = UUID.randomUUID();
        String cacheName = "test-cache";
        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(cache.get(id.toString(), String.class)).thenReturn(null);

        decorator = new DefaultInboxCache(cacheManager, cacheName, cacheListener);

        // when
        boolean result = decorator.isConsumed(id);

        // then
        Assertions.assertThat(result).isFalse();
        Mockito.verify(cache).get(id.toString(), String.class);
        Mockito.verify(cacheListener).onMiss();
        Mockito.verify(cacheListener, Mockito.never()).onHit();
    }

    @Test
    @DisplayName("UT isConsumed() when cache miss and delegate returns false should return false")
    void isConsumed_whenCacheMissAndDelegateReturnsFalse_shouldReturnFalse() {
        // given
        UUID id = UUID.randomUUID();
        String cacheName = "test-cache";
        when(cacheManager.getCache(cacheName)).thenReturn(cache);
        when(cache.get(id.toString(), String.class)).thenReturn(null);

        decorator = new DefaultInboxCache(cacheManager, cacheName, cacheListener);

        // when
        boolean result = decorator.isConsumed(id);

        // then
        Assertions.assertThat(result).isFalse();
        Mockito.verify(cache, Mockito.never()).put(id.toString(), String.class);
        Mockito.verify(cacheListener).onMiss();
    }

    @Test
    @DisplayName("UT consume() when cache exists should put empty string under the id as a string")
    void consume_whenCacheExists_shouldPutEmptyStringUnderIdAsString() {
        // given
        UUID id = UUID.randomUUID();
        String cacheName = "test-cache";
        when(cacheManager.getCache(cacheName)).thenReturn(cache);

        decorator = new DefaultInboxCache(cacheManager, cacheName, cacheListener);

        // when
        decorator.consume(id);

        // then
        Mockito.verify(cache).put(id.toString(), "");
    }

    @Test
    @DisplayName("UT consume() when cache is null should not throw exception")
    void consume_whenCacheIsNull_shouldNotThrowException() {
        // given
        UUID id = UUID.randomUUID();
        String cacheName = "test-cache";
        when(cacheManager.getCache(cacheName)).thenReturn(cache).thenReturn(null);

        decorator = new DefaultInboxCache(cacheManager, cacheName, cacheListener);

        // when
        Assertions.assertThatCode(() -> decorator.consume(id))
                .doesNotThrowAnyException();

        // then
        Mockito.verify(cache, Mockito.never()).put(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("UT isConsumed() after consume() on an in-memory cache should report the id as consumed")
    void isConsumed_afterConsumeOnInMemoryCache_shouldReportConsumed() {
        // given
        UUID id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        DefaultInboxCache tested = new DefaultInboxCache(
                new ConcurrentMapCacheManager("inbox"), "inbox", cacheListener
        );

        // when
        tested.consume(id);

        // then
        Assertions.assertThat(tested.isConsumed(id)).isTrue();
    }
}
