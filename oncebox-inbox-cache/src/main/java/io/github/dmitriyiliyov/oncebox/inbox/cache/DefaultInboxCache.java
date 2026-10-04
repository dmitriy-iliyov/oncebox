package io.github.dmitriyiliyov.oncebox.inbox.cache;

import io.github.dmitriyiliyov.oncebox.core.inbox.cache.InboxCache;
import io.github.dmitriyiliyov.oncebox.core.inbox.cache.InboxCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Objects;
import java.util.UUID;

public class DefaultInboxCache implements InboxCache {

    private static final Logger log = LoggerFactory.getLogger(DefaultInboxCache.class);

    private final String cacheName;
    private final CacheManager cacheManager;
    private final InboxCacheListener cacheListener;

    public DefaultInboxCache(CacheManager cacheManager, String cacheName, InboxCacheListener cacheListener) {
        this.cacheManager = Objects.requireNonNull(cacheManager, "cacheManager cannot be null");
        this.cacheName = Objects.requireNonNull(cacheName, "cacheName cannot be null");
        if (cacheName.isBlank()) {
            throw new IllegalArgumentException("cacheName cannot be empty or blank");
        }
        Objects.requireNonNull(cacheManager.getCache(cacheName), "cache with cacheName '%s' cannot be null".formatted(cacheName));
        this.cacheListener = Objects.requireNonNull(cacheListener, "cacheListener cannot be null");
    }

    @Override
    public boolean isConsumed(UUID id) {
        Cache cache = cacheManager.getCache(cacheName);
        boolean isConsumed = cache.get(id.toString(), String.class) != null;
        if (isConsumed) {
            cacheListener.onHit();
            return isConsumed;
        }
        cacheListener.onMiss();
        return false;
    }

    @Override
    public void consume(UUID id) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(id, "");
        } else {
            log.warn("Cache for inbox with name '{}' not found", cacheName);
        }
    }
}
