package io.github.dmitriyiliyov.oncebox.example.producer.configs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(prefix = "oncebox.publisher.sender", name = "type", havingValue = "rabbit")
@Configuration
@Slf4j
public class RabbitConfig {

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.port}")
    private Integer port;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

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
    public RabbitTemplate.ReturnsCallback returnCallback() {
        return returned -> log.error(
                "Message returned. exchange={}, routingKey={}, replyCode={}, replyText={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getReplyText()
        );
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         RabbitTemplate.ReturnsCallback returnCallback) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        template.setReturnsCallback(returnCallback);
        return template;
    }

    @Bean
    public Queue ordersQueue() {
        return new Queue("orders", true);
    }

    @Bean
    public Queue ordersCreatedQueue() {
        return new Queue("orders.created", true);
    }

    @Bean
    public DirectExchange ordersExchange() {
        return new DirectExchange("orders-exchange");
    }

    @Bean
    public Binding ordersUpdateBinding(Queue ordersQueue, DirectExchange ordersExchange) {
        return BindingBuilder
                .bind(ordersQueue)
                .to(ordersExchange)
                .with("update-order");
    }

    @Bean
    public Binding ordersDeleteBinding(Queue ordersQueue, DirectExchange ordersExchange) {
        return BindingBuilder
                .bind(ordersQueue)
                .to(ordersExchange)
                .with("delete-order");
    }

    @Bean
    public Binding ordersCreatedBinding(Queue ordersCreatedQueue, DirectExchange ordersExchange) {
        return BindingBuilder
                .bind(ordersCreatedQueue)
                .to(ordersExchange)
                .with("create-order");
    }
}
