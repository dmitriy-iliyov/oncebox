package io.github.dmitriyiliyov.oncebox.example.consumer.config;

import io.github.dmitriyiliyov.oncebox.example.shared.OrderDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;

@Profile("kafka")
@Configuration
public class KafkaConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderDto> kafkaBatchListenerContainerFactory(
            ConsumerFactory<String, OrderDto> consumerFactory,
            @Qualifier("inboxKafkaRecordMessageConverter")RecordMessageConverter recordMessageConverter
    ) {
        ConcurrentKafkaListenerContainerFactory<String, OrderDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchMessageConverter(new BatchMessagingMessageConverter(recordMessageConverter));
        factory.setBatchListener(true);
        return factory;
    }
}
