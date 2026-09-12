package io.github.dmitriyiliyov.oncebox.starter.publisher.dlq;

import io.github.dmitriyiliyov.oncebox.dlq.api.*;
import io.github.dmitriyiliyov.oncebox.metrics.publisher.dlq.OutboxDlqApiServiceMetricsDecorator;
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

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class OutboxDlqApiAutoConfigurationIntegrationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    OutboxDlqApiAutoConfiguration.class,
                    OutboxDlqApiMetricsAutoConfiguration.class
            ))
            .withBean("outboxJdbcTemplate", JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withBean(Clock.class, Clock::systemDefaultZone)
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
            .withPropertyValues(
                    "spring.datasource.url=" + PostgresTestContainerSingleton.INSTANCE.getJdbcUrl(),
                    "spring.datasource.driver-class-name=" + PostgresTestContainerSingleton.INSTANCE.getDriverClassName(),
                    "spring.datasource.username=" + PostgresTestContainerSingleton.INSTANCE.getUsername(),
                    "spring.datasource.password=" + PostgresTestContainerSingleton.INSTANCE.getPassword()
            );

    @Test
    @DisplayName("IT should not register any beans when dlq.enabled property is missing")
    void shouldNotRegisterBeansWhenPropertyIsMissing() {
        contextRunner.run(this::assertNoBeans);
    }

    @Test
    @DisplayName("IT should not register any beans when dlq.enabled=false")
    void shouldNotRegisterBeansWhenPropertyIsFalse() {
        contextRunner
                .withPropertyValues("oncebox.publisher.dlq.enabled=false")
                .run(this::assertNoBeans);
    }

    @Test
    @DisplayName("IT should register all beans when dlq.enabled=true and metrics disabled")
    void shouldRegisterAllBeansWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "oncebox.publisher.dlq.enabled=true",
                        "oncebox.publisher.metrics.enabled=false"
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
                        "oncebox.publisher.dlq.enabled=true",
                        "oncebox.publisher.metrics.enabled=true"
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
                .withPropertyValues("oncebox.publisher.dlq.enabled=true")
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
                .withPropertyValues("oncebox.publisher.dlq.enabled=true")
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
                .withPropertyValues("oncebox.publisher.dlq.enabled=true")
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
                .withPropertyValues("oncebox.publisher.dlq.enabled=true")
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
                .withPropertyValues("oncebox.publisher.dlq.enabled=true")
                .withUserConfiguration(CustomAdviceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(OutboxDlqControllerAdvice.class);
                    assertThat(context.getBean(OutboxDlqControllerAdvice.class))
                            .isSameAs(context.getBean("customAdvice"));
                });
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