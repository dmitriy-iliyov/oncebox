package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.starter.outbox.OutboxAutoConfiguration;
import io.github.dmitriyiliyov.oncebox.starter.outbox.dlq.OutboxDlqAutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OnAnyMetricsEnabledConditionIntegrationTests {

    @Configuration
    static class TestConfiguration {

        @Bean
        @Conditional(OnAnyMetricsEnabledCondition.class)
        String conditionalBean() {
            return "metrics-enabled-bean";
        }
    }

    private ApplicationContextRunner baseRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        TransactionAutoConfiguration.class,
                        JacksonAutoConfiguration.class,
                        OnceboxAutoConfiguration.class,
                        OutboxAutoConfiguration.class,
                        OutboxDlqAutoConfiguration.class
                ))
                .withUserConfiguration(TestConfiguration.class)
                .withBean(DataSource.class, () -> {
                    try {
                        DataSource mockDataSource = mock(DataSource.class);
                        Connection mockConnection = mock(Connection.class);
                        DatabaseMetaData mockMetaData = mock(DatabaseMetaData.class);
                        Mockito.when(mockDataSource.getConnection()).thenReturn(mockConnection);
                        Mockito.when(mockConnection.getMetaData()).thenReturn(mockMetaData);
                        Mockito.when(mockMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
                        return mockDataSource;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .withBean(KafkaTemplate.class, () -> {
                    KafkaTemplate<String, Object> mockTemplate = mock(KafkaTemplate.class);
                    ProducerFactory<String, Object> mockFactory = mock(ProducerFactory.class);
                    Mockito.when(mockTemplate.getProducerFactory()).thenReturn(mockFactory);
                    Mockito.when(mockFactory.getConfigurationProperties()).thenReturn(Collections.emptyMap());
                    return mockTemplate;
                })
                .withBean(TransactionTemplate.class, () -> mock(TransactionTemplate.class))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:postgresql://outbox-producer-postgresql:5432/outbox_example",
                        "spring.datasource.driver-class-name=org.postgresql.Driver",
                        "spring.datasource.username=admin",
                        "spring.datasource.password=root",
                        "oncebox.tables.auto-create=false",
                        "oncebox.outbox.sender.type=kafka",
                        "oncebox.outbox.events.my-event.topic=my.topic"
                );
    }

    @Test
    @DisplayName("IT should match when only outbox metrics enabled")
    void shouldMatch_whenOnlyOutboxMetricsEnabled() {
        baseRunner()
                .withPropertyValues("oncebox.outbox.metrics.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when only inbox metrics enabled")
    void shouldMatch_whenOnlyInboxMetricsEnabled() {
        baseRunner()
                .withPropertyValues("oncebox.inbox.metrics.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when both outbox and inbox metrics enabled")
    void shouldMatch_whenBothMetricsEnabled() {
        baseRunner()
                .withPropertyValues(
                        "oncebox.outbox.metrics.enabled=true",
                        "oncebox.inbox.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when outbox enabled and inbox explicitly disabled")
    void shouldMatch_whenOutboxEnabledAndInboxDisabled() {
        baseRunner()
                .withPropertyValues(
                        "oncebox.outbox.metrics.enabled=true",
                        "oncebox.inbox.metrics.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should match when inbox enabled and outbox explicitly disabled")
    void shouldMatch_whenInboxEnabledAndOutboxDisabled() {
        baseRunner()
                .withPropertyValues(
                        "oncebox.outbox.metrics.enabled=false",
                        "oncebox.inbox.metrics.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should not match when both metrics explicitly disabled")
    void shouldNotMatch_whenBothMetricsDisabled() {
        baseRunner()
                .withPropertyValues(
                        "oncebox.outbox.metrics.enabled=false",
                        "oncebox.inbox.metrics.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should not match when no metrics properties specified")
    void shouldNotMatch_whenNoPropertiesSpecified() {
        baseRunner()
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should not match when outbox absent and inbox explicitly disabled")
    void shouldNotMatch_whenOutboxAbsentAndInboxDisabled() {
        baseRunner()
                .withPropertyValues("oncebox.inbox.metrics.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("conditionalBean");
                });
    }

    @Test
    @DisplayName("IT should not match when inbox absent and outbox explicitly disabled")
    void shouldNotMatch_whenInboxAbsentAndOutboxDisabled() {
        baseRunner()
                .withPropertyValues("oncebox.outbox.metrics.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("conditionalBean");
                });
    }
}