package io.github.dmitriyiliyov.oncebox.starter.inbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        prefix = "oncebox.inbox",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnProperty(
        prefix = "oncebox.inbox.source",
        name = "type",
        havingValue = "rabbit"
)
public class InboxRabbitAutoConfiguration {

    private final InboxProperties inboxProperties;

    public InboxRabbitAutoConfiguration(InboxProperties inboxProperties) {
        this.inboxProperties = inboxProperties;
    }

    @Bean(name = "inboxRabbitMessageConverter")
    @ConditionalOnMissingBean(name = "inboxRabbitMessageConverter")
    public MessageConverter inboxRabbitMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setClassMapper(new RabbitEventClassMapper(inboxProperties.getMappings()));
        return converter;
    }

    @Bean(name = "inboxRabbitListenerContainerFactory")
    @ConditionalOnMissingBean(name = "inboxRabbitListenerContainerFactory")
    public RabbitListenerContainerFactory<?> inboxRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            @Qualifier("inboxRabbitMessageConverter") MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}
