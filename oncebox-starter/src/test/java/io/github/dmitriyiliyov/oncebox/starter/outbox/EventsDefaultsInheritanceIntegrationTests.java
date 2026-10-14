package io.github.dmitriyiliyov.oncebox.starter.outbox;

import io.github.dmitriyiliyov.oncebox.starter.OnceboxAutoConfiguration;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.PollingType;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class EventsDefaultsInheritanceIntegrationTests {

    private ApplicationContextRunner getBaseContextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        TransactionAutoConfiguration.class,
                        JacksonAutoConfiguration.class,
                        OnceboxAutoConfiguration.class,
                        OutboxAutoConfiguration.class,
                        OutboxDlqAutoConfiguration.class
                ))
                .withBean(DataSource.class, () -> {
                    try {
                        DataSource mockDataSource = Mockito.mock(DataSource.class);
                        Connection mockConnection = Mockito.mock(Connection.class);
                        DatabaseMetaData mockMetaData = Mockito.mock(DatabaseMetaData.class);

                        Mockito.when(mockDataSource.getConnection()).thenReturn(mockConnection);
                        Mockito.when(mockConnection.getMetaData()).thenReturn(mockMetaData);
                        Mockito.when(mockMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");

                        return mockDataSource;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .withBean(KafkaTemplate.class, () -> {
                    KafkaTemplate<String, Object> mockTemplate = Mockito.mock(KafkaTemplate.class);
                    ProducerFactory<String, Object> mockFactory = Mockito.mock(ProducerFactory.class);

                    Mockito.when(mockTemplate.getProducerFactory()).thenReturn(mockFactory);
                    Mockito.when(mockFactory.getConfigurationProperties()).thenReturn(Collections.emptyMap());

                    return mockTemplate;
                })
                .withBean(TransactionTemplate.class, () -> Mockito.mock(TransactionTemplate.class))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:postgresql://outbox-producer-postgresql:5432/outbox_example",
                        "spring.datasource.driver-class-name=org.postgresql.Driver",
                        "spring.datasource.username=admin",
                        "spring.datasource.password=root",
                        "oncebox.tables.auto-create=false",
                        "oncebox.outbox.enabled=true",
                        "oncebox.outbox.sender.type=kafka",
                        "oncebox.outbox.events.my-event.topic=my.topic",
                        "oncebox.outbox.dlq.enabled=true"
                );
    }

    private ApplicationContextRunner getPropertiesRunner() {
        return getBaseContextRunner().withPropertyValues(
                "oncebox.outbox.defaults.batch-size=200",
                "oncebox.outbox.defaults.polling.type=adaptive",
                "oncebox.outbox.defaults.polling.initial-delay=5m",
                "oncebox.outbox.defaults.polling.min-fixed-delay=1s",
                "oncebox.outbox.defaults.polling.max-fixed-delay=1m",
                "oncebox.outbox.defaults.polling.multiplier=1.5",
                "oncebox.outbox.defaults.max-retries=3",
                "oncebox.outbox.defaults.backoff.enabled=true",
                "oncebox.outbox.defaults.backoff.delay=10s",
                "oncebox.outbox.defaults.backoff.multiplier=3.0"
        );
    }

    @Test
    @DisplayName("IT High Priority should inherit defaults and override specific polling")
    void highPriority_inheritsAndOverrides() {
        getPropertiesRunner()
                .withPropertyValues(
                        "oncebox.outbox.events.high-priority.topic=events",
                        "oncebox.outbox.events.high-priority.polling.min-fixed-delay=250ms"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OnceboxProperties properties = context.getBean(OnceboxProperties.class);
                    var highPriority = properties.getOutbox().getEvents().get("high-priority");

                    assertThat(highPriority).isNotNull();
                    assertThat(highPriority.getBatchSize()).isEqualTo(200);
                    assertThat(highPriority.getMaxRetries()).isEqualTo(3);
                    assertThat(highPriority.getPolling().getType()).isEqualTo(PollingType.ADAPTIVE);
                    assertThat(highPriority.getPolling().getMinFixedDelay()).isEqualTo(Duration.ofMillis(250));
                    assertThat(highPriority.getPolling().getMultiplier()).isEqualTo(1.5);
                    assertThat(highPriority.getPolling().getMaxFixedDelay()).isEqualTo(Duration.ofMinutes(1));
                });
    }

    @Test
    @DisplayName("IT Low Priority should disable backoff and override polling block")
    void lowPriority_overridesPollingAndBackoff() {
        getPropertiesRunner()
                .withPropertyValues(
                        "oncebox.outbox.events.low-priority.topic=events",
                        "oncebox.outbox.events.low-priority.polling.min-fixed-delay=10s",
                        "oncebox.outbox.events.low-priority.polling.max-fixed-delay=5m",
                        "oncebox.outbox.events.low-priority.polling.multiplier=2.0",
                        "oncebox.outbox.events.low-priority.backoff.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OnceboxProperties properties = context.getBean(OnceboxProperties.class);
                    var lowPriority = properties.getOutbox().getEvents().get("low-priority");

                    assertThat(lowPriority).isNotNull();
                    assertThat(lowPriority.getBackoff().isEnabled()).isFalse();
                    assertThat(lowPriority.getPolling().getType()).isEqualTo(PollingType.ADAPTIVE);
                    assertThat(lowPriority.getPolling().getMinFixedDelay()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(lowPriority.getPolling().getMaxFixedDelay()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(lowPriority.getPolling().getMultiplier()).isEqualTo(2.0);
                    assertThat(lowPriority.getBatchSize()).isEqualTo(200);
                });
    }

    @Test
    @DisplayName("IT Default Priority should inherit exactly all defaults without overrides")
    void defaultPriority_inheritsEverything() {
        getPropertiesRunner()
                .withPropertyValues(
                        "oncebox.outbox.events.default-priority.topic=events"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OnceboxProperties properties = context.getBean(OnceboxProperties.class);
                    var defaultPriority = properties.getOutbox().getEvents().get("default-priority");

                    assertThat(defaultPriority).isNotNull();
                    assertThat(defaultPriority.getBatchSize()).isEqualTo(200);
                    assertThat(defaultPriority.getMaxRetries()).isEqualTo(3);

                    assertThat(defaultPriority.getPolling().getType()).isEqualTo(PollingType.ADAPTIVE);
                    assertThat(defaultPriority.getPolling().getInitialDelay()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(defaultPriority.getPolling().getMinFixedDelay()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(defaultPriority.getPolling().getMaxFixedDelay()).isEqualTo(Duration.ofMinutes(1));
                    assertThat(defaultPriority.getPolling().getMultiplier()).isEqualTo(1.5);

                    assertThat(defaultPriority.getBackoff().isEnabled()).isTrue();
                    assertThat(defaultPriority.getBackoff().getDelay()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(defaultPriority.getBackoff().getMultiplier()).isEqualTo(3.0);
                });
    }

    @Test
    @DisplayName("IT Fixed Priority should switch type to FIXED and apply specific properties")
    void fixedPriority_switchesToFixedPolling() {
        getPropertiesRunner()
                .withPropertyValues(
                        "oncebox.outbox.events.fixed-priority.topic=events",
                        "oncebox.outbox.events.fixed-priority.polling.type=fixed",
                        "oncebox.outbox.events.fixed-priority.polling.fixed-delay=2s"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OnceboxProperties properties = context.getBean(OnceboxProperties.class);
                    var fixedPriority = properties.getOutbox().getEvents().get("fixed-priority");

                    assertThat(fixedPriority).isNotNull();
                    assertThat(fixedPriority.getBatchSize()).isEqualTo(200);

                    assertThat(fixedPriority.getPolling().getType()).isEqualTo(PollingType.FIXED);
                    assertThat(fixedPriority.getPolling().getInitialDelay()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(fixedPriority.getPolling().getFixedDelay()).isEqualTo(Duration.ofSeconds(2));

                    assertThat(fixedPriority.getPolling().getMinFixedDelay()).isZero();
                    assertThat(fixedPriority.getPolling().getMaxFixedDelay()).isZero();
                    assertThat(fixedPriority.getPolling().getMultiplier()).isNaN();
                });
    }

    @Test
    @DisplayName("IT Complete Override should ignore defaults and apply all event specific values")
    void completeOverride_overridesAllDefaults() {
        getPropertiesRunner()
                .withPropertyValues(
                        "oncebox.outbox.events.full-override.topic=custom-topic",
                        "oncebox.outbox.events.full-override.batch-size=500",
                        "oncebox.outbox.events.full-override.max-retries=10",
                        "oncebox.outbox.events.full-override.polling.type=adaptive",
                        "oncebox.outbox.events.full-override.polling.initial-delay=1s",
                        "oncebox.outbox.events.full-override.polling.min-fixed-delay=100ms",
                        "oncebox.outbox.events.full-override.polling.max-fixed-delay=200ms",
                        "oncebox.outbox.events.full-override.polling.multiplier=5.0",
                        "oncebox.outbox.events.full-override.backoff.enabled=true",
                        "oncebox.outbox.events.full-override.backoff.delay=2s",
                        "oncebox.outbox.events.full-override.backoff.multiplier=4.0"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OnceboxProperties properties = context.getBean(OnceboxProperties.class);
                    var fullOverride = properties.getOutbox().getEvents().get("full-override");

                    assertThat(fullOverride).isNotNull();
                    assertThat(fullOverride.getTopic()).isEqualTo("custom-topic");
                    assertThat(fullOverride.getBatchSize()).isEqualTo(500);
                    assertThat(fullOverride.getMaxRetries()).isEqualTo(10);

                    assertThat(fullOverride.getPolling().getType()).isEqualTo(PollingType.ADAPTIVE);
                    assertThat(fullOverride.getPolling().getInitialDelay()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(fullOverride.getPolling().getMinFixedDelay()).isEqualTo(Duration.ofMillis(100));
                    assertThat(fullOverride.getPolling().getMaxFixedDelay()).isEqualTo(Duration.ofMillis(200));
                    assertThat(fullOverride.getPolling().getMultiplier()).isEqualTo(5.0);

                    assertThat(fullOverride.getBackoff().isEnabled()).isTrue();
                    assertThat(fullOverride.getBackoff().getDelay()).isEqualTo(Duration.ofSeconds(2));
                    assertThat(fullOverride.getBackoff().getMultiplier()).isEqualTo(4.0);
                });
    }

    @Test
    @DisplayName("IT Partial Backoff Override should inherit enabled state but apply specific delay and multiplier")
    void partialBackoffOverride_inheritsEnabledState() {
        getPropertiesRunner()
                .withPropertyValues(
                        "oncebox.outbox.events.custom-backoff.topic=events",
                        "oncebox.outbox.events.custom-backoff.backoff.delay=15s",
                        "oncebox.outbox.events.custom-backoff.backoff.multiplier=2.5"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OnceboxProperties properties = context.getBean(OnceboxProperties.class);
                    var customBackoff = properties.getOutbox().getEvents().get("custom-backoff");

                    assertThat(customBackoff).isNotNull();
                    assertThat(customBackoff.getBackoff().isEnabled()).isTrue();
                    assertThat(customBackoff.getBackoff().getDelay()).isEqualTo(Duration.ofSeconds(15));
                    assertThat(customBackoff.getBackoff().getMultiplier()).isEqualTo(2.5);
                });
    }

    @Test
    @DisplayName("IT Initial Delay Override should apply specific initial delay and inherit the rest of the polling configuration")
    void overrideInitialDelayOnly_inheritsRestOfPolling() {
        getPropertiesRunner()
                .withPropertyValues(
                        "oncebox.outbox.events.fast-start.topic=events",
                        "oncebox.outbox.events.fast-start.polling.initial-delay=10s"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OnceboxProperties properties = context.getBean(OnceboxProperties.class);
                    var fastStart = properties.getOutbox().getEvents().get("fast-start");

                    assertThat(fastStart).isNotNull();
                    assertThat(fastStart.getPolling().getType()).isEqualTo(PollingType.ADAPTIVE);
                    assertThat(fastStart.getPolling().getInitialDelay()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(fastStart.getPolling().getMinFixedDelay()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(fastStart.getPolling().getMaxFixedDelay()).isEqualTo(Duration.ofMinutes(1));
                    assertThat(fastStart.getPolling().getMultiplier()).isEqualTo(1.5);
                });
    }
}