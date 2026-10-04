package io.github.dmitriyiliyov.oncebox.inbox.cache;

import io.github.dmitriyiliyov.oncebox.core.inbox.cache.InboxCacheListener;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultInboxCacheIntegrationTests extends BaseRedisIntegrationTests {

    private final CacheManager cacheManager;

    private DefaultInboxCache decorator;

    private static final String CACHE_NAME = "oncebox:inbox";

    DefaultInboxCacheIntegrationTests(@Qualifier("redisIntegrationTestsCacheManager") CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @BeforeEach
    void setUp() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
        decorator = new DefaultInboxCache(
                cacheManager, CACHE_NAME, InboxCacheListener.NOOP
        );
    }

    @Test
    @DisplayName("IT constructor throws when cacheName is null")
    void constructor_nullCacheName_throwsNullPointerException() {
        Assertions.assertThatThrownBy(() ->
                new DefaultInboxCache(cacheManager, null, InboxCacheListener.NOOP)
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("IT constructor throws when cacheName is blank")
    void constructor_blankCacheName_throwsIllegalArgumentException() {
        Assertions.assertThatThrownBy(() ->
                new DefaultInboxCache(cacheManager, "  ", InboxCacheListener.NOOP)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("IT isConsumed() cache miss - calls delegate and puts into cache")
    void isConsumed_cacheMiss_callsDelegateAndCaches() {
        UUID id = UUID.randomUUID();

        boolean result = decorator.isConsumed(id);

        Assertions.assertThat(result).isFalse();
    }

    @Test
    @DisplayName("IT isConsumed() cache hit - does not call delegate")
    void isConsumed_cacheHit_doesNotCallDelegate() {
        UUID id = UUID.randomUUID();
        decorator.isConsumed(id);
        decorator.consume(id);

        boolean result = decorator.isConsumed(id);

        Assertions.assertThat(result).isTrue();
    }

    @Test
    @DisplayName("IT isConsumed() delegate returns true - result is true and cached")
    void isConsumed_delegateReturnsTrue_resultTryTrueAndCached() {
        UUID id = UUID.randomUUID();

        decorator.consume(id);
        boolean second = decorator.isConsumed(id);

        Assertions.assertThat(second).isTrue();
    }

    @Test
    @DisplayName("IT isConsumed() different ids cached independently")
    void isConsumed_differentIds_cachedIndependently() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        decorator.isConsumed(id1);
        decorator.isConsumed(id2);

        decorator.consume(id1);
        decorator.consume(id2);

        assertTrue(decorator.isConsumed(id1));
        assertTrue(decorator.isConsumed(id2));
    }

    @Test
    @DisplayName("IT isConsumed() after cache clear - calls delegate again")
    void isConsumed_afterCacheClear_callsDelegateAgain() {
        UUID id = UUID.randomUUID();
        decorator.isConsumed(id);
        decorator.consume(id);
        decorator.isConsumed(id);

        cacheManager.getCache(CACHE_NAME).clear();
        assertFalse(decorator.isConsumed(id));
    }

    @Test
    @DisplayName("IT consume() should store empty string in cache")
    void consume_shouldStoreEmptyStringInCache() {
        UUID id = UUID.randomUUID();

        decorator.consume(id);

        Cache cache = cacheManager.getCache(CACHE_NAME);
        Assertions.assertThat(cache).isNotNull();
        Assertions.assertThat(cache.get(id, String.class)).isEqualTo("");
    }
}