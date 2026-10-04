package io.github.dmitriyiliyov.oncebox.tests.e2e;

import io.github.dmitriyiliyov.oncebox.starter.EnableOncebox;
import io.github.dmitriyiliyov.oncebox.tests.e2e.config.E2eTestConfig;
import io.github.dmitriyiliyov.oncebox.tests.e2e.config.KafkaBrokerConfig;
import io.github.dmitriyiliyov.oncebox.tests.e2e.config.RabbitBrokerConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Single application acting as both publisher and consumer.
 * Events still travel through a real broker, so the whole lifecycle is exercised:
 * business transaction -> outbox polling -> broker -> idempotent consumer.
 * Only one broker config activates per run, selected by the active profile (kafka|rabbit).
 */
@SpringBootApplication
@EnableTransactionManagement
@EnableOncebox
@Import({E2eTestConfig.class, KafkaBrokerConfig.class, RabbitBrokerConfig.class})
public class E2eTestApplication { }
