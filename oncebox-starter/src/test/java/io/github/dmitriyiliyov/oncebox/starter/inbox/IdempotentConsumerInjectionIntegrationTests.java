package io.github.dmitriyiliyov.oncebox.starter.inbox;

import io.github.dmitriyiliyov.oncebox.core.inbox.*;
import io.github.dmitriyiliyov.oncebox.metrics.inbox.IdempotentConsumerMetricsDecorator;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxRepositoryFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * What user code receives when it asks for an {@link IdempotentConsumer}. Listeners use field injection, so the
 * field name is known to the container whatever the compiler flags are.
 */
class IdempotentConsumerInjectionIntegrationTests {

    private static final String TRANSACTIONAL = "inboxTransactionalIdempotentConsumer";
    private static final String NON_TRANSACTIONAL = "inboxNonTransactionalIdempotentConsumer";
    private static final String UNDECORATED_TRANSACTIONAL = "inboxUndecoratedTransactionalIdempotentConsumer";
    private static final String UNDECORATED_NON_TRANSACTIONAL = "inboxUndecoratedNonTransactionalIdempotentConsumer";

    @Test
    @DisplayName("IT context when inbox enabled should register both implementations, raw and decorated")
    void context_whenInboxEnabled_shouldRegisterBothImplementationsRawAndDecorated() {
        contextRunner()
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(UNDECORATED_TRANSACTIONAL)).isInstanceOf(TransactionalIdempotentConsumer.class);
                    assertThat(context.getBean(UNDECORATED_NON_TRANSACTIONAL))
                            .isInstanceOf(NonTransactionalIdempotentConsumer.class);
                    assertThat(innermostOf(context.getBean(TRANSACTIONAL, IdempotentConsumer.class)))
                            .isSameAs(context.getBean(UNDECORATED_TRANSACTIONAL));
                    assertThat(innermostOf(context.getBean(NON_TRANSACTIONAL, IdempotentConsumer.class)))
                            .isSameAs(context.getBean(UNDECORATED_NON_TRANSACTIONAL));
                });
    }

    @Test
    @DisplayName("IT injection by type without qualifier should fail the context: the guarantee is chosen explicitly")
    void injection_whenByTypeWithoutQualifier_shouldFailContext() {
        contextRunner()
                .withBean(UnqualifiedListener.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).rootCause()
                            .isInstanceOf(NoUniqueBeanDefinitionException.class);
                });
    }

    @Test
    @DisplayName("IT injection with the transactional qualifier should inject the decorated transactional consumer")
    void injection_whenTransactionalQualifier_shouldInjectDecoratedTransactionalConsumer() {
        contextRunner()
                .withBean(TransactionalListener.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    IdempotentConsumer injected = context.getBean(TransactionalListener.class).consumer;
                    assertThat(injected).isSameAs(context.getBean(TRANSACTIONAL));
                    assertThat(injected).isInstanceOf(IdempotentConsumerMetricsDecorator.class);
                    assertThat(innermostOf(injected)).isInstanceOf(TransactionalIdempotentConsumer.class);
                });
    }

    @Test
    @DisplayName("IT injection with the non-transactional qualifier should inject the decorated non-transactional consumer")
    void injection_whenNonTransactionalQualifier_shouldInjectDecoratedNonTransactionalConsumer() {
        contextRunner()
                .withBean(NonTransactionalListener.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    IdempotentConsumer injected = context.getBean(NonTransactionalListener.class).consumer;
                    assertThat(injected).isSameAs(context.getBean(NON_TRANSACTIONAL));
                    assertThat(injected).isInstanceOf(IdempotentConsumerMetricsDecorator.class);
                    assertThat(innermostOf(injected)).isInstanceOf(NonTransactionalIdempotentConsumer.class);
                });
    }

    @Test
    @DisplayName("IT injection when the field is named as the decorated bean should inject that bean")
    void injection_whenFieldNamedAsDecoratedBean_shouldInjectThatBean() {
        contextRunner()
                .withBean(NamedAsDecoratedListener.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(NamedAsDecoratedListener.class).inboxNonTransactionalIdempotentConsumer)
                            .isSameAs(context.getBean(NON_TRANSACTIONAL));
                });
    }

    @Test
    @DisplayName("IT injection when the field is named as the undecorated bean should fail: raw beans need a qualifier")
    void injection_whenFieldNamedAsUndecoratedBean_shouldFailContext() {
        contextRunner()
                .withBean(NamedAsUndecoratedListener.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).rootCause()
                            .isInstanceOf(NoUniqueBeanDefinitionException.class);
                });
    }

    @Test
    @DisplayName("IT injection with the undecorated qualifier should inject the raw consumer")
    void injection_whenUndecoratedQualifier_shouldInjectRawConsumer() {
        contextRunner()
                .withBean(UndecoratedListener.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(UndecoratedListener.class).consumer)
                            .isInstanceOf(NonTransactionalIdempotentConsumer.class)
                            .isSameAs(context.getBean(UNDECORATED_NON_TRANSACTIONAL));
                });
    }

    @Test
    @DisplayName("IT injection of a list should inject only the two decorated consumers")
    void injection_whenList_shouldInjectOnlyDecoratedConsumers() {
        contextRunner()
                .withBean(ListListener.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(ListListener.class).consumers)
                            .containsExactlyInAnyOrder(
                                    context.getBean(TRANSACTIONAL, IdempotentConsumer.class),
                                    context.getBean(NON_TRANSACTIONAL, IdempotentConsumer.class)
                            );
                });
    }

    @Test
    @DisplayName("IT context when user defines the undecorated transactional bean should decorate the user's bean")
    void context_whenUserDefinesUndecoratedTransactional_shouldDecorateUserBean() {
        contextRunner()
                .withUserConfiguration(UserTransactionalConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(TransactionalIdempotentConsumer.class);
                    assertThat(innermostOf(context.getBean(TRANSACTIONAL, IdempotentConsumer.class)))
                            .isSameAs(UserTransactionalConfiguration.USER_CONSUMER);
                });
    }

    @Test
    @DisplayName("IT context when user defines the undecorated non-transactional bean should decorate the user's bean")
    void context_whenUserDefinesUndecoratedNonTransactional_shouldDecorateUserBean() {
        contextRunner()
                .withUserConfiguration(UserNonTransactionalConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(NonTransactionalIdempotentConsumer.class);
                    assertThat(innermostOf(context.getBean(NON_TRANSACTIONAL, IdempotentConsumer.class)))
                            .isSameAs(UserNonTransactionalConfiguration.USER_CONSUMER);
                });
    }

    @Test
    @DisplayName("IT context when user's undecorated bean is a default candidate should let it into injection by type")
    void context_whenUserUndecoratedBeanIsDefaultCandidate_shouldLetItIntoInjectionByType() {
        IdempotentConsumer userConsumer = mock(IdempotentConsumer.class);
        contextRunner()
                .withBean(UNDECORATED_TRANSACTIONAL, IdempotentConsumer.class, () -> userConsumer)
                .withBean(ListListener.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(ListListener.class).consumers)
                            .hasSize(3)
                            .contains(userConsumer);
                });
    }

    private ApplicationContextRunner contextRunner() {
        OnceboxRepositoryFactory repositoryFactory = mock(OnceboxRepositoryFactory.class);
        when(repositoryFactory.createInboxRepository()).thenReturn(mock(InboxRepository.class));

        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(InboxAutoConfiguration.class))
                .withBean(InboxProperties.class, () -> mock(InboxProperties.class))
                .withBean(TransactionTemplate.class, () -> mock(TransactionTemplate.class))
                .withBean(OnceboxRepositoryFactory.class, () -> repositoryFactory)
                .withBean(Clock.class, Clock::systemUTC)
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues(
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.metrics.enabled=true"
                );
    }

    private static Object innermostOf(IdempotentConsumer consumer) {
        IdempotentConsumer current = consumer;
        while (current instanceof AbstractIdempotentConsumerDecorator) {
            current = (IdempotentConsumer) ReflectionTestUtils.getField(current, "delegate");
        }
        return current;
    }

    static class UnqualifiedListener {
        @Autowired
        IdempotentConsumer idempotentConsumer;
    }

    static class TransactionalListener {
        @Autowired
        @Qualifier(TRANSACTIONAL)
        IdempotentConsumer consumer;
    }

    static class NonTransactionalListener {
        @Autowired
        @Qualifier(NON_TRANSACTIONAL)
        IdempotentConsumer consumer;
    }

    static class NamedAsDecoratedListener {
        @Autowired
        IdempotentConsumer inboxNonTransactionalIdempotentConsumer;
    }

    static class NamedAsUndecoratedListener {
        @Autowired
        IdempotentConsumer inboxUndecoratedNonTransactionalIdempotentConsumer;
    }

    static class UndecoratedListener {
        @Autowired
        @Qualifier(UNDECORATED_NON_TRANSACTIONAL)
        IdempotentConsumer consumer;
    }

    static class ListListener {
        @Autowired
        List<IdempotentConsumer> consumers;
    }

    @Configuration
    static class UserTransactionalConfiguration {

        static final IdempotentConsumer USER_CONSUMER = mock(IdempotentConsumer.class);

        @Bean(defaultCandidate = false)
        IdempotentConsumer inboxUndecoratedTransactionalIdempotentConsumer() {
            return USER_CONSUMER;
        }
    }

    @Configuration
    static class UserNonTransactionalConfiguration {

        static final IdempotentConsumer USER_CONSUMER = mock(IdempotentConsumer.class);

        @Bean(defaultCandidate = false)
        IdempotentConsumer inboxUndecoratedNonTransactionalIdempotentConsumer() {
            return USER_CONSUMER;
        }
    }
}
