package io.github.dmitriyiliyov.oncebox.example.consumer;

import io.github.dmitriyiliyov.oncebox.starter.EnableOncebox;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@EnableCaching
@EnableOncebox
public class ConsumerRunner {

    public static void main(String [] args) {
        SpringApplication.run(ConsumerRunner.class, args);
    }
}
