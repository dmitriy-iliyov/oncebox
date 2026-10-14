package io.github.dmitriyiliyov.oncebox.tests.integration.publish;

import io.github.dmitriyiliyov.oncebox.starter.EnableOncebox;
import io.github.dmitriyiliyov.oncebox.tests.integration.publish.config.OutboxIntegrationTestsConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
        scanBasePackages = {
                "io.github.dmitriyiliyov.oncebox.tests.integration.publish",
                "io.github.dmitriyiliyov.oncebox.tests.integration.domain"
        }
)
@EntityScan(basePackages = "io.github.dmitriyiliyov.oncebox.tests.integration.domain")
@EnableJpaRepositories(basePackages = "io.github.dmitriyiliyov.oncebox.tests.integration.publish")
@EnableTransactionManagement
@EnableOncebox
@Import(OutboxIntegrationTestsConfig.class)
public class OutboxIntegrationTestApplication { }
