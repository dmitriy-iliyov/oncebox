package io.github.dmitriyiliyov.oncebox.starter.registration.scannedsender;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxSender;
import io.github.dmitriyiliyov.oncebox.core.outbox.SenderResult;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
public class ScannedSenderApplication {

    @Configuration
    static class SenderConfiguration {

        @Bean
        OutboxSender userOutboxSender() {
            return (topic, events) -> SenderResult.empty();
        }
    }
}
