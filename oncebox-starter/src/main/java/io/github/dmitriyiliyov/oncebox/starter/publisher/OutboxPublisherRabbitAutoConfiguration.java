package io.github.dmitriyiliyov.oncebox.starter.publisher;

import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxSender;
import io.github.dmitriyiliyov.oncebox.rabbit.RabbitOutboxSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Configuration
@ConditionalOnProperty(
        prefix = "oncebox.publisher",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnClass({RabbitTemplate.class, RabbitOutboxSender.class})
@ConditionalOnProperty(
        prefix = "oncebox.publisher.sender",
        name = "type",
        havingValue = "rabbit"
)
public class OutboxPublisherRabbitAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherRabbitAutoConfiguration.class);

    private final OutboxPublisherProperties publisherProperties;

    public OutboxPublisherRabbitAutoConfiguration(OutboxPublisherProperties publisherProperties) {
        this.publisherProperties = publisherProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxSender rabbitOutboxSender(ApplicationContext context) {
        OutboxPublisherProperties.SenderProperties senderProperties = publisherProperties.getSender();
        String beanName = senderProperties.getBeanName();
        RabbitTemplate rabbitTemplate;
        if (beanName == null || beanName.isEmpty()) {
            log.warn("'sender.bean-name' is not specified, resolving the template by type");
            String [] beanNames = context.getBeanNamesForType(RabbitTemplate.class);
            if (beanNames.length == 0) {
                throw new IllegalStateException("Cannot create OutboxSender: no RabbitTemplate bean found");
            }
            if (beanNames.length > 1) {
                throw new IllegalStateException(
                        "Cannot create OutboxSender: found more then one RabbitTemplate bean: " +
                                Arrays.toString(beanNames) +
                                ". Please define a RabbitTemplate bean with this name, " +
                                "or configure 'sender.bean-name'"
                );
            }
            beanName = beanNames[0];
        }
        if (!context.containsBean(beanName)) {
            throw new IllegalArgumentException(
                    "Cannot create OutboxSender: RabbitTemplate bean '" + beanName + "' not found. " +
                            "Please define a RabbitTemplate bean with this name, " +
                            "or configure 'sender.bean-name'"
            );
        }
        senderProperties.setBeanName(beanName);
        rabbitTemplate = context.getBean(beanName, RabbitTemplate.class);
        if (!rabbitTemplate.isMandatoryFor(new Message(Boolean.FALSE.toString().getBytes(StandardCharsets.UTF_8)))) {
            log.error("RabbitTemplate '{}' mandatory flag is false. " +
                    "ReturnedMessage will not be received. You should set 'mandatory=true' for at-least-once", beanName);
        }
        return new RabbitOutboxSender(rabbitTemplate, senderProperties.getEmergencyTimeout().toSeconds());
    }
}
