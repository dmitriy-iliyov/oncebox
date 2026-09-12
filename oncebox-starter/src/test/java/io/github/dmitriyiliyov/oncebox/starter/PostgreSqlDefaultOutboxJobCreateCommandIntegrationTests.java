package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.tests.fixtures.PostgresTestContainerSingleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostgreSqlDefaultOutboxJobCreateCommandIntegrationTests {

    private final DefaultOutboxJobCreateCommandIntegrationVerifier verifier =
            new DefaultOutboxJobCreateCommandIntegrationVerifier(
                    PostgresTestContainerSingleton.INSTANCE.getJdbcUrl(),
                    "org.postgresql.Driver",
                    PostgresTestContainerSingleton.INSTANCE.getUsername(),
                    PostgresTestContainerSingleton.INSTANCE.getPassword()
            );

    @BeforeEach
    void setUp() {
        verifier.setUpSchema();
    }

    @Test
    @DisplayName("IT should insert new job in PostgreSQL")
    void shouldInsertNewJob() {
        verifier.shouldInsertNewJob();
    }

    @Test
    @DisplayName("IT should handle duplicate job gracefully in PostgreSQL")
    void shouldNotThrowExceptionWhenJobAlreadyExists() {
        verifier.shouldNotThrowExceptionWhenJobAlreadyExists();
    }
}