package io.github.dmitriyiliyov.oncebox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class OnAnyCleanUpEnabledConditionIntegrationTests {

    @Configuration
    static class TestConfiguration {

        @Bean
        @Conditional(OnAnyCleanUpEnabledCondition.class)
        String conditionalBean() {
            return "clean-up-enabled-bean";
        }
    }

    private ApplicationContextRunner baseRunner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration.class);
    }

    @Test
    @DisplayName("IT should match when only outbox clean-up enabled")
    void shouldMatch_whenOnlyOutboxCleanUpEnabled() {
        baseRunner()
                .withPropertyValues("oncebox.outbox.clean-up.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when only inbox clean-up enabled")
    void shouldMatch_whenOnlyInboxCleanUpEnabled() {
        baseRunner()
                .withPropertyValues("oncebox.inbox.clean-up.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when both outbox and inbox clean-up enabled")
    void shouldMatch_whenBothCleanUpEnabled() {
        baseRunner()
                .withPropertyValues(
                        "oncebox.outbox.clean-up.enabled=true",
                        "oncebox.inbox.clean-up.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when outbox clean-up enabled and inbox explicitly disabled")
    void shouldMatch_whenOutboxEnabledAndInboxDisabled() {
        baseRunner()
                .withPropertyValues(
                        "oncebox.outbox.clean-up.enabled=true",
                        "oncebox.inbox.clean-up.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when inbox clean-up enabled and outbox explicitly disabled")
    void shouldMatch_whenInboxEnabledAndOutboxDisabled() {
        baseRunner()
                .withPropertyValues(
                        "oncebox.outbox.clean-up.enabled=false",
                        "oncebox.inbox.clean-up.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should not match when both clean-up explicitly disabled")
    void shouldNotMatch_whenBothCleanUpDisabled() {
        baseRunner()
                .withPropertyValues(
                        "oncebox.outbox.clean-up.enabled=false",
                        "oncebox.inbox.clean-up.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when no clean-up properties specified")
    void shouldNotMatch_whenNoPropertiesSpecified() {
        baseRunner()
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when outbox absent and inbox explicitly disabled")
    void shouldNotMatch_whenOutboxAbsentAndInboxDisabled() {
        baseRunner()
                .withPropertyValues("oncebox.inbox.clean-up.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should not match when inbox absent and outbox explicitly disabled")
    void shouldNotMatch_whenInboxAbsentAndOutboxDisabled() {
        baseRunner()
                .withPropertyValues("oncebox.outbox.clean-up.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("conditionalBean");
                });
    }
}