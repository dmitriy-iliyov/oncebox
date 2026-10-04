package io.github.dmitriyiliyov.oncebox.core.inbox.cache;

/**
 * Abstraction for monitoring hits and misses of the inbox cache.
 */
public interface InboxCacheListener {

    void onHit();

    void onMiss();
    
    /**
     * A no-operation implementation that does nothing on hit or miss.
     */
    InboxCacheListener NOOP = new InboxCacheListener() {
        @Override
        public void onHit() { }

        @Override
        public void onMiss() { }
    };
}
