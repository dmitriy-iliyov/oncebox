package io.github.dmitriyiliyov.oncebox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

// Which JdbcTemplate application code gets by type next to the oncebox one; no database is reached
class OnceboxJdbcTemplateIntegrationTests {

    @Test
    @DisplayName("IT context() when oncebox registers before Spring Boot should leave Boot jdbcTemplate to the application")
    void context_whenOnceboxRegistersBeforeSpringBoot_shouldLeaveBootJdbcTemplateToTheApplication() {
        // given
        ApplicationContextRunner runner = baseRunner()
                .withUserConfiguration(OnceboxAutoConfiguration.class, ApplicationConfiguration.class)
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        JdbcTemplateAutoConfiguration.class
                ));

        // when / then
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("onceboxJdbcTemplate");
            assertThat(context).hasBean("jdbcTemplate");
            assertThat(context.getBean(ApplicationJdbcDao.class).jdbc())
                    .as("JdbcTemplate injected into application code by type")
                    .isSameAs(context.getBean("jdbcTemplate"));
        });
    }

    @Test
    @DisplayName("IT context() when oncebox is auto-configuration should leave Boot jdbcTemplate to the application")
    void context_whenOnceboxIsAutoConfiguration_shouldLeaveBootJdbcTemplateToTheApplication() {
        // given
        ApplicationContextRunner runner = baseRunner()
                .withUserConfiguration(ApplicationConfiguration.class)
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        JdbcTemplateAutoConfiguration.class,
                        OnceboxAutoConfiguration.class
                ));

        // when / then
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("onceboxJdbcTemplate");
            assertThat(context).hasBean("jdbcTemplate");
            assertThat(context.getBean(ApplicationJdbcDao.class).jdbc())
                    .as("JdbcTemplate injected into application code by type")
                    .isSameAs(context.getBean("jdbcTemplate"));
        });
    }

    @Test
    @DisplayName("IT context() when Boot jdbcTemplate is absent should not offer onceboxJdbcTemplate for injection by type")
    void context_whenBootJdbcTemplateIsAbsent_shouldNotOfferOnceboxJdbcTemplateForInjectionByType() {
        // given
        ApplicationContextRunner runner = baseRunner()
                .withUserConfiguration(OnceboxAutoConfiguration.class, ApplicationConfiguration.class)
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class));

        // when / then
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(NoSuchBeanDefinitionException.class)
                    .hasMessageContaining(JdbcTemplate.class.getName());
        });
    }

    @Test
    @DisplayName("IT context() when onceboxJdbcTemplate is not a default candidate should still wire it into the repository factory by name")
    void context_whenOnceboxJdbcTemplateIsNotDefaultCandidate_shouldStillWireItIntoRepositoryFactory() {
        // given
        ApplicationContextRunner runner = baseRunner()
                .withUserConfiguration(OnceboxAutoConfiguration.class)
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        JdbcTemplateAutoConfiguration.class
                ));

        // when / then
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(OnceboxRepositoryFactory.class);
        });
    }

    private ApplicationContextRunner baseRunner() {
        return new ApplicationContextRunner()
                .withPropertyValues(
                        "oncebox.tables.auto-create=false",
                        "spring.datasource.url=jdbc:postgresql://localhost:1/oncebox",
                        "oncebox.outbox.enabled=false"
                );
    }

    record ApplicationJdbcDao(JdbcTemplate jdbc) {
    }

    @Configuration
    static class ApplicationConfiguration {

        // the parameter name matches no bean, so the template can only be resolved by type
        @Bean
        ApplicationJdbcDao applicationJdbcDao(JdbcTemplate jdbc) {
            return new ApplicationJdbcDao(jdbc);
        }
    }
}
