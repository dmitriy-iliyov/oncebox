package io.github.dmitriyiliyov.oncebox.tests.e2e.config;

import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.starter.inbox.NonTransactionalConsumer;
import io.github.dmitriyiliyov.oncebox.starter.inbox.TransactionalConsumer;
import io.github.dmitriyiliyov.oncebox.tests.e2e.consume.RabbitConsumerBusinessService;
import io.github.dmitriyiliyov.oncebox.tests.e2e.consume.RecordingBusinessOperation;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.E2eEvents;
import io.github.dmitriyiliyov.oncebox.tests.e2e.publish.RabbitRawEventResender;
import io.github.dmitriyiliyov.oncebox.tests.e2e.publish.RawEventResender;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("rabbit")
public class RabbitBrokerConfig {

    @Bean
    public TopicExchange e2eEventsExchange() {
        return new TopicExchange(E2eEvents.TOPIC);
    }

    @Bean
    public Queue e2eEventsQueue() {
        return QueueBuilder.durable(E2eEvents.QUEUE).build();
    }

    @Bean
    public Binding e2eEventsBinding(Queue e2eEventsQueue, TopicExchange e2eEventsExchange) {
        // '#' matches every routing key, so all event types land in the single consumer queue
        return BindingBuilder.bind(e2eEventsQueue).to(e2eEventsExchange).with("#");
    }

    @Bean
    public TopicExchange e2eAtMostOnceEventsExchange() {
        return new TopicExchange(E2eEvents.AT_MOST_ONCE_TOPIC);
    }

    @Bean
    public Queue e2eAtMostOnceEventsQueue() {
        return QueueBuilder.durable(E2eEvents.AT_MOST_ONCE_QUEUE).build();
    }

    @Bean
    public Binding e2eAtMostOnceEventsBinding(Queue e2eAtMostOnceEventsQueue,
                                              TopicExchange e2eAtMostOnceEventsExchange) {
        return BindingBuilder.bind(e2eAtMostOnceEventsQueue).to(e2eAtMostOnceEventsExchange).with("#");
    }

    /**
     * Dedicated sender template resolved by the library via {@code oncebox.outbox.sender.bean-name}.
     * {@code mandatory=true} is required for at-least-once so unroutable messages are reported back.
     */
    @Bean
    public RabbitTemplate outboxRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitConsumerBusinessService rabbitConsumerBusinessService(
            @TransactionalConsumer IdempotentConsumer transactionalConsumer,
            @NonTransactionalConsumer IdempotentConsumer nonTransactionalConsumer,
            RecordingBusinessOperation operation
    ) {
        return new RabbitConsumerBusinessService(transactionalConsumer, nonTransactionalConsumer, operation);
    }

    @Bean
    public RawEventResender rawEventResender(RabbitTemplate outboxRabbitTemplate) {
        return new RabbitRawEventResender(outboxRabbitTemplate);
    }
}
