package io.github.dmitriyiliyov.oncebox.starter.outbox;

import io.github.dmitriyiliyov.oncebox.starter.OnceboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.PollingType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboxPropertiesStuckRecoveryPropertiesUnitTests {

    @Test
    @DisplayName("UT applyDefaults() with no parameters should assign default values")
    void applyDefaults_noParams_shouldAssignDefaults() {
        // given + when
        OutboxProperties.StuckRecoveryProperties recovery = new OutboxProperties.StuckRecoveryProperties();

        recovery.setBatchSize(null);
        recovery.setMaxBatchProcessingTime(null);
        recovery.setPolling(null);

        recovery.applyDefaults();

        // then
        assertEquals(500, recovery.getBatchSize());
        assertEquals(Duration.ofMinutes(5), recovery.getMaxBatchProcessingTime());

        Assertions.assertEquals(PollingType.ADAPTIVE, recovery.getPolling().getType());
        assertEquals(Duration.ofMinutes(5), recovery.getInitialDelay());
        assertEquals(Duration.ZERO, recovery.getFixedDelay());
        assertEquals(Duration.ofSeconds(1), recovery.getMinFixedDelay());
        assertEquals(Duration.ofMinutes(1), recovery.getMaxFixedDelay());
        assertEquals(4.0, recovery.getMultiplier());
    }

    @Test
    @DisplayName("UT applyDefaults() with negative batchSize should assign default batchSize and preserve pollingDefaults")
    void applyDefaults_negativeBatchSize_shouldUseDefault() {
        // given
        OutboxProperties.StuckRecoveryProperties recovery = new OutboxProperties.StuckRecoveryProperties();
        recovery.setBatchSize(-10);

        OnceboxProperties.PollingProperties polling = new OnceboxProperties.PollingProperties();
        polling.setType(PollingType.FIXED);
        polling.setInitialDelay(Duration.ofSeconds(100));
        polling.setFixedDelay(Duration.ofSeconds(200));
        recovery.setPolling(polling);

        // when
        recovery.applyDefaults();

        // then
        assertEquals(500, recovery.getBatchSize());
        assertEquals(Duration.ofMinutes(5), recovery.getMaxBatchProcessingTime());

        assertEquals(PollingType.FIXED, recovery.getPolling().getType());
        assertEquals(Duration.ofSeconds(100), recovery.getInitialDelay());
        assertEquals(Duration.ofSeconds(200), recovery.getFixedDelay());
        assertEquals(Duration.ZERO, recovery.getMinFixedDelay());
        assertEquals(Duration.ZERO, recovery.getMaxFixedDelay());
        assertEquals(Double.NaN, recovery.getMultiplier());
    }

    @Test
    @DisplayName("UT applyDefaults() with valid parameters should assign provided values")
    void applyDefaults_validParameters_shouldAssignValues() {
        // given
        int batchSize = 50;
        Duration maxProcessing = Duration.ofSeconds(300);
        Duration initialDelay = Duration.ofSeconds(60);
        Duration fixedDelay = Duration.ofSeconds(120);

        OutboxProperties.StuckRecoveryProperties recovery = new OutboxProperties.StuckRecoveryProperties();
        recovery.setBatchSize(batchSize);
        recovery.setMaxBatchProcessingTime(maxProcessing);

        OnceboxProperties.PollingProperties polling = new OnceboxProperties.PollingProperties();
        polling.setType(PollingType.FIXED);
        polling.setInitialDelay(initialDelay);
        polling.setFixedDelay(fixedDelay);
        recovery.setPolling(polling);

        // when
        recovery.applyDefaults();

        // then
        assertEquals(batchSize, recovery.getBatchSize());
        assertEquals(maxProcessing, recovery.getMaxBatchProcessingTime());
        assertEquals(PollingType.FIXED, recovery.getPolling().getType());
        assertEquals(initialDelay, recovery.getInitialDelay());
        assertEquals(fixedDelay, recovery.getFixedDelay());
    }
}