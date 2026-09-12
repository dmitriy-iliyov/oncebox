package io.github.dmitriyiliyov.oncebox.metrics.it_config;

import io.github.dmitriyiliyov.oncebox.tests.fixtures.PostgresTestContainerSingleton;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


@ActiveProfiles("postgres-it")
public abstract class BasePostgresSqlIntegrationTests extends BaseIntegrationTests {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTestContainerSingleton.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresTestContainerSingleton.INSTANCE::getUsername);
        registry.add("spring.datasource.password", PostgresTestContainerSingleton.INSTANCE::getPassword);
    }
}
