package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.core.inbox.*;
import io.github.dmitriyiliyov.oncebox.metrics.inbox.IdempotentConsumerMetricsDecorator;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxRepositoryFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Injection through {@link TransactionalConsumer} and {@link NonTransactionalConsumer}, on a context refreshed by
 * hand: the user's classes are registered before the inbox configuration, as an application's own beans are.
 */
class IdempotentConsumerQualifierAnnotationsIntegrationTests {

    private static final String TRANSACTIONAL = "inboxTransactionalIdempotentConsumer";
    private static final String NON_TRANSACTIONAL = "inboxNonTransactionalIdempotentConsumer";

    @Test
    @DisplayName("IT injection when @TransactionalConsumer is on a constructor should inject the decorated transactional bean")
    void injection_whenTransactionalConsumerOnConstructor_shouldInjectDecoratedTransactionalConsumer() {
        // given / when
        try (ConfigurableApplicationContext context = refreshedContext(TransactionalConstructorListener.class)) {
            IdempotentConsumer injected = context.getBean(TransactionalConstructorListener.class).consumer;

            // then
            assertThat(injected).isSameAs(context.getBean(TRANSACTIONAL));
            assertThat(injected).isInstanceOf(IdempotentConsumerMetricsDecorator.class);
            assertThat(innermostOf(injected)).isInstanceOf(TransactionalIdempotentConsumer.class);
        }
    }

    @Test
    @DisplayName("IT injection when @NonTransactionalConsumer is on a constructor should inject the decorated bean")
    void injection_whenNonTransactionalConsumerOnConstructor_shouldInjectDecoratedNonTransactionalConsumer() {
        // given / when
        try (ConfigurableApplicationContext context = refreshedContext(NonTransactionalConstructorListener.class)) {
            IdempotentConsumer injected = context.getBean(NonTransactionalConstructorListener.class).consumer;

            // then
            assertThat(injected).isSameAs(context.getBean(NON_TRANSACTIONAL));
            assertThat(injected).isInstanceOf(IdempotentConsumerMetricsDecorator.class);
            assertThat(innermostOf(injected)).isInstanceOf(NonTransactionalIdempotentConsumer.class);
        }
    }

    @Test
    @DisplayName("IT injection when @TransactionalConsumer is on a field should inject the decorated transactional bean")
    void injection_whenTransactionalConsumerOnField_shouldInjectDecoratedTransactionalConsumer() {
        // given / when
        try (ConfigurableApplicationContext context = refreshedContext(TransactionalFieldListener.class)) {

            // then
            assertThat(context.getBean(TransactionalFieldListener.class).consumer)
                    .isSameAs(context.getBean(TRANSACTIONAL));
        }
    }

    @Test
    @DisplayName("IT injection when @NonTransactionalConsumer is on a setter should inject the decorated bean")
    void injection_whenNonTransactionalConsumerOnSetter_shouldInjectDecoratedNonTransactionalConsumer() {
        // given / when
        try (ConfigurableApplicationContext context = refreshedContext(NonTransactionalSetterListener.class)) {

            // then
            assertThat(context.getBean(NonTransactionalSetterListener.class).consumer)
                    .isSameAs(context.getBean(NON_TRANSACTIONAL));
        }
    }

    @Test
    @DisplayName("IT injection when both annotations are in one bean should give each parameter its own consumer")
    void injection_whenBothAnnotationsInOneBean_shouldGiveEachParameterItsOwnConsumer() {
        // given / when
        try (ConfigurableApplicationContext context = refreshedContext(BothConsumersListener.class)) {
            BothConsumersListener listener = context.getBean(BothConsumersListener.class);

            // then
            assertThat(listener.transactional).isSameAs(context.getBean(TRANSACTIONAL));
            assertThat(listener.nonTransactional).isSameAs(context.getBean(NON_TRANSACTIONAL));
        }
    }

    @Test
    @DisplayName("IT injection when @TransactionalConsumer is on a @Bean method parameter should inject the decorated bean")
    void injection_whenTransactionalConsumerOnBeanMethodParameter_shouldInjectDecoratedTransactionalConsumer() {
        // given / when
        try (ConfigurableApplicationContext context = refreshedContext(BeanMethodListenerConfiguration.class)) {

            // then
            assertThat(context.getBean(BeanMethodListener.class).consumer)
                    .isSameAs(context.getBean(TRANSACTIONAL));
        }
    }

