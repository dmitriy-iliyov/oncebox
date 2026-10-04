package io.github.dmitriyiliyov.oncebox.starter.inbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.core.OnceboxHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.kafka.support.mapping.DefaultJackson2JavaTypeMapper;
import org.springframework.kafka.support.mapping.Jackson2JavaTypeMapper;

@Configuration
@ConditionalOnProperty(
        prefix = "oncebox.inbox",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(
        prefix = "oncebox.inbox.source",
        name = "type",
        havingValue = "kafka"
)
public class InboxKafkaAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(InboxKafkaAutoConfiguration.class);

    private final InboxProperties inboxProperties;

    public InboxKafkaAutoConfiguration(InboxProperties inboxProperties) {
        this.inboxProperties = inboxProperties;
    }

    @Bean(name = "inboxKafkaRecordMessageConverter")
    @ConditionalOnMissingBean(name = "inboxKafkaRecordMessageConverter")
    public RecordMessageConverter inboxKafkaRecordMessageConverter(ObjectMapper objectMapper) {
        StringJsonMessageConverter converter = new StringJsonMessageConverter(objectMapper);

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.TYPE_ID);
        typeMapper.setClassIdFieldName(OnceboxHeaders.EVENT_TYPE.getValue());
        typeMapper.setIdClassMapping(inboxProperties.getMappings());

        converter.setTypeMapper(typeMapper);
        return converter;
    }

    @Bean(name = "inboxKafkaListenerContainerFactory")
    @ConditionalOnMissingBean(name = "inboxKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<Object, Object> inboxKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory,
            @Qualifier("inboxKafkaRecordMessageConverter") RecordMessageConverter recordMessageConverter
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);
        factory.setRecordMessageConverter(recordMessageConverter);
        if (Boolean.TRUE.equals(factory.isBatchListener())) {
        } else {
            factory.setRecordMessageConverter(recordMessageConverter);
            factory.setBatchMessageConverter(new BatchMessagingMessageConverter(recordMessageConverter));
        }
        if (!ContainerProperties.AckMode.MANUAL.equals(factory.getContainerProperties().getAckMode()) &&
                !ContainerProperties.AckMode.MANUAL_IMMEDIATE.equals(factory.getContainerProperties().getAckMode())) {
            log.warn("Outbox Consumer AckMode isn't MANUAL. It is highly recommended to set " +
                    "'spring.kafka.listener.ack-mode=manual' for 'at-least-once' delivery guarantees.");
        }
        return factory;
    }
}
