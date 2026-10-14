package io.github.dmitriyiliyov.oncebox.starter.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxSerializerJacksonConditionUnitTests {

    private static final String SERIALIZER_BEAN = "outboxSerializer";

    private ApplicationContextRunner runner(AtomicBoolean definitionRegistered) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
                .withPropertyValues(
                        "oncebox.outbox.sender.type=kafka",
                        "oncebox.outbox.sender.bean-name=kafkaTemplate",
                        "oncebox.outbox.events.my-event.topic=my.topic"
                )
                .withInitializer(ctx -> ctx.addBeanFactoryPostProcessor(beanFactory ->
                        definitionRegistered.set(
                                Arrays.asList(beanFactory.getBeanDefinitionNames()).contains(SERIALIZER_BEAN)
                        )
                ));
    }

    @Test
    @DisplayName("UT registers outboxSerializer definition when Jackson is on the classpath")
    void outboxSerializer_isRegistered_whenJacksonPresent() {
        AtomicBoolean definitionRegistered = new AtomicBoolean();

        runner(definitionRegistered).run(ctx -> assertThat(definitionRegistered).isTrue());
    }

    @Test
    @DisplayName("UT skips outboxSerializer definition without classpath errors when Jackson is absent")
    void outboxSerializer_isSkipped_whenJacksonAbsent() {
        AtomicBoolean definitionRegistered = new AtomicBoolean(true);

        runner(definitionRegistered)
                .withClassLoader(new FilteredClassLoader(ObjectMapper.class))
                .run(ctx -> {
                    assertThat(definitionRegistered).isFalse();
                    assertThat(causeChainOf(ctx.getStartupFailure()))
                            .noneMatch(cause -> cause instanceof NoClassDefFoundError
                                    || cause instanceof ClassNotFoundException);
                });
    }

    private static Throwable[] causeChainOf(Throwable throwable) {
        return java.util.stream.Stream.iterate(throwable, java.util.Objects::nonNull, Throwable::getCause)
                .toArray(Throwable[]::new);
    }
}
