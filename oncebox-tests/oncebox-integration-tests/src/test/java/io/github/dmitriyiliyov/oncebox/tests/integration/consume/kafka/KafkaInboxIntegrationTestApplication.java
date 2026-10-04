package io.github.dmitriyiliyov.oncebox.tests.integration.consume.kafka;

import io.github.dmitriyiliyov.oncebox.starter.EnableOncebox;
import io.github.dmitriyiliyov.oncebox.tests.integration.ClockConfig;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.InboxIntegrationTestsConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
        scanBasePackages = {
                "io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared",
                "io.github.dmitriyiliyov.oncebox.tests.integration.consume.kafka",
                "io.github.dmitriyiliyov.oncebox.tests.integration.domain"
        }
)
@EntityScan(basePackages = "io.github.dmitriyiliyov.oncebox.tests.integration.domain")
@EnableJpaRepositories(basePackages = "io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared")
@EnableTransactionManagement
@EnableOncebox
@Import({
        InboxIntegrationTestsConfig.class,
        KafkaIntegrationTestsConfig.class,
        ClockConfig.class
})
public class KafkaInboxIntegrationTestApplication { }
