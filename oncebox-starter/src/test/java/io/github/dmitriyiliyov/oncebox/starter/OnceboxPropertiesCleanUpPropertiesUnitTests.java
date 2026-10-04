package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.starter.OnceboxProperties.CleanUpProperties;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxProperties.PollingProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class OnceboxPropertiesCleanUpPropertiesUnitTests {

    @Test
    @DisplayName("UT applyDefaults() with enabled = true and no parameters should use defaults")
    public void applyDefaults_enabledTrue_noParams_shouldUseDefaults() {
        // given
        CleanUpProperties cleanup = new CleanUpProperties();
        cleanup.setEnabled(true);

        // when
        cleanup.applyDefaults();

        // then
        assertTrue(cleanup.isEnabled());
        assertEquals(500, cleanup.getBatchSize());
        assertEquals(Duration.ofHours(24), cleanup.getTtl());

        assertEquals(PollingType.ADAPTIVE, cleanup.getPolling().getType());
        assertEquals(Duration.ofMinutes(5), cleanup.getInitialDelay());
        assertEquals(Duration.ZERO, cleanup.getFixedDelay());
        assertEquals(Duration.ofSeconds(5), cleanup.getMinFixedDelay());
        assertEquals(Duration.ofMinutes(1), cleanup.getMaxFixedDelay());
        assertEquals(2.0, cleanup.getMultiplier());
    }

    @Test
    @DisplayName("UT applyDefaults() with enabled = true and valid parameters should assign values FIXED case")
    public void applyDefaults_enabledTrue_withFixedValues_shouldAssignValues() {
        // given
        PollingProperties customPolling = new PollingProperties();
        customPolling.setType(PollingType.ADAPTIVE);
        customPolling.setInitialDelay(Duration.ofSeconds(100));
        customPolling.setFixedDelay(Duration.ofSeconds(10));
        customPolling.setMaxFixedDelay(Duration.ofMinutes(10));
        customPolling.setMultiplier(1.1);

        CleanUpProperties cleanup = new CleanUpProperties();
        cleanup.setEnabled(true);
        cleanup.setBatchSize(50);
        cleanup.setTtl(Duration.ofMinutes(30));
        cleanup.setPolling(customPolling);

        // when
        cleanup.applyDefaults();

        // then
        assertTrue(cleanup.isEnabled());
        assertEquals(50, cleanup.getBatchSize());
        assertEquals(Duration.ofMinutes(30), cleanup.getTtl());

        assertEquals(PollingType.ADAPTIVE, cleanup.getPolling().getType());
        assertEquals(Duration.ofSeconds(100), cleanup.getInitialDelay());
        assertEquals(Duration.ZERO, cleanup.getFixedDelay());
        assertEquals(Duration.ofSeconds(5), cleanup.getMinFixedDelay());
        assertEquals(Duration.ofMinutes(10), cleanup.getMaxFixedDelay());
        assertEquals(1.1, cleanup.getMultiplier());
    }

    @Test
    @DisplayName("UT applyDefaults() with enabled = true and valid parameters should assign values ADAPTIVE case")
    public void applyDefaults_enabledTrue_withAdaptiveValues_shouldAssignValues() {
        // given
        PollingProperties customPolling = new PollingProperties();
        customPolling.setType(PollingType.FIXED);
        customPolling.setInitialDelay(Duration.ofSeconds(100));
        customPolling.setFixedDelay(Duration.ofSeconds(10));

        CleanUpProperties cleanup = new CleanUpProperties();
        cleanup.setEnabled(true);
        cleanup.setBatchSize(50);
        cleanup.setTtl(Duration.ofMinutes(30));
        cleanup.setPolling(customPolling);

        // when
        cleanup.applyDefaults();

        // then
        assertTrue(cleanup.isEnabled());
        assertEquals(50, cleanup.getBatchSize());
        assertEquals(Duration.ofMinutes(30), cleanup.getTtl());

        assertEquals(PollingType.FIXED, cleanup.getPolling().getType());
        assertEquals(Duration.ofSeconds(100), cleanup.getInitialDelay());
        assertEquals(Duration.ofSeconds(10), cleanup.getFixedDelay());
        assertEquals(Duration.ZERO, cleanup.getMinFixedDelay());
        assertEquals(Duration.ZERO, cleanup.getMaxFixedDelay());
        assertEquals(Double.NaN, cleanup.getMultiplier());
    }

    @Test
    @DisplayName("UT applyDefaults() with enabled = true and partial null/invalid fields should fallback to defaults")
    public void applyDefaults_enabledTrue_partialNull_shouldUseDefaultsForNull() {
        // given
        PollingProperties partialPolling = new PollingProperties();
        partialPolling.setType(PollingType.ADAPTIVE);
        partialPolling.setInitialDelay(Duration.ofSeconds(200));

        CleanUpProperties cleanup = new CleanUpProperties();
        cleanup.setEnabled(true);
        cleanup.setBatchSize(-1);
        cleanup.setTtl(null);
        cleanup.setPolling(partialPolling);

        // when
        cleanup.applyDefaults();

        // then
        assertTrue(cleanup.isEnabled());
        assertEquals(500, cleanup.getBatchSize());
        assertEquals(Duration.ofHours(24), cleanup.getTtl());

        assertEquals(PollingType.ADAPTIVE, cleanup.getPolling().getType());
        assertEquals(Duration.ofSeconds(200), cleanup.getInitialDelay());
        assertEquals(Duration.ofSeconds(5), cleanup.getMinFixedDelay());
        assertEquals(Duration.ofMinutes(1), cleanup.getMaxFixedDelay());
        assertEquals(2.0, cleanup.getMultiplier());
    }

    @Test
    @DisplayName("UT applyDefaults() with enabled = false should disable all fields")
    public void applyDefaults_enabledFalse_shouldDisableAndIgnoreValues() {
        // given
        PollingProperties dummyPolling = new PollingProperties();
        dummyPolling.setInitialDelay(Duration.ofSeconds(100));

        CleanUpProperties cleanup = new CleanUpProperties();
        cleanup.setEnabled(false);
        cleanup.setBatchSize(50);
        cleanup.setTtl(Duration.ofMinutes(30));
        cleanup.setPolling(dummyPolling);

        // when
        cleanup.applyDefaults();

        // then
        assertFalse(cleanup.isEnabled());
        assertEquals(0, cleanup.getBatchSize());
        assertNull(cleanup.getTtl());

        assertNotNull(cleanup.getPolling());
        assertNull(cleanup.getPolling().getType());
        assertNull(cleanup.getInitialDelay());
        assertNull(cleanup.getFixedDelay());
        assertNull(cleanup.getMinFixedDelay());
        assertNull(cleanup.getMaxFixedDelay());
        assertNull(cleanup.getMultiplier());
    }

    @Test
    @DisplayName("UT applyDefaults() with enabled = null should enable and initialize fields with defaults")
    public void applyDefaults_enabledNull_shouldEnable() {
        // given
        CleanUpProperties cleanup = new CleanUpProperties();
        cleanup.setEnabled(null);

        // when
        cleanup.applyDefaults();

        // then
        assertTrue(cleanup.isEnabled());
        assertEquals(500, cleanup.getBatchSize());
        assertEquals(Duration.ofHours(24), cleanup.getTtl());
        assertEquals(PollingType.ADAPTIVE, cleanup.getPolling().getType());
        assertEquals(Duration.ofMinutes(5), cleanup.getInitialDelay());
        assertEquals(Duration.ofSeconds(5), cleanup.getMinFixedDelay());
        assertEquals(Duration.ofMinutes(1), cleanup.getMaxFixedDelay());
        assertEquals(2.0, cleanup.getMultiplier());
    }
}
