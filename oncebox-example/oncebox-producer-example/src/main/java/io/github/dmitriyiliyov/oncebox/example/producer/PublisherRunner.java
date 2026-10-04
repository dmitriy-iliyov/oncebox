package io.github.dmitriyiliyov.oncebox.example.producer;

import io.github.dmitriyiliyov.oncebox.starter.EnableOncebox;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
@EnableKafka
@EnableOncebox
public class PublisherRunner {

    public static void main(String ... args) {
        SpringApplication.run(PublisherRunner.class, args);
    }
}
