package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.tests.fixtures.OracleTestContainerSingleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OracleDefaultOutboxJobCreateCommandIntegrationTests {

    private final DefaultOutboxJobCreateCommandIntegrationVerifier verifier =
            new DefaultOutboxJobCreateCommandIntegrationVerifier(
                    OracleTestContainerSingleton.INSTANCE.getJdbcUrl(),
                    "oracle.jdbc.OracleDriver",
                    OracleTestContainerSingleton.INSTANCE.getUsername(),
                    OracleTestContainerSingleton.INSTANCE.getPassword()
            );

    @BeforeEach
    void setUp() {
        verifier.setUpSchema();
    }

    @Test
    @DisplayName("IT should insert new job in Oracle")
    void shouldInsertNewJob() {
        verifier.shouldInsertNewJob();
    }

    @Test
    @DisplayName("IT should handle duplicate job gracefully in Oracle")
    void shouldNotThrowExceptionWhenJobAlreadyExists() {
        verifier.shouldNotThrowExceptionWhenJobAlreadyExists();
    }
}