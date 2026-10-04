package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.core.inbox.cache.InboxCacheListener;
import io.github.dmitriyiliyov.oncebox.inbox.cache.DefaultInboxCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@ConditionalOnProperty(
        prefix = "oncebox.inbox",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnClass(DefaultInboxCache.class)
@ConditionalOnProperty(
        prefix = "oncebox.inbox.cache",
        name = "enabled",
        havingValue = "true"
)
public class InboxCacheAutoConfiguration {

    private final InboxProperties inboxProperties;

    public InboxCacheAutoConfiguration(InboxProperties inboxProperties) {
        this.inboxProperties = inboxProperties;
    }

    @Bean
    @ConditionalOnMissingBean(InboxCacheListener.class)
    public InboxCacheListener noopInboxCacheListener() {
        return InboxCacheListener.NOOP;
    }

    @Bean
    @Order(1)
    public IdempotentConsumerDecoratorSupplier inboxIdempotentConsumerCacheDecoratorSupplier(
            InboxCacheListener cacheListener,
            CacheManager cacheManager
    ) {
        return new IdempotentConsumerCacheDecoratorSupplier(
                new DefaultInboxCache(
                        cacheManager,
                        inboxProperties.getCache().getCacheName(),
                        cacheListener
                )
        );
    }
}
