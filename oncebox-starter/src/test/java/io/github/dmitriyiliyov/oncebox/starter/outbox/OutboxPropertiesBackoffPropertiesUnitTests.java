package io.github.dmitriyiliyov.oncebox.starter.outbox;

import io.github.dmitriyiliyov.oncebox.starter.outbox.OutboxProperties.BackoffProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class OutboxPropertiesBackoffPropertiesUnitTests {

    @Test
    @DisplayName("UT applyDefaults() with default constructor should retain ZERO and NaN")
    public void applyDefaults_defaultConstructor_retainsZeroAndNaN() {
        BackoffProperties backoff = new BackoffProperties();
        BackoffProperties.Defaults defaults = new BackoffProperties.Defaults(Duration.ofSeconds(10), 3.0);

        backoff.applyDefaults(defaults);

        assertTrue(backoff.isEnabled());
        assertEquals(Duration.ofSeconds(10), backoff.getDelay());
        assertEquals(3.0, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT applyDefaults() with enabled=true and null fields should assign defaults")
    public void applyDefaults_enabledTrueWithNull_assignsDefaults() {
        BackoffProperties backoff = new BackoffProperties();
        backoff.setEnabled(true);
        backoff.setDelay(null);
        backoff.setMultiplier(null);
        BackoffProperties.Defaults defaults = new BackoffProperties.Defaults(Duration.ofSeconds(10), 3.0);

        backoff.applyDefaults(defaults);

        assertTrue(backoff.isEnabled());
        assertEquals(Duration.ofSeconds(10), backoff.getDelay());
        assertEquals(3.0, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT applyDefaults() with enabled=false should disable backoff and set minimal values")
    public void applyDefaults_enabledFalse_disablesBackoff() {
        BackoffProperties backoff = new BackoffProperties();
        backoff.setEnabled(false);
        backoff.setDelay(Duration.ofSeconds(100));
        backoff.setMultiplier(5.6);
        BackoffProperties.Defaults defaults = new BackoffProperties.Defaults(Duration.ofSeconds(10), 3.0);

        backoff.applyDefaults(defaults);

        assertFalse(backoff.isEnabled());
        assertEquals(Duration.ofSeconds(0), backoff.getDelay());
        assertEquals(1.0, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT applyDefaults() with enabled=null should treat as enabled and assign defaults")
    public void applyDefaults_enabledNull_assignsDefaults() {
        BackoffProperties backoff = new BackoffProperties();
        backoff.setEnabled(null);
        backoff.setDelay(null);
        backoff.setMultiplier(null);
        BackoffProperties.Defaults defaults = new BackoffProperties.Defaults(Duration.ofSeconds(10), 3.0);

        backoff.applyDefaults(defaults);

        assertTrue(backoff.isEnabled());
        assertEquals(Duration.ofSeconds(10), backoff.getDelay());
        assertEquals(3.0, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT applyDefaults() with valid delay and multiplier should keep assigned values")
    public void applyDefaults_validDelayAndMultiplier_keepsValues() {
        BackoffProperties backoff = new BackoffProperties();
        backoff.setEnabled(true);
        backoff.setDelay(Duration.ofSeconds(20));
        backoff.setMultiplier(5.6);
        BackoffProperties.Defaults defaults = new BackoffProperties.Defaults(Duration.ofSeconds(10), 3.0);

        backoff.applyDefaults(defaults);

        assertTrue(backoff.isEnabled());
        assertEquals(Duration.ofSeconds(20), backoff.getDelay());
        assertEquals(5.6, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT applyDefaults() with multiplier < 1 should assign default multiplier")
    public void applyDefaults_multiplierLessThanOne_usesDefaultMultiplier() {
        BackoffProperties backoff = new BackoffProperties();
        backoff.setEnabled(true);
        backoff.setDelay(Duration.ofSeconds(5));
        backoff.setMultiplier(0.0);
        BackoffProperties.Defaults defaults = new BackoffProperties.Defaults(Duration.ofSeconds(10), 3.0);

        backoff.applyDefaults(defaults);

        assertTrue(backoff.isEnabled());
        assertEquals(Duration.ofSeconds(5), backoff.getDelay());
        assertEquals(3.0, backoff.getMultiplier());
    }

    @Test
    @DisplayName("UT Defaults.ofBackoffProperties() should extract delay and multiplier into Defaults record")
    public void defaults_ofBackoffProperties_createsCorrectRecord() {
        BackoffProperties backoff = new BackoffProperties(true, Duration.ofSeconds(15), 4.5);

        BackoffProperties.Defaults defaults = BackoffProperties.Defaults.ofBackoffProperties(backoff);

        assertEquals(Duration.ofSeconds(15), defaults.delay());
        assertEquals(4.5, defaults.multiplier());
    }
}