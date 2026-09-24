package io.github.dmitriyiliyov.oncebox.tests.integration.consume.rabbit;

import io.github.dmitriyiliyov.oncebox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.ConsumerBusinessRepository;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.ConsumerVariant;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.JdbcConsumerBusinessRepository;
import io.github.dmitriyiliyov.oncebox.tests.integration.utils.IdPreparer;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MethodRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.util.ReflectionUtils;
import org.testcontainers.containers.RabbitMQContainer;

import java.util.Arrays;
import java.util.List;

@TestConfiguration
public class RabbitIntegrationTestsConfig {

    private static final RabbitMQContainer RABBIT = RabbitTestContainerSingleton.INSTANCE;

    @Bean
    public ConnectionFactory testRabbitConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(RABBIT.getHost());
        factory.setPort(RABBIT.getAmqpPort());
        factory.setUsername(RABBIT.getAdminUsername());
        factory.setPassword(RABBIT.getAdminPassword());
        return factory;
    }

    @Bean
    public RabbitAdmin testRabbitAdmin(ConnectionFactory testRabbitConnectionFactory) {
        return new RabbitAdmin(testRabbitConnectionFactory);
    }

    @Bean
    public RabbitTemplate testRabbitTemplate(ConnectionFactory testRabbitConnectionFactory) {
        return new RabbitTemplate(testRabbitConnectionFactory);
    }

    @Bean
    public Declarables testOutboxQueues() {
        List<String> baseNames = List.of(
                RabbitConsumerBusinessService.SINGLE_QUEUE,
                RabbitConsumerBusinessService.BATCH_QUEUE,
                RabbitConsumerBusinessService.SINGLE_ID_QUEUE,
                RabbitConsumerBusinessService.BATCH_ID_QUEUE,
                RabbitConsumerFaultyBusinessService.SINGLE_FAILING_QUEUE,
                RabbitConsumerFaultyBusinessService.BATCH_FAILING_QUEUE,
                RabbitConsumerFaultyBusinessService.SINGLE_ID_FAILING_QUEUE,
                RabbitConsumerFaultyBusinessService.BATCH_ID_FAILING_QUEUE
        );
        return new Declarables(Arrays.stream(ConsumerVariant.values())
                .flatMap(variant -> baseNames.stream().map(variant::of))
                .map(name -> (Declarable) QueueBuilder.durable(name).build())
                .toList());
    }

    @Bean
    public SimpleRabbitListenerContainerFactory testSingleRabbitListenerContainerFactory(
            ConnectionFactory testRabbitConnectionFactory,
            @Qualifier("outboxRabbitMessageConverter") MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(testRabbitConnectionFactory);
        factory.setBatchListener(false);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setMessageConverter(messageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory testBatchRabbitListenerContainerFactory(
            ConnectionFactory testRabbitConnectionFactory,
            @Qualifier("outboxRabbitMessageConverter") MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(testRabbitConnectionFactory);
        factory.setBatchListener(true);
        factory.setConsumerBatchEnabled(true);
        factory.setBatchSize(100);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setMessageConverter(messageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public RabbitConsumerBusinessService rabbitMqJdbcConsumerBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
            IdPreparer idPreparer
    ) {
        return new RabbitConsumerBusinessService(
                outboxIdempotentConsumer,
                new JdbcConsumerBusinessRepository(jdbcTemplate, idPreparer),
                ConsumerVariant.JDBC
        );
    }

    @Bean
    public RabbitConsumerBusinessService rabbitMqJpaConsumerBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            @Qualifier("jpaConsumerBusinessRepositoryProxy") ConsumerBusinessRepository repository
    ) {
        return new RabbitConsumerBusinessService(
                outboxIdempotentConsumer,
                repository,
                ConsumerVariant.JPA
        );
    }

    @Bean
    public RabbitConsumerFaultyBusinessService rabbitMqJdbcFaultyConsumerBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
            IdPreparer idPreparer
    ) {
        return new RabbitConsumerFaultyBusinessService(
                outboxIdempotentConsumer,
                new JdbcConsumerBusinessRepository(jdbcTemplate, idPreparer),
                ConsumerVariant.JDBC
        );
    }

    @Bean
    public RabbitConsumerFaultyBusinessService rabbitMqJpaFaultyConsumerBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            @Qualifier("jpaConsumerBusinessRepositoryProxy") ConsumerBusinessRepository repository
    ) {
        return new RabbitConsumerFaultyBusinessService(
                outboxIdempotentConsumer,
                repository,
                ConsumerVariant.JPA
        );
    }

    /**
     * Registers every listener of every consumer bean on that bean's own queues. Spring AMQP resolves
     * {@code @RabbitListener(queues)} once per class, so two beans of one class cannot listen on different queues
     * through the annotation; they would share one queue and split its messages.
     */
    @Bean
    public RabbitListenerConfigurer consumerVariantListeners(List<RabbitConsumerBusinessService> businessServices,
                                                             List<RabbitConsumerFaultyBusinessService> faultyServices,
                                                             BeanFactory beanFactory) {
        DefaultMessageHandlerMethodFactory handlerMethodFactory = new DefaultMessageHandlerMethodFactory();
        handlerMethodFactory.afterPropertiesSet();
        return registrar -> {
            for (RabbitConsumerBusinessService service : businessServices) {
                ConsumerVariant variant = service.variant();
                register(registrar, beanFactory, handlerMethodFactory, service, variant, RabbitConsumerBusinessService.class,
                        RabbitConsumerBusinessService.SINGLE_QUEUE, "listenSingleMessage", false);
                register(registrar, beanFactory, handlerMethodFactory, service, variant, RabbitConsumerBusinessService.class,
                        RabbitConsumerBusinessService.BATCH_QUEUE, "listenBatchMessages", true);
                register(registrar, beanFactory, handlerMethodFactory, service, variant, RabbitConsumerBusinessService.class,
                        RabbitConsumerBusinessService.SINGLE_ID_QUEUE, "listenSingleId", false);
                register(registrar, beanFactory, handlerMethodFactory, service, variant, RabbitConsumerBusinessService.class,
                        RabbitConsumerBusinessService.BATCH_ID_QUEUE, "listenBatchIds", true);
            }
            for (RabbitConsumerFaultyBusinessService service : faultyServices) {
                ConsumerVariant variant = service.variant();
                register(registrar, beanFactory, handlerMethodFactory, service, variant, RabbitConsumerFaultyBusinessService.class,
                        RabbitConsumerFaultyBusinessService.SINGLE_FAILING_QUEUE, "listenFailing", false);
                register(registrar, beanFactory, handlerMethodFactory, service, variant, RabbitConsumerFaultyBusinessService.class,
                        RabbitConsumerFaultyBusinessService.BATCH_FAILING_QUEUE, "listenBatchFailing", true);
                register(registrar, beanFactory, handlerMethodFactory, service, variant, RabbitConsumerFaultyBusinessService.class,
                        RabbitConsumerFaultyBusinessService.SINGLE_ID_FAILING_QUEUE, "listenSingleIdFailing", false);
                register(registrar, beanFactory, handlerMethodFactory, service, variant, RabbitConsumerFaultyBusinessService.class,
                        RabbitConsumerFaultyBusinessService.BATCH_ID_FAILING_QUEUE, "listenBatchIdsFailing", true);
            }
        };
    }

    private static void register(RabbitListenerEndpointRegistrar registrar,
                                 BeanFactory beanFactory,
                                 DefaultMessageHandlerMethodFactory handlerMethodFactory,
                                 Object service,
                                 ConsumerVariant variant,
                                 Class<?> serviceClass,
                                 String baseQueue,
                                 String methodName,
                                 boolean batch) {
        MethodRabbitListenerEndpoint endpoint = new MethodRabbitListenerEndpoint();
        endpoint.setId(variant.of(baseQueue));
        endpoint.setQueueNames(variant.of(baseQueue));
        endpoint.setBean(service);
        endpoint.setMethod(ReflectionUtils.findMethod(serviceClass, methodName, batch ? List.class : Message.class));
        endpoint.setMessageHandlerMethodFactory(handlerMethodFactory);
        endpoint.setBeanFactory(beanFactory);
        String containerFactory = batch ? "testBatchRabbitListenerContainerFactory" : "testSingleRabbitListenerContainerFactory";
        registrar.registerEndpoint(endpoint, beanFactory.getBean(containerFactory, SimpleRabbitListenerContainerFactory.class));
    }
}
