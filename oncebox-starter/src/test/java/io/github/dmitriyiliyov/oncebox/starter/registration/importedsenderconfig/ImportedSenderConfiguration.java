package io.github.dmitriyiliyov.oncebox.starter.registration.importedsenderconfig;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxSender;
import io.github.dmitriyiliyov.oncebox.core.outbox.SenderResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImportedSenderConfiguration {

    @Bean
    OutboxSender userOutboxSender() {
        return (topic, events) -> SenderResult.empty();
    }
}
