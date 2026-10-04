package io.github.dmitriyiliyov.oncebox.tests.e2e.config;

import io.github.dmitriyiliyov.oncebox.core.inbox.IdempotentConsumer;
import io.github.dmitriyiliyov.oncebox.tests.e2e.consume.KafkaConsumerBusinessService;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.E2eEvents;
import io.github.dmitriyiliyov.oncebox.tests.e2e.publish.KafkaRawEventResender;
import io.github.dmitriyiliyov.oncebox.tests.e2e.publish.RawEventResender;
import io.github.dmitriyiliyov.oncebox.tests.e2e.repository.TestOutboxRepository;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

@TestConfiguration
@Profile("kafka")
public class KafkaBrokerConfig {

    @Bean
    public NewTopic e2eEventsTopic() {
        return TopicBuilder.name(E2eEvents.TOPIC).partitions(1).replicas(1).build();
    }

    /**
     * Dedicated sender template with aggressive timeouts so broker outages fail fast
     * and retry/DLQ scenarios do not wait for the default 2-minute delivery timeout.
     */
    @Bean
    public KafkaTemplate<String, String> outboxKafkaTemplate() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerSingleton.INSTANCE.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000,
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 2000,
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 4000
        );
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Bean
    public KafkaConsumerBusinessService kafkaConsumerBusinessService(IdempotentConsumer idempotentConsumer,
                                                                     TestOutboxRepository repository) {
        return new KafkaConsumerBusinessService(idempotentConsumer, repository);
    }

    @Bean
    public RawEventResender rawEventResender(KafkaTemplate<String, String> outboxKafkaTemplate) {
        return new KafkaRawEventResender(outboxKafkaTemplate);
    }
}
