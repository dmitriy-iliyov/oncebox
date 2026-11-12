package io.github.dmitriyiliyov.oncebox.starter.registration.componentsender;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxSender;
import io.github.dmitriyiliyov.oncebox.core.outbox.SenderResult;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.List;

@SpringBootApplication
public class ComponentSenderApplication {

    @Component("userOutboxSender")
    static class UserOutboxSender implements OutboxSender {

        @Override
        public SenderResult sendEvents(String topic, List<OutboxEvent> events) {
            return SenderResult.empty();
        }
    }
}
