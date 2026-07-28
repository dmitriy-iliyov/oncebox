package io.github.dmitriyiliyov.oncebox.dlq.api.it.config;

import io.github.dmitriyiliyov.oncebox.dlq.api.DlqStatusQueryConverter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@TestConfiguration
public class WebTestsConfig {

    @Bean
    public DlqStatusQueryConverter dlqStatusQueryConverter() {
        return new DlqStatusQueryConverter();
    }

    // Was provided by ClockConfig, which moved to oncebox-testkit together with SqlTestApplication.
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
