package io.github.dmitriyiliyov.oncebox.starter.publisher;

import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxSender;
import io.github.dmitriyiliyov.oncebox.kafka.KafkaOutboxSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Arrays;
import java.util.Map;

@Configuration
@ConditionalOnProperty(
        prefix = "oncebox.publisher",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnClass({KafkaTemplate.class, KafkaOutboxSender.class})
@ConditionalOnProperty(
        prefix = "oncebox.publisher.sender",
        name = "type",
        havingValue = "kafka"
)
public class OutboxPublisherKafkaAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherKafkaAutoConfiguration.class);

    private final OutboxPublisherProperties publisherProperties;

    public OutboxPublisherKafkaAutoConfiguration(OutboxPublisherProperties publisherProperties) {
        this.publisherProperties = publisherProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxSender kafkaOutboxSender(ApplicationContext context) {
        OutboxPublisherProperties.SenderProperties senderProperties = publisherProperties.getSender();
        String beanName = senderProperties.getBeanName();
        KafkaTemplate<String, String> kafkaTemplate;
        if (beanName == null || beanName.isEmpty()) {
            log.warn("'sender.bean-name' is not specified, resolving the template by type");
            String [] beanNames = context.getBeanNamesForType(KafkaTemplate.class);
            if (beanNames.length == 0) {
                throw new IllegalStateException("Cannot create OutboxSender: no KafkaTemplate bean found");
            }
            if (beanNames.length > 1) {
                throw new IllegalStateException(
                        "Cannot create OutboxSender: found more then one KafkaTemplate bean: " +
                                Arrays.toString(beanNames) +
                                ". Please define a KafkaTemplate<String, String> bean with this name, " +
                                "or configure 'sender.bean-name'"
                );
            }
            beanName = beanNames[0];
        }
        if (!context.containsBean(beanName)) {
            throw new IllegalArgumentException(
                    "Cannot create OutboxSender: KafkaTemplate bean '" + beanName + "' not found. " +
                            "Please define a KafkaTemplate<String, String> bean with this name, " +
                            "or configure 'sender.bean-name'"
            );
        }
        senderProperties.setBeanName(beanName);
        kafkaTemplate = context.getBean(beanName, KafkaTemplate.class);
        Map<String, Object> configs = kafkaTemplate.getProducerFactory().getConfigurationProperties();
        String acks = (String) configs.get("acks");
        if (acks == null || !acks.equals("all")) {
            log.warn("Kafka producer factory is configured without 'acks=all'. Outbox cannot guarantee at-least-once delivery");
        }
        Boolean idempotence = null;
        Object idempotenceObj = configs.get("enable.idempotence");
        if (idempotenceObj instanceof Boolean) {
            idempotence = (Boolean) idempotenceObj;
        } else if (idempotenceObj instanceof String) {
            idempotence = Boolean.parseBoolean((String) idempotenceObj);
        }
        if (idempotence == null || !idempotence) {
            log.warn("Kafka producer is not idempotent. It is recommended to enabled 'enable.idempotence=true' to avoid message duplication");
        }
        return new KafkaOutboxSender(kafkaTemplate, senderProperties.getEmergencyTimeout().toMillis());
    }
}
