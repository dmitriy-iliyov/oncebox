package io.github.dmitriyiliyov.oncebox.tests.integration.consume.kafka;

import io.github.dmitriyiliyov.oncebox.core.consumer.OutboxIdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.ConsumerVariant;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.JdbcConsumerBusinessRepository;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.JpaConsumerBusinessRepositoryProxy;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestConfiguration
public class KafkaIntegrationTestsConfig {

    private static final String BOOTSTRAP = KafkaTestContainerSingleton.INSTANCE.getBootstrapServers();

    @Bean
    public KafkaAdmin.NewTopics testOutboxTopics() {
        List<String> baseNames = List.of(
                KafkaConsumerBusinessService.SINGLE_TOPIC,
                KafkaConsumerBusinessService.BATCH_TOPIC,
                KafkaConsumerBusinessService.SINGLE_ID_TOPIC,
                KafkaConsumerBusinessService.BATCH_ID_TOPIC,
                KafkaConsumerFaultyBusinessService.SINGLE_FAILING_TOPIC,
                KafkaConsumerFaultyBusinessService.BATCH_FAILING_TOPIC,
                KafkaConsumerFaultyBusinessService.SINGLE_ID_FAILING_TOPIC,
                KafkaConsumerFaultyBusinessService.BATCH_ID_FAILING_TOPIC
        );
        return new KafkaAdmin.NewTopics(Arrays.stream(ConsumerVariant.values())
                .flatMap(variant -> baseNames.stream().map(variant::of))
                .map(name -> TopicBuilder.name(name).partitions(1).replicas(1).build())
                .toArray(NewTopic[]::new));
    }

    @Bean
    public KafkaTemplate<String, String> testKafkaTemplate() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
        );
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Bean
    public ConsumerFactory<Object, Object> testConsumerFactory() {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.GROUP_ID_CONFIG, KafkaConsumerBusinessService.CONSUMER_GROUP,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"
        );
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> testSingleKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> testConsumerFactory,
            @Qualifier("outboxKafkaRecordMessageConverter") RecordMessageConverter outboxKafkaRecordMessageConverter
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();

        configurer.configure(factory, testConsumerFactory);

        factory.setRecordMessageConverter(outboxKafkaRecordMessageConverter);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setBatchListener(false);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> testBatchKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> testConsumerFactory,
            RecordMessageConverter outboxKafkaRecordMessageConverter
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();

        configurer.configure(factory, testConsumerFactory);

        factory.setBatchMessageConverter(new BatchMessagingMessageConverter(outboxKafkaRecordMessageConverter));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setBatchListener(true);
        return factory;
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP);
        return new KafkaAdmin(configs);
    }

    @Bean
    public AdminClient adminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    @Bean
    public KafkaTestUtils kafkaTestUtils(AdminClient adminClient) {
        return new KafkaTestUtils(adminClient);
    }

    @Bean
    public KafkaConsumerBusinessService kafkaJdbcConsumerBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            JdbcConsumerBusinessRepository repository
    ) {
        return new KafkaConsumerBusinessService(outboxIdempotentConsumer, repository, ConsumerVariant.JDBC);
    }

    @Bean
    public KafkaConsumerBusinessService kafkaJpaConsumerBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            JpaConsumerBusinessRepositoryProxy repository
    ) {
        return new KafkaConsumerBusinessService(outboxIdempotentConsumer, repository, ConsumerVariant.JPA);
    }

    @Bean
    public KafkaConsumerFaultyBusinessService kafkaJdbcConsumerFaultyBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            JdbcConsumerBusinessRepository repository
    ) {
        return new KafkaConsumerFaultyBusinessService(outboxIdempotentConsumer, repository, ConsumerVariant.JDBC);
    }

    @Bean
    public KafkaConsumerFaultyBusinessService kafkaJpaConsumerFaultyBusinessService(
            OutboxIdempotentConsumer outboxIdempotentConsumer,
            JpaConsumerBusinessRepositoryProxy repository
    ) {
        return new KafkaConsumerFaultyBusinessService(outboxIdempotentConsumer, repository, ConsumerVariant.JPA);
    }
}