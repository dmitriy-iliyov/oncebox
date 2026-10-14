package io.github.dmitriyiliyov.oncebox.starter.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.dlq.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
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

    /**
     * Takes the application's own {@link Clock} when it has exactly one, and the UTC system clock otherwise - the
     * starter adds no {@link Clock} bean of its own, since a configuration imported by {@link
     * io.github.dmitriyiliyov.oncebox.starter.EnableOncebox} cannot reliably see whether the application declares one.
     */
    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqControllerAdvice outboxDlqControllerAdvice(ObjectProvider<Clock> clock) {
        return new OutboxDlqControllerAdvice(clock.getIfUnique(Clock::systemUTC));
    }
}
