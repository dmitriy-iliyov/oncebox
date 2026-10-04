package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.tests.fixtures.MySqlTestContainerSingleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MySqlOnceboxJobsInitializerIntegrationTests {

    private final OnceboxJobsInitializerIntegrationVerifier verifier =
            new OnceboxJobsInitializerIntegrationVerifier(
                    MySqlTestContainerSingleton.INSTANCE.getJdbcUrl(),
                    "com.mysql.cj.jdbc.Driver",
                    MySqlTestContainerSingleton.INSTANCE.getUsername(),
                    MySqlTestContainerSingleton.INSTANCE.getPassword()
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