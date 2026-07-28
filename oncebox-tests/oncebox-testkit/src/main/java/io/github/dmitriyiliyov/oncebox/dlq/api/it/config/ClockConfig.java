package io.github.dmitriyiliyov.oncebox.dlq.api.it.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@TestConfiguration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
