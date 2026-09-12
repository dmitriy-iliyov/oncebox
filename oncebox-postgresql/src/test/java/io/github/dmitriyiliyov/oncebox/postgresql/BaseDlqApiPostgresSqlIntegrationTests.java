package io.github.dmitriyiliyov.oncebox.postgresql;

import io.github.dmitriyiliyov.oncebox.dlq.api.it.BaseIntegrationTests;
import io.github.dmitriyiliyov.oncebox.tests.fixtures.PostgresTestContainerSingleton;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


@ActiveProfiles("postgres-it")
@Import(DlqApiPostgresSqlIntegrationTestsConfig.class)
public abstract class BaseDlqApiPostgresSqlIntegrationTests extends BaseIntegrationTests {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTestContainerSingleton.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresTestContainerSingleton.INSTANCE::getUsername);
        registry.add("spring.datasource.password", PostgresTestContainerSingleton.INSTANCE::getPassword);
    }
}
