package io.github.dmitriyiliyov.oncebox.starter.schema.gap.app;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxSender;
import io.github.dmitriyiliyov.oncebox.core.outbox.SenderResult;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

// an application with spring.sql.init scripts next to oncebox; the sender keeps Kafka out of the picture
@SpringBootApplication
public class GapSqlInitTestApplication {

    @Bean
    OutboxSender userOutboxSender() {
        return (topic, events) -> SenderResult.empty();
    }
}
