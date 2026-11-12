package io.github.dmitriyiliyov.oncebox.tests.integration.schema;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxSender;
import io.github.dmitriyiliyov.oncebox.core.outbox.SenderResult;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

// a user application next to oncebox; the sender keeps the broker out, the schema is all these tests look at
@SpringBootApplication
public class SchemaIntegrationTestApplication {

    @Bean
    OutboxSender schemaOutboxSender() {
        return (topic, events) -> SenderResult.empty();
    }
}
