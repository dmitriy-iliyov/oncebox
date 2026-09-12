package io.github.dmitriyiliyov.oncebox.oracle;

import io.github.dmitriyiliyov.oncebox.dlq.api.it.BaseIntegrationTests;
import io.github.dmitriyiliyov.oncebox.tests.fixtures.OracleTestContainerSingleton;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@ActiveProfiles("oracle-it")
@Import(DlqApiOracleIntegrationTestsConfig.class)
public abstract class BaseDlqApiOracleIntegrationTests extends BaseIntegrationTests {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", OracleTestContainerSingleton.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", OracleTestContainerSingleton.INSTANCE::getUsername);
        registry.add("spring.datasource.password", OracleTestContainerSingleton.INSTANCE::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "oracle.jdbc.OracleDriver");
    }
}