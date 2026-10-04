package io.github.dmitriyiliyov.oncebox.starter.inbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.converter.RecordMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InboxKafkaAutoConfigurationUnitTests {

    private InboxKafkaAutoConfiguration config;

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
        config = new InboxKafkaAutoConfiguration(inboxProperties);
    }

    @Test
    @DisplayName("UT inboxKafkaListenerContainerFactory creates factory for non-batch listener with MANUAL ack mode")
    void createsFactory_nonBatchListener_manualAckMode() {
        doAnswer(invocation -> {
            ConcurrentKafkaListenerContainerFactory<Object, Object> factory = invocation.getArgument(0);
            factory.setBatchListener(false);
            factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
            return null;
        }).when(configurer).configure(any(ConcurrentKafkaListenerContainerFactory.class), eq(consumerFactory));

        ConcurrentKafkaListenerContainerFactory<Object, Object> result = config.inboxKafkaListenerContainerFactory(
                configurer, consumerFactory, recordMessageConverter
        );

        assertThat(result.isBatchListener()).isFalse();
        assertThat(result.getContainerProperties().getAckMode()).isEqualTo(ContainerProperties.AckMode.MANUAL);
        verify(configurer).configure(any(), eq(consumerFactory));
    }

    @Test
    @DisplayName("UT inboxKafkaListenerContainerFactory creates factory for batch listener with MANUAL_IMMEDIATE ack mode")
    void createsFactory_batchListener_manualImmediateAckMode() {
        doAnswer(invocation -> {
            ConcurrentKafkaListenerContainerFactory<Object, Object> factory = invocation.getArgument(0);
            factory.setBatchListener(true);
            factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
            return null;
        }).when(configurer).configure(any(ConcurrentKafkaListenerContainerFactory.class), eq(consumerFactory));

        ConcurrentKafkaListenerContainerFactory<Object, Object> result = config.inboxKafkaListenerContainerFactory(
                configurer, consumerFactory, recordMessageConverter
        );

        assertThat(result.isBatchListener()).isTrue();
        assertThat(result.getContainerProperties().getAckMode()).isEqualTo(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        verify(configurer).configure(any(), eq(consumerFactory));
    }
    
    @Test
    @DisplayName("UT inboxKafkaListenerContainerFactory creates factory with other ack mode and logs warning")
    void createsFactory_otherAckMode_logsWarning() {
        doAnswer(invocation -> {
            ConcurrentKafkaListenerContainerFactory<Object, Object> factory = invocation.getArgument(0);
            factory.setBatchListener(false);
            factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
            return null;
        }).when(configurer).configure(any(ConcurrentKafkaListenerContainerFactory.class), eq(consumerFactory));

        ConcurrentKafkaListenerContainerFactory<Object, Object> result = config.inboxKafkaListenerContainerFactory(
                configurer, consumerFactory, recordMessageConverter
        );

        assertThat(result.isBatchListener()).isFalse();
        assertThat(result.getContainerProperties().getAckMode()).isEqualTo(ContainerProperties.AckMode.RECORD);
        verify(configurer).configure(any(), eq(consumerFactory));
    }
}
