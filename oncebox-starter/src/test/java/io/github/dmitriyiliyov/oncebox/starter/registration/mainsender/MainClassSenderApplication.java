package io.github.dmitriyiliyov.oncebox.starter.registration.mainsender;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxSender;
import io.github.dmitriyiliyov.oncebox.core.outbox.SenderResult;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MainClassSenderApplication {

    @Bean
    OutboxSender userOutboxSender() {
        return (topic, events) -> SenderResult.empty();
    }
}
