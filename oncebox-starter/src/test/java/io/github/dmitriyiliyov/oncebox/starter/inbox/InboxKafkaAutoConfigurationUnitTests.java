package io.github.dmitriyiliyov.oncebox.starter.inbox;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.converter.RecordMessageConverter;

import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class InboxKafkaAutoConfigurationUnitTests {

    private static final String AUTO_COMMIT_WARNING = "Inbox Kafka consumer has 'enable.auto.commit=true'";

    private InboxKafkaAutoConfiguration tested;

    @Mock
    private InboxProperties inboxProperties;

    @Mock
    private ConcurrentKafkaListenerContainerFactoryConfigurer configurer;

    @Mock
    private ConsumerFactory<Object, Object> consumerFactory;

    @Mock
    private RecordMessageConverter recordMessageConverter;

    @BeforeEach
    void setUp() {
        tested = new InboxKafkaAutoConfiguration(inboxProperties);
    }

    @Test
    @DisplayName("UT inboxKafkaListenerContainerFactory() when record listener should keep configured ack mode")
    void inboxKafkaListenerContainerFactory_whenRecordListener_shouldKeepConfiguredAckMode() {
        // given
        givenConfigured(factory -> {
            factory.setBatchListener(false);
            factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        });

        // when
        ConcurrentKafkaListenerContainerFactory<Object, Object> result = createFactory();

        // then
        assertThat(result.isBatchListener()).isFalse();
        assertThat(result.getContainerProperties().getAckMode()).isEqualTo(ContainerProperties.AckMode.RECORD);
        verify(configurer).configure(any(), eq(consumerFactory));
    }

    @Test
    @DisplayName("UT inboxKafkaListenerContainerFactory() when batch listener should keep configured ack mode")
    void inboxKafkaListenerContainerFactory_whenBatchListener_shouldKeepConfiguredAckMode() {
        // given
        givenConfigured(factory -> {
            factory.setBatchListener(true);
            factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        });

        // when
        ConcurrentKafkaListenerContainerFactory<Object, Object> result = createFactory();

        // then
        assertThat(result.isBatchListener()).isTrue();
        assertThat(result.getContainerProperties().getAckMode()).isEqualTo(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        verify(configurer).configure(any(), eq(consumerFactory));
    }

    @Test
    @DisplayName("UT inboxKafkaListenerContainerFactory() when consumer factory enables auto commit should warn")
    void inboxKafkaListenerContainerFactory_whenConsumerFactoryEnablesAutoCommit_shouldWarn(CapturedOutput output) {
        // given
        givenConfigured(factory -> { });
        when(consumerFactory.getConfigurationProperties())
                .thenReturn(Map.of(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true));

        // when
        createFactory();

        // then
        assertThat(output).contains(AUTO_COMMIT_WARNING);
    }

    @Test
    @DisplayName("UT inboxKafkaListenerContainerFactory() when auto commit is set as string 'true' should warn")
    void inboxKafkaListenerContainerFactory_whenAutoCommitIsStringTrue_shouldWarn(CapturedOutput output) {
        // given
        givenConfigured(factory -> { });
        when(consumerFactory.getConfigurationProperties())
                .thenReturn(Map.of(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true"));

        // when
        createFactory();

        // then
        assertThat(output).contains(AUTO_COMMIT_WARNING);
    }

    @Test
    @DisplayName("UT inboxKafkaListenerContainerFactory() when container overrides auto commit to true should warn")
    void inboxKafkaListenerContainerFactory_whenContainerOverridesAutoCommitToTrue_shouldWarn(CapturedOutput output) {
        // given
        givenConfigured(factory -> factory.getContainerProperties()
                .getKafkaConsumerProperties()
                .setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true"));

        // when
        createFactory();

        // then
        assertThat(output).contains(AUTO_COMMIT_WARNING);
        verify(consumerFactory, never()).getConfigurationProperties();
    }

    @Test
    @DisplayName("UT inboxKafkaListenerContainerFactory() when container overrides auto commit to false should not warn")
    void inboxKafkaListenerContainerFactory_whenContainerOverridesAutoCommitToFalse_shouldNotWarn(CapturedOutput output) {
        // given
        givenConfigured(factory -> factory.getContainerProperties()
                .getKafkaConsumerProperties()
                .setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"));

        // when
        createFactory();

        // then
        assertThat(output).doesNotContain(AUTO_COMMIT_WARNING);
        verify(consumerFactory, never()).getConfigurationProperties();
    }

    @Test
    @DisplayName("UT inboxKafkaListenerContainerFactory() when auto commit is false should not warn")
    void inboxKafkaListenerContainerFactory_whenAutoCommitIsFalse_shouldNotWarn(CapturedOutput output) {
        // given
        givenConfigured(factory -> { });
        when(consumerFactory.getConfigurationProperties())
                .thenReturn(Map.of(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false));

        // when
        createFactory();

        // then
        assertThat(output).doesNotContain(AUTO_COMMIT_WARNING);
    }

    @Test
    @DisplayName("UT inboxKafkaListenerContainerFactory() when auto commit is not set should not warn, Spring Kafka disables it")
    void inboxKafkaListenerContainerFactory_whenAutoCommitIsNotSet_shouldNotWarn(CapturedOutput output) {
        // given
        givenConfigured(factory -> { });
        when(consumerFactory.getConfigurationProperties()).thenReturn(Map.of());

        // when
        createFactory();

        // then
        assertThat(output).doesNotContain(AUTO_COMMIT_WARNING);
    }

    @Test
    @DisplayName("UT inboxKafkaListenerContainerFactory() when consumer factory does not expose its config should not warn")
    void inboxKafkaListenerContainerFactory_whenConsumerFactoryDoesNotExposeConfig_shouldNotWarn(CapturedOutput output) {
        // given
        givenConfigured(factory -> { });
        when(consumerFactory.getConfigurationProperties()).thenThrow(new UnsupportedOperationException());

        // when
        ConcurrentKafkaListenerContainerFactory<Object, Object> result = createFactory();

        // then
        assertThat(result).isNotNull();
        assertThat(output).doesNotContain(AUTO_COMMIT_WARNING);
    }

    private void givenConfigured(Consumer<ConcurrentKafkaListenerContainerFactory<Object, Object>> configuration) {
        doAnswer(invocation -> {
            configuration.accept(invocation.getArgument(0));
            return null;
        }).when(configurer).configure(any(ConcurrentKafkaListenerContainerFactory.class), eq(consumerFactory));
    }

    private ConcurrentKafkaListenerContainerFactory<Object, Object> createFactory() {
        return tested.inboxKafkaListenerContainerFactory(configurer, consumerFactory, recordMessageConverter);
    }
}
