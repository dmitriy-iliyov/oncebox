package io.github.dmitriyiliyov.oncebox.starter.schema.app;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxSender;
import io.github.dmitriyiliyov.oncebox.core.outbox.SenderResult;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

// an application with its own Flyway migrations next to oncebox; the sender keeps Kafka out of the picture
@SpringBootApplication
public class SchemaMigrationTestApplication {

    @Bean
    OutboxSender userOutboxSender() {
        return (topic, events) -> SenderResult.empty();
    }
}
