package io.github.dmitriyiliyov.oncebox.example.consumer.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("rabbit")
@Configuration
public class RabbitConfig {

    @Value("${spring.rabbitmq.host}")
    public String host;

    @Value("${spring.rabbitmq.port}")
    public Integer port;

    @Value("${spring.rabbitmq.username}")
    public String username;

    @Value("${spring.rabbitmq.password}")
    public String password;

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitBatchListenerContainerFactory(
            ConnectionFactory connectionFactory,
            @Qualifier("inboxRabbitMessageConverter") MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);

        factory.setBatchListener(true);
        factory.setConsumerBatchEnabled(true);
        factory.setBatchSize(200);

        return factory;
    }
}
