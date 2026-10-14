package io.github.dmitriyiliyov.oncebox.starter.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.starter.OnceboxAutoConfiguration;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.PollingType;
import io.github.dmitriyiliyov.oncebox.starter.outbox.OutboxAutoConfiguration;
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

public class DlqDefaultsInheritanceIntegrationTests {

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
                        "oncebox.outbox.sender.type=kafka",
                        "oncebox.outbox.events.my-event.topic=my.topic"
                );
    }

    private ApplicationContextRunner getDlqPropertiesRunner() {
        return getBaseContextRunner().withPropertyValues(
                "oncebox.outbox.dlq.enabled=true",
                "oncebox.outbox.dlq.batch-size=500",
                "oncebox.outbox.dlq.polling.type=adaptive",
                "oncebox.outbox.dlq.polling.initial-delay=5m",
                "oncebox.outbox.dlq.polling.min-fixed-delay=1s",
                "oncebox.outbox.dlq.polling.max-fixed-delay=2m",
                "oncebox.outbox.dlq.polling.multiplier=10.0"
        );
    }

    @Test
    @DisplayName("IT Transfer-To and Transfer-From should inherit all defaults when not specified")
    void fullInheritance_whenNoOverridesSpecified() {
        getDlqPropertiesRunner()
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OnceboxProperties properties = context.getBean(OnceboxProperties.class);
                    var dlq = properties.getOutbox().getDlq();

                    assertThat(dlq.isEnabled()).isTrue();

                    var transferTo = dlq.getTransferTo();
                    assertThat(transferTo.getBatchSize()).isEqualTo(500);
                    assertThat(transferTo.getPolling().getType()).isEqualTo(PollingType.ADAPTIVE);
                    assertThat(transferTo.getPolling().getInitialDelay()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(transferTo.getPolling().getMinFixedDelay()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(transferTo.getPolling().getMaxFixedDelay()).isEqualTo(Duration.ofMinutes(2));
                    assertThat(transferTo.getPolling().getMultiplier()).isEqualTo(10.0);

                    var transferFrom = dlq.getTransferFrom();
                    assertThat(transferFrom.getBatchSize()).isEqualTo(500);
                    assertThat(transferFrom.getPolling().getType()).isEqualTo(PollingType.ADAPTIVE);
                    assertThat(transferFrom.getPolling().getInitialDelay()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(transferFrom.getPolling().getMinFixedDelay()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(transferFrom.getPolling().getMaxFixedDelay()).isEqualTo(Duration.ofMinutes(2));
                    assertThat(transferFrom.getPolling().getMultiplier()).isEqualTo(10.0);
                });
    }

    @Test
    @DisplayName("IT Transfer-To should partially override polling multiplier and inherit the rest")
    void partialOverride_transferTo_inheritsRestOfPolling() {
        getDlqPropertiesRunner()
                .withPropertyValues(
                        "oncebox.outbox.dlq.transfer-to.polling.multiplier=2.5"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OnceboxProperties properties = context.getBean(OnceboxProperties.class);
                    var transferTo = properties.getOutbox().getDlq().getTransferTo();

                    assertThat(transferTo.getBatchSize()).isEqualTo(500);

                    assertThat(transferTo.getPolling().getType()).isEqualTo(PollingType.ADAPTIVE);
                    assertThat(transferTo.getPolling().getInitialDelay()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(transferTo.getPolling().getMinFixedDelay()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(transferTo.getPolling().getMaxFixedDelay()).isEqualTo(Duration.ofMinutes(2));

                    assertThat(transferTo.getPolling().getMultiplier()).isEqualTo(2.5);
                });
    }

    @Test
    @DisplayName("IT Transfer-From should completely override settings and switch polling to FIXED")
    void completeOverride_transferFrom_switchesToFixedPolling() {
        getDlqPropertiesRunner()
                .withPropertyValues(
                        "oncebox.outbox.dlq.transfer-from.batch-size=1000",
                        "oncebox.outbox.dlq.transfer-from.polling.type=fixed",
                        "oncebox.outbox.dlq.transfer-from.polling.initial-delay=5m",
                        "oncebox.outbox.dlq.transfer-from.polling.fixed-delay=1m"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OnceboxProperties properties = context.getBean(OnceboxProperties.class);
                    var transferFrom = properties.getOutbox().getDlq().getTransferFrom();

                    assertThat(transferFrom.getBatchSize()).isEqualTo(1000);

                    assertThat(transferFrom.getPolling().getType()).isEqualTo(PollingType.FIXED);
                    assertThat(transferFrom.getPolling().getInitialDelay()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(transferFrom.getPolling().getFixedDelay()).isEqualTo(Duration.ofMinutes(1));

                    assertThat(transferFrom.getPolling().getMinFixedDelay()).isZero();
                    assertThat(transferFrom.getPolling().getMaxFixedDelay()).isZero();
                    assertThat(transferFrom.getPolling().getMultiplier()).isNaN();
                });
    }

    @Test
    @DisplayName("IT DLQ should apply hardcoded defaults when only enabled is set to true")
    void pureDefaults_whenOnlyEnabledSet() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.outbox.dlq.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OnceboxProperties properties = context.getBean(OnceboxProperties.class);
                    var dlq = properties.getOutbox().getDlq();

                    assertThat(dlq.isEnabled()).isTrue();

                    var transferTo = dlq.getTransferTo();
                    assertThat(transferTo.getBatchSize()).isEqualTo(500);
                    assertThat(transferTo.getPolling().getType()).isEqualTo(PollingType.ADAPTIVE);
                    assertThat(transferTo.getPolling().getInitialDelay()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(transferTo.getPolling().getMinFixedDelay()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(transferTo.getPolling().getMaxFixedDelay()).isEqualTo(Duration.ofMinutes(2));
                    assertThat(transferTo.getPolling().getMultiplier()).isEqualTo(10.0);

                    var transferFrom = dlq.getTransferFrom();
                    assertThat(transferFrom.getBatchSize()).isEqualTo(500);
                    assertThat(transferFrom.getPolling().getType()).isEqualTo(PollingType.ADAPTIVE);
                    assertThat(transferFrom.getPolling().getInitialDelay()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(transferFrom.getPolling().getMinFixedDelay()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(transferFrom.getPolling().getMaxFixedDelay()).isEqualTo(Duration.ofMinutes(2));
                    assertThat(transferFrom.getPolling().getMultiplier()).isEqualTo(10.0);
                });
    }

    @Test
    @DisplayName("IT DLQ should be properly disabled when explicitly set to false")
    void disabledDlq_respectsFlag() {
        getBaseContextRunner()
                .withPropertyValues(
                        "oncebox.outbox.dlq.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    OnceboxProperties properties = context.getBean(OnceboxProperties.class);
                    assertThat(properties.getOutbox().getDlq().isEnabled()).isFalse();
                });
    }

    @Test
    @DisplayName("IT Fixed Polling should not fallback to fixed-delay if not specified in YAML")
    void fixedPolling_fallbackTo5sDefault() {
        getDlqPropertiesRunner()
                .withPropertyValues(
                        "oncebox.outbox.dlq.transfer-to.polling.type=fixed"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    OnceboxProperties properties = context.getBean(OnceboxProperties.class);
                    var transferTo = properties.getOutbox().getDlq().getTransferTo();

                    assertThat(transferTo.getPolling().getType()).isEqualTo(PollingType.FIXED);
                    assertThat(transferTo.getPolling().getFixedDelay()).isEqualTo(Duration.ofSeconds(0));

                    assertThat(transferTo.getPolling().getMinFixedDelay()).isZero();
                    assertThat(transferTo.getPolling().getMaxFixedDelay()).isZero();
                    assertThat(transferTo.getPolling().getMultiplier()).isNaN();
                });
    }
}