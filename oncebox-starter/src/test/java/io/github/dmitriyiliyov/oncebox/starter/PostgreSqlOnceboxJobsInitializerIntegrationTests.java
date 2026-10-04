package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.tests.fixtures.PostgresTestContainerSingleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostgreSqlOnceboxJobsInitializerIntegrationTests {

    private final OnceboxJobsInitializerIntegrationVerifier verifier =
            new OnceboxJobsInitializerIntegrationVerifier(
                    PostgresTestContainerSingleton.INSTANCE.getJdbcUrl(),
                    "org.postgresql.Driver",
                    PostgresTestContainerSingleton.INSTANCE.getUsername(),
                    PostgresTestContainerSingleton.INSTANCE.getPassword()
            );

    @Test
    @DisplayName("IT should execute real command and insert job to PostgreSQL")
    void shouldExecuteCommandsAndSaveToDatabase() {
        verifier.shouldExecuteCommandsAndSaveToDatabase();
    }

    @Test
    @DisplayName("IT should execute real command and insert job to PostgreSQL when DLQ enabled")
    void shouldExecuteCommandsAndSaveToDatabase_whenDlqEnabled() {
        verifier.shouldExecuteCommandsAndSaveToDatabase_whenDlqEnabled();
    }

    @Test
    @DisplayName("IT should execute real command and insert job to PostgreSQL when inbox clean up enabled")
    void shouldExecuteCommandsAndSaveToDatabase_whenInboxEnabled() {
        verifier.shouldExecuteCommandsAndSaveToDatabase_whenInboxEnabled();
    }
}