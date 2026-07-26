package io.github.dmitriyiliyov.oncebox.example.producer.configs;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@ConditionalOnProperty(prefix = "oncebox.publisher.sender", name = "type", havingValue = "kafka")
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 1000);
        ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(properties);
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public NewTopic ordersEventTopic() {
        return TopicBuilder.name("orders")
                .partitions(3)
                .build();
    }

    @Bean
    public NewTopic ordersCreateEventTopic() {
        return TopicBuilder.name("orders.created")
                .partitions(3)
                .build();
    }
}
