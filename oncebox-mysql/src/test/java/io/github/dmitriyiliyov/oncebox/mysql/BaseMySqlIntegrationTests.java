package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.it.BaseIntegrationTests;
import io.github.dmitriyiliyov.oncebox.tests.fixtures.MySqlTestContainerSingleton;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@ActiveProfiles("mysql-it")
@Import(MySqlIntegrationTestsConfig.class)
public abstract class BaseMySqlIntegrationTests extends BaseIntegrationTests {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MySqlTestContainerSingleton.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", MySqlTestContainerSingleton.INSTANCE::getUsername);
        registry.add("spring.datasource.password", MySqlTestContainerSingleton.INSTANCE::getPassword);
    }
}
