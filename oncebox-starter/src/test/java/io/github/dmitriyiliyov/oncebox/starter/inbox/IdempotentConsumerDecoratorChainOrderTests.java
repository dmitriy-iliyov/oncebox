package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.core.inbox.InboxRepository;
import io.github.dmitriyiliyov.oncebox.core.inbox.NonTransactionalIdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.core.inbox.TransactionalIdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.core.inbox.cache.IdempotentConsumerCacheDecorator;
import io.github.dmitriyiliyov.oncebox.metrics.inbox.IdempotentConsumerMetricsDecorator;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxRepositoryFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Both consumer beans are decorated from an injected {@code List<IdempotentConsumerDecoratorSupplier>}, so their
 * nesting depends entirely on the {@code @Order} values of the supplier beans. Nothing about that ordering is
 * observable through the emitted metrics, which is why it is asserted structurally here.
 */
class IdempotentConsumerDecoratorChainOrderTests {

    private static final String TRANSACTIONAL = "inboxTransactionalIdempotentConsumer";
    private static final String NON_TRANSACTIONAL = "inboxNonTransactionalIdempotentConsumer";

    static Stream<Arguments> consumers() {
        return Stream.of(
                Arguments.of(TRANSACTIONAL, TransactionalIdempotentConsumer.class),
                Arguments.of(NON_TRANSACTIONAL, NonTransactionalIdempotentConsumer.class)
        );
    }

    @Test
    @DisplayName("IT context should inject decorator suppliers ordered cache first, metrics second")
    void context_whenCacheAndMetricsEnabled_shouldInjectSuppliersCacheFirst() {
        contextRunner()
                .withPropertyValues(
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("consumers")
    @DisplayName("IT context when cache and metrics enabled should nest the consumer as metrics -> cache -> implementation")
    void context_whenCacheAndMetricsEnabled_shouldNestMetricsOutsideCache(String beanName, Class<?> implementation) {
        contextRunner()
                .withPropertyValues(
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    IdempotentConsumer consumer = context.getBean(beanName, IdempotentConsumer.class);

                    assertThat(consumer).isInstanceOf(IdempotentConsumerMetricsDecorator.class);
                    IdempotentConsumer inner = delegateOf(consumer);
                    assertThat(inner).isInstanceOf(IdempotentConsumerCacheDecorator.class);
                    assertThat(delegateOf(inner)).isInstanceOf(implementation);
                });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("consumers")
    @DisplayName("IT context when only cache enabled should nest only the cache decorator")
    void context_whenOnlyCacheEnabled_shouldNestOnlyCache(String beanName, Class<?> implementation) {
        contextRunner()
                .withPropertyValues(
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.metrics.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    IdempotentConsumer consumer = context.getBean(beanName, IdempotentConsumer.class);

                    assertThat(consumer).isInstanceOf(IdempotentConsumerCacheDecorator.class);
                    assertThat(delegateOf(consumer)).isInstanceOf(implementation);
                });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("consumers")
    @DisplayName("IT context when only metrics enabled should nest only the metrics decorator")
    void context_whenOnlyMetricsEnabled_shouldNestOnlyMetrics(String beanName, Class<?> implementation) {
        contextRunner()
                .withPropertyValues(
                        "oncebox.inbox.cache.enabled=false",
                        "oncebox.inbox.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    IdempotentConsumer consumer = context.getBean(beanName, IdempotentConsumer.class);

                    assertThat(consumer).isInstanceOf(IdempotentConsumerMetricsDecorator.class);
                    assertThat(delegateOf(consumer)).isInstanceOf(implementation);
                });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("consumers")
    @DisplayName("IT context when cache and metrics disabled should leave the consumer undecorated")
    void context_whenCacheAndMetricsDisabled_shouldNotDecorate(String beanName, Class<?> implementation) {
        contextRunner()
                .withPropertyValues(
                        "oncebox.inbox.cache.enabled=false",
                        "oncebox.inbox.metrics.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(beanName)).isInstanceOf(implementation);
                });
    }

    @Test
    @DisplayName("IT context when cache and metrics enabled should give each consumer its own decorators")
    void context_whenCacheAndMetricsEnabled_shouldNotShareDecoratorsBetweenConsumers() {
        contextRunner()
                .withPropertyValues(
                        "oncebox.inbox.cache.enabled=true",
                        "oncebox.inbox.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    IdempotentConsumer transactional = context.getBean(TRANSACTIONAL, IdempotentConsumer.class);
                    IdempotentConsumer nonTransactional = context.getBean(NON_TRANSACTIONAL, IdempotentConsumer.class);

                    assertThat(transactional).isNotSameAs(nonTransactional);
                    assertThat(delegateOf(transactional)).isNotSameAs(delegateOf(nonTransactional));
                });
    }

    private ApplicationContextRunner contextRunner() {
        InboxProperties properties = mock(InboxProperties.class);
        InboxProperties.CacheProperties cache = mock(InboxProperties.CacheProperties.class);
        when(cache.getCacheName()).thenReturn("cache-name");
        when(properties.getCache()).thenReturn(cache);

        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache("cache-name")).thenReturn(mock(Cache.class));

        OnceboxRepositoryFactory repositoryFactory = mock(OnceboxRepositoryFactory.class);
        when(repositoryFactory.createInboxRepository()).thenReturn(mock(InboxRepository.class));

        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(InboxAutoConfiguration.class))
                .withUserConfiguration(ProbeConfig.class)
                .withBean(InboxProperties.class, () -> properties)
                .withBean(CacheManager.class, () -> cacheManager)
                .withBean(TransactionTemplate.class, () -> mock(TransactionTemplate.class))
                .withBean(OnceboxRepositoryFactory.class, () -> repositoryFactory)
                .withBean(Clock.class, Clock::systemUTC)
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues("oncebox.inbox.enabled=true");
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
