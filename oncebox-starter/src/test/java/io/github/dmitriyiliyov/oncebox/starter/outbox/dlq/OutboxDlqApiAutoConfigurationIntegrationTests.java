package io.github.dmitriyiliyov.oncebox.starter.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.dlq.api.*;
import io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq.OutboxDlqApiServiceMetricsDecorator;
import io.github.dmitriyiliyov.oncebox.tests.fixtures.PostgresTestContainerSingleton;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class OutboxDlqApiAutoConfigurationIntegrationTests {

    private final ApplicationContextRunner contextRunnerWithoutClock = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    OutboxDlqApiAutoConfiguration.class,
                    OutboxDlqApiMetricsAutoConfiguration.class
            ))
            .withBean("onceboxJdbcTemplate", JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
            .withPropertyValues(
                    "spring.datasource.url=" + PostgresTestContainerSingleton.INSTANCE.getJdbcUrl(),
                    "spring.datasource.driver-class-name=" + PostgresTestContainerSingleton.INSTANCE.getDriverClassName(),
                    "spring.datasource.username=" + PostgresTestContainerSingleton.INSTANCE.getUsername(),
                    "spring.datasource.password=" + PostgresTestContainerSingleton.INSTANCE.getPassword()
            );

    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final ApplicationContextRunner contextRunner = contextRunnerWithoutClock
            .withBean("userClock", Clock.class, Clock::systemDefaultZone);

    @Test
    @DisplayName("IT outboxDlqControllerAdvice when the application has one Clock should stamp problems with it")
    void outboxDlqControllerAdvice_whenApplicationHasOneClock_shouldStampProblemsWithIt() {
        contextRunnerWithoutClock
                .withBean("userClock", Clock.class, () -> Clock.fixed(FIXED_NOW, ZoneOffset.UTC))
                .withPropertyValues("oncebox.outbox.dlq.enabled=true")
                .run(context -> assertThat(stampOf(context.getBean(OutboxDlqControllerAdvice.class)))
                        .isEqualTo(FIXED_NOW.toString()));
    }

    @Test
    @DisplayName("IT context when the application has no Clock should start and register no Clock of its own")
    void context_whenApplicationHasNoClock_shouldStartWithoutRegisteringClock() {
        contextRunnerWithoutClock
                .withPropertyValues("oncebox.outbox.dlq.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(Clock.class);
                    assertThat(stampOf(context.getBean(OutboxDlqControllerAdvice.class))).isNotNull();
                });
    }

    @Test
    @DisplayName("IT context when the application has two Clocks should start and stamp problems by the UTC clock")
    void context_whenApplicationHasTwoClocks_shouldStartAndUseUtcClock() {
        contextRunnerWithoutClock
                .withBean("firstClock", Clock.class, () -> Clock.fixed(FIXED_NOW, ZoneOffset.UTC))
                .withBean("secondClock", Clock.class, () -> Clock.fixed(FIXED_NOW, ZoneOffset.UTC))
                .withPropertyValues("oncebox.outbox.dlq.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(stampOf(context.getBean(OutboxDlqControllerAdvice.class)))
                            .isNotEqualTo(FIXED_NOW.toString());
                });
    }

    @Test
    @DisplayName("IT should not register any beans when dlq.enabled property is missing")
    void shouldNotRegisterBeansWhenPropertyIsMissing() {
        contextRunner.run(this::assertNoBeans);
    }

    @Test
    @DisplayName("IT should not register any beans when dlq.enabled=false")
    void shouldNotRegisterBeansWhenPropertyIsFalse() {
        contextRunner
                .withPropertyValues("oncebox.outbox.dlq.enabled=false")
                .run(this::assertNoBeans);
    }

    @Test
    @DisplayName("IT should register all beans when dlq.enabled=true and metrics disabled")
    void shouldRegisterAllBeansWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "oncebox.outbox.dlq.enabled=true",
                        "oncebox.outbox.metrics.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OutboxDlqApiRepository.class);
                    assertThat(context).hasSingleBean(OutboxDlqController.class);
                    assertThat(context).hasSingleBean(DlqStatusQueryConverter.class);
                    assertThat(context).hasSingleBean(OutboxDlqControllerAdvice.class);

                    assertThat(context.getBean(OutboxDlqApiService.class))
                            .isExactlyInstanceOf(DefaultOutboxDlqApiService.class);
                    assertThat(context).doesNotHaveBean(OutboxDlqApiServiceMetricsDecorator.class);
                });
    }

    @Test
    @DisplayName("IT should register metrics decorator as primary when metrics.enabled=true")
    void shouldRegisterMetricsDecoratorWhenMetricsEnabled() {
        contextRunner
                .withPropertyValues(
                        "oncebox.outbox.dlq.enabled=true",
                        "oncebox.outbox.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasBean("outboxDlqApiService");
                    assertThat(context).hasBean("outboxDlqApiServiceMetricsDecorator");
                    assertThat(context.getBean(OutboxDlqApiService.class))
                            .isInstanceOf(OutboxDlqApiServiceMetricsDecorator.class);
                });
    }

    @Test
    @DisplayName("IT should not register metrics decorator when metrics.enabled is missing")
    void shouldNotRegisterMetricsDecoratorWhenMetricsPropertyMissing() {
        contextRunner
                .withPropertyValues("oncebox.outbox.dlq.enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OutboxDlqApiServiceMetricsDecorator.class);
                    assertThat(context.getBean(OutboxDlqApiService.class))
                            .isExactlyInstanceOf(DefaultOutboxDlqApiService.class);
                });
    }

    @Test
    @DisplayName("IT should not override custom OutboxDlqApiRepository")
    void shouldNotOverrideCustomRepository() {
        contextRunner
                .withPropertyValues("oncebox.outbox.dlq.enabled=true")
                .withUserConfiguration(CustomRepositoryConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(OutboxDlqApiRepository.class);
                    assertThat(context.getBean(OutboxDlqApiRepository.class))
                            .isSameAs(context.getBean("customRepository"));
                });
    }

    @Test
    @DisplayName("IT should not override custom OutboxDlqController")
    void shouldNotOverrideCustomController() {
        contextRunner
                .withPropertyValues("oncebox.outbox.dlq.enabled=true")
                .withUserConfiguration(CustomControllerConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(OutboxDlqController.class);
                    assertThat(context.getBean(OutboxDlqController.class))
                            .isSameAs(context.getBean("customController"));
                });
    }

    @Test
    @DisplayName("IT should not override custom DlqStatusQueryConverter")
    void shouldNotOverrideCustomConverter() {
        contextRunner
                .withPropertyValues("oncebox.outbox.dlq.enabled=true")
                .withUserConfiguration(CustomConverterConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(DlqStatusQueryConverter.class);
                    assertThat(context.getBean(DlqStatusQueryConverter.class))
                            .isSameAs(context.getBean("customConverter"));
                });
    }

    @Test
    @DisplayName("IT should not override custom OutboxDlqControllerAdvice")
    void shouldNotOverrideCustomAdvice() {
        contextRunner
                .withPropertyValues("oncebox.outbox.dlq.enabled=true")
                .withUserConfiguration(CustomAdviceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(OutboxDlqControllerAdvice.class);
                    assertThat(context.getBean(OutboxDlqControllerAdvice.class))
                            .isSameAs(context.getBean("customAdvice"));
                });
    }

    private static String stampOf(OutboxDlqControllerAdvice advice) {
        Object timestamp = advice.handleUnexpectedException(new IllegalStateException("boom"), new MockHttpServletRequest())
                .getProperties()
                .get("timestamp");
        return String.valueOf(timestamp);
    }

    private void assertNoBeans(AssertableApplicationContext context) {
        assertThat(context).doesNotHaveBean(OutboxDlqApiRepository.class);
        assertThat(context).doesNotHaveBean(OutboxDlqApiService.class);
        assertThat(context).doesNotHaveBean(OutboxDlqController.class);
        assertThat(context).doesNotHaveBean(DlqStatusQueryConverter.class);
        assertThat(context).doesNotHaveBean(OutboxDlqControllerAdvice.class);
    }

    @Configuration
    static class CustomRepositoryConfiguration {
        @Bean
        OutboxDlqApiRepository customRepository() {
            return mock(OutboxDlqApiRepository.class);
        }
    }

    @Configuration
    static class CustomManagerConfiguration {
        @Bean
        OutboxDlqApiService customManager() {
            return mock(OutboxDlqApiService.class);
        }
    }

    @Configuration
    static class CustomControllerConfiguration {
        @Bean
        OutboxDlqController customController() {
            return mock(OutboxDlqController.class);
        }
    }

    @Configuration
    static class CustomConverterConfiguration {
        @Bean
        DlqStatusQueryConverter customConverter() {
            return mock(DlqStatusQueryConverter.class);
        }
    }

    @Configuration
    static class CustomAdviceConfiguration {
        @Bean
        OutboxDlqControllerAdvice customAdvice() {
            return mock(OutboxDlqControllerAdvice.class);
        }
    }
}