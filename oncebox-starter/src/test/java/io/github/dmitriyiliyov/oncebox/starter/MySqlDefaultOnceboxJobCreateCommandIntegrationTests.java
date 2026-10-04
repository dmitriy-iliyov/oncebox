package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.tests.fixtures.MySqlTestContainerSingleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MySqlDefaultOnceboxJobCreateCommandIntegrationTests {

    private final DefaultOnceboxJobCreateCommandIntegrationVerifier verifier =
            new DefaultOnceboxJobCreateCommandIntegrationVerifier(
                    MySqlTestContainerSingleton.INSTANCE.getJdbcUrl(),
                    "com.mysql.cj.jdbc.Driver",
                    MySqlTestContainerSingleton.INSTANCE.getUsername(),
                    MySqlTestContainerSingleton.INSTANCE.getPassword()
            );

    @BeforeEach
    void setUp() {
        verifier.setUpSchema();
    }

    @Test
    @DisplayName("IT should insert new job in MySQL")
    void shouldInsertNewJob() {
        verifier.shouldInsertNewJob();
    }

    @Test
    @DisplayName("IT should handle duplicate job gracefully in MySQL")
    void shouldNotThrowExceptionWhenJobAlreadyExists() {
        verifier.shouldNotThrowExceptionWhenJobAlreadyExists();
    }
}