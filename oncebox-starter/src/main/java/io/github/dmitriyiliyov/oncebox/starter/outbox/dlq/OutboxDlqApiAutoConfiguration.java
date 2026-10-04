package io.github.dmitriyiliyov.oncebox.starter.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.dlq.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Clock;

@Configuration
@ConditionalOnProperty(
        prefix = "oncebox.outbox.dlq",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnClass(OutboxDlqController.class)
@Import({
        PostgreSqlOutboxDlqApiRepositoryConfiguration.class,
        MySqlOutboxDlqApiRepositoryConfiguration.class,
        OracleOutboxDlqApiRepositoryConfiguration.class
})
public class OutboxDlqApiAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OutboxDlqApiAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(name = "outboxDlqApiService")
    public OutboxDlqApiService outboxDlqApiService(OutboxDlqApiRepository repository) {
        return new DefaultOutboxDlqApiService(repository);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqController outboxDlqController(OutboxDlqApiService service) {
        log.warn("Outbox DLQ API is exposed at '/api/outbox-dlq/events' path should be secured");
        return new OutboxDlqController(service);
    }

    @Bean
    @ConditionalOnMissingBean
    public DlqStatusQueryConverter outboxDlqStatusQueryConverter() {
        return new DlqStatusQueryConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqControllerAdvice outboxDlqControllerAdvice(Clock clock) {
        return new OutboxDlqControllerAdvice(clock);
    }
}
