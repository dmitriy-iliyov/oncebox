package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.core.inbox.DefaultIdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.core.inbox.InboxRepository;
import io.github.dmitriyiliyov.oncebox.core.inbox.cache.IdempotentConsumerCacheDecorator;
import io.github.dmitriyiliyov.oncebox.metrics.inbox.IdempotentConsumerMetricsDecorator;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxRepositoryFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * The consumer decorator chain is built by {@code primaryIdempotentConsumer} from an injected
 * {@code List<IdempotentConsumerDecoratorSupplier>}, so its nesting depends entirely on the
 * {@code @Order} values of the supplier beans. Nothing about that ordering is observable through the
 * emitted metrics, which is why it is asserted structurally here.
 */
class IdempotentConsumerDecoratorChainOrderTests {

    private final InboxProperties properties = mock(InboxProperties.class);
    private final CacheManager cacheManager = mock(CacheManager.class);

    private ApplicationContextRunner contextRunner() {
        InboxProperties.CacheProperties cache = mock(InboxProperties.CacheProperties.class);
        when(cache.getCacheName()).thenReturn("cache-name");
        when(properties.getCache()).thenReturn(cache);
        when(cacheManager.getCache("cache-name")).thenReturn(mock(Cache.class));

        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        OnceboxRepositoryFactory repositoryFactory = mock(OnceboxRepositoryFactory.class);
        when(repositoryFactory.createInboxRepository()).thenReturn(mock(InboxRepository.class));

        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(InboxAutoConfiguration.class))
                .withUserConfiguration(ProbeConfig.class)
                .withBean(InboxProperties.class, () -> properties)
                .withBean(CacheManager.class, () -> cacheManager)
                .withBean(TransactionTemplate.class, () -> transactionTemplate)
                .withBean(OnceboxRepositoryFactory.class, () -> repositoryFactory)
                .withBean(Clock.class, Clock::systemUTC)
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new);
    }

    @Test
    @DisplayName("IT should inject decorator suppliers ordered cache first, metrics second")
    void shouldInjectSuppliersInOrder() {
        contextRunner()
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(Probe.class).suppliers())
                            .extracting(Object::getClass)
                            .containsExactly(
                                    IdempotentConsumerCacheDecoratorSupplier.class,
                                    IdempotentConsumerMetricsDecoratorSupplier.class
                            );
                });
    }

    @Test
    @DisplayName("IT should nest the primary consumer as metrics -> cache -> default")
    void shouldNestMetricsOutsideCache() {
        contextRunner()
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    IdempotentConsumer primary = context.getBean(IdempotentConsumer.class);

                    assertThat(primary).isInstanceOf(IdempotentConsumerMetricsDecorator.class);
                    IdempotentConsumer inner = delegateOf(primary);
                    assertThat(inner).isInstanceOf(IdempotentConsumerCacheDecorator.class);
                    assertThat(delegateOf(inner)).isInstanceOf(DefaultIdempotentConsumer.class);
                });
    }

    @Test
    @DisplayName("IT should nest only the cache decorator when metrics are disabled")
    void shouldNestOnlyCacheWhenMetricsDisabled() {
        contextRunner()
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.metrics.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    IdempotentConsumer primary = context.getBean(IdempotentConsumer.class);

                    assertThat(primary).isInstanceOf(IdempotentConsumerCacheDecorator.class);
                    assertThat(delegateOf(primary)).isInstanceOf(DefaultIdempotentConsumer.class);
                });
    }

    @Test
    @DisplayName("IT should nest only the metrics decorator when the cache is disabled")
    void shouldNestOnlyMetricsWhenCacheDisabled() {
        contextRunner()
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.cache.enabled=false",
                        "oncebox.inbox.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    IdempotentConsumer primary = context.getBean(IdempotentConsumer.class);

                    assertThat(primary).isInstanceOf(IdempotentConsumerMetricsDecorator.class);
                    assertThat(delegateOf(primary)).isInstanceOf(DefaultIdempotentConsumer.class);
                });
    }

    @Test
    @DisplayName("IT should leave the primary consumer undecorated when cache and metrics are disabled")
    void shouldNotDecorateWhenCacheAndMetricsDisabled() {
        contextRunner()
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.cache.enabled=false",
                        "oncebox.inbox.metrics.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(IdempotentConsumer.class))
                            .isInstanceOf(DefaultIdempotentConsumer.class);
                });
    }

    private static IdempotentConsumer delegateOf(IdempotentConsumer decorator) {
        return (IdempotentConsumer) ReflectionTestUtils.getField(decorator, "delegate");
    }

    record Probe(List<IdempotentConsumerDecoratorSupplier> suppliers) { }

    @Configuration
    static class ProbeConfig {
        @Bean
        Probe probe(List<IdempotentConsumerDecoratorSupplier> suppliers) {
            return new Probe(suppliers);
        }
    }
}
