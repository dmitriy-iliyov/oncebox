package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

public class CompositePostApplicationReadyOnceboxInitializerIntegrationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OnceboxAutoConfiguration.class))
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withBean(DistributedLockRepository.class, () -> mock(DistributedLockRepository.class))
            .withPropertyValues("oncebox.tables.auto-create=false", "oncebox.outbox.enabled=false");

    @Test
    @DisplayName("IT should call init on all injected PostApplicationReadyOnceboxInitializer beans directly")
    void shouldCallInitOnAllInitializersWhenCalledDirectly() {
        contextRunner
                .withBean("mockInitializer1", PostApplicationReadyOnceboxInitializer.class, () -> mock(PostApplicationReadyOnceboxInitializer.class))
                .withBean("mockInitializer2", PostApplicationReadyOnceboxInitializer.class, () -> mock(PostApplicationReadyOnceboxInitializer.class))
                .run(ctx -> {
                    PostApplicationReadyOnceboxInitializer mock1 = ctx.getBean("mockInitializer1", PostApplicationReadyOnceboxInitializer.class);
                    PostApplicationReadyOnceboxInitializer mock2 = ctx.getBean("mockInitializer2", PostApplicationReadyOnceboxInitializer.class);

                    CompositePostApplicationReadyOnceboxInitializer composite =
                            ctx.getBean(CompositePostApplicationReadyOnceboxInitializer.class);

                    composite.init();

                    verify(mock1, times(1)).init();
                    verify(mock2, times(1)).init();
                });
    }

    @Test
    @DisplayName("IT should invoke init on nested initializers when ApplicationReadyEvent is fired")
    void shouldInvokeInitWhenApplicationReadyEventFired() {
        contextRunner
                .withBean("mockInitializer", PostApplicationReadyOnceboxInitializer.class, () -> mock(PostApplicationReadyOnceboxInitializer.class))
                .run(ctx -> {
                    PostApplicationReadyOnceboxInitializer mockInitializer = ctx.getBean("mockInitializer", PostApplicationReadyOnceboxInitializer.class);

                    ctx.publishEvent(new ApplicationReadyEvent(
                            mock(SpringApplication.class),
                            new String[]{},
                            ctx.getSourceApplicationContext(),
                            Duration.ZERO
                    ));

                    verify(mockInitializer, times(1)).init();
                });
    }

    @Test
    @DisplayName("IT should work gracefully when there are no other initializer beans")
    void shouldWorkWithNoOtherInitializerBeans() {
        contextRunner.run(ctx ->
                assertThatCode(() ->
                        ctx.getBean(CompositePostApplicationReadyOnceboxInitializer.class).init()
                ).doesNotThrowAnyException()
        );
    }

    @Test
    @DisplayName("IT should call init twice on nested beans when composite init is called twice")
    void shouldCallInitTwiceWhenInitCalledTwice() {
        contextRunner
                .withBean("mockInitializer", PostApplicationReadyOnceboxInitializer.class, () -> mock(PostApplicationReadyOnceboxInitializer.class))
                .run(ctx -> {
                    PostApplicationReadyOnceboxInitializer mockInitializer = ctx.getBean("mockInitializer", PostApplicationReadyOnceboxInitializer.class);
                    CompositePostApplicationReadyOnceboxInitializer composite =
                            ctx.getBean(CompositePostApplicationReadyOnceboxInitializer.class);

                    composite.init();
                    composite.init();

                    verify(mockInitializer, times(2)).init();
                });
    }
}