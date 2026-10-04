package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.tests.fixtures.OracleTestContainerSingleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OracleOnceboxJobsInitializerIntegrationTests {

    private final OnceboxJobsInitializerIntegrationVerifier verifier =
            new OnceboxJobsInitializerIntegrationVerifier(
                    OracleTestContainerSingleton.INSTANCE.getJdbcUrl(),
                    "oracle.jdbc.OracleDriver",
                    OracleTestContainerSingleton.INSTANCE.getUsername(),
                    OracleTestContainerSingleton.INSTANCE.getPassword()
            );

    @Test
    @DisplayName("IT should execute real command and insert job to Oracle")
    void shouldExecuteCommandsAndSaveToDatabase() {
        verifier.shouldExecuteCommandsAndSaveToDatabase();
    }

    @Test
    @DisplayName("IT should execute real command and insert job to Oracle when DLQ enabled")
    void shouldExecuteCommandsAndSaveToDatabase_whenDlqEnabled() {
        verifier.shouldExecuteCommandsAndSaveToDatabase_whenDlqEnabled();
    }

    @Test
    @DisplayName("IT should execute real command and insert job to Oracle when inbox clean up enabled")
    void shouldExecuteCommandsAndSaveToDatabase_whenInboxEnabled() {
        verifier.shouldExecuteCommandsAndSaveToDatabase_whenInboxEnabled();
    }
}