    @Test
    @DisplayName("IT injection when the user replaced the undecorated bean should inject the user's bean decorated")
    void injection_whenUserReplacedUndecoratedTransactional_shouldInjectDecoratedUserBean() {
        // given / when
        try (ConfigurableApplicationContext context = refreshedContext(
                UserTransactionalConfiguration.class,
                TransactionalConstructorListener.class
        )) {
            IdempotentConsumer injected = context.getBean(TransactionalConstructorListener.class).consumer;

            // then
            assertThat(injected).isSameAs(context.getBean(TRANSACTIONAL));
            assertThat(innermostOf(injected)).isSameAs(UserTransactionalConfiguration.USER_CONSUMER);
        }
    }

    private static ConfigurableApplicationContext refreshedContext(Class<?>... userClasses) {
        OnceboxRepositoryFactory repositoryFactory = mock(OnceboxRepositoryFactory.class);
        when(repositoryFactory.createInboxRepository()).thenReturn(mock(InboxRepository.class));

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        TestPropertyValues.of(
                "oncebox.inbox.enabled=true",
                "oncebox.inbox.metrics.enabled=true"
        ).applyTo(context);
        context.registerBean(InboxProperties.class, () -> mock(InboxProperties.class));
        context.registerBean(TransactionTemplate.class, () -> mock(TransactionTemplate.class));
        context.registerBean(OnceboxRepositoryFactory.class, () -> repositoryFactory);
        context.registerBean(MeterRegistry.class, SimpleMeterRegistry::new);
        context.register(userClasses);
        context.register(InboxAutoConfiguration.class);
        context.refresh();
        return context;
    }

    private static Object innermostOf(IdempotentConsumer consumer) {
        IdempotentConsumer current = consumer;
        while (current instanceof AbstractIdempotentConsumerDecorator) {
            current = (IdempotentConsumer) ReflectionTestUtils.getField(current, "delegate");
        }
        return current;
    }

    static class TransactionalConstructorListener {

        final IdempotentConsumer consumer;

        TransactionalConstructorListener(@TransactionalConsumer IdempotentConsumer consumer) {
            this.consumer = consumer;
        }
    }

    static class NonTransactionalConstructorListener {

        final IdempotentConsumer consumer;

        NonTransactionalConstructorListener(@NonTransactionalConsumer IdempotentConsumer consumer) {
            this.consumer = consumer;
        }
    }

    static class TransactionalFieldListener {
        @Autowired
        @TransactionalConsumer
        IdempotentConsumer consumer;
    }

    static class NonTransactionalSetterListener {

        IdempotentConsumer consumer;

        @Autowired
        @NonTransactionalConsumer
        void setConsumer(IdempotentConsumer consumer) {
            this.consumer = consumer;
        }
    }

    static class BothConsumersListener {

        final IdempotentConsumer transactional;
        final IdempotentConsumer nonTransactional;

        BothConsumersListener(@TransactionalConsumer IdempotentConsumer transactional,
                              @NonTransactionalConsumer IdempotentConsumer nonTransactional) {
            this.transactional = transactional;
            this.nonTransactional = nonTransactional;
        }
    }

    static class BeanMethodListener {

        final IdempotentConsumer consumer;

        BeanMethodListener(IdempotentConsumer consumer) {
            this.consumer = consumer;
        }
    }

    @Configuration
    static class BeanMethodListenerConfiguration {

        @Bean
        BeanMethodListener beanMethodListener(@TransactionalConsumer IdempotentConsumer consumer) {
            return new BeanMethodListener(consumer);
        }
    }

    @Configuration
    static class UserTransactionalConfiguration {

        static final IdempotentConsumer USER_CONSUMER = mock(IdempotentConsumer.class);

        @Bean(defaultCandidate = false)
        IdempotentConsumer inboxUndecoratedTransactionalIdempotentConsumer() {
            return USER_CONSUMER;
        }
    }
}
