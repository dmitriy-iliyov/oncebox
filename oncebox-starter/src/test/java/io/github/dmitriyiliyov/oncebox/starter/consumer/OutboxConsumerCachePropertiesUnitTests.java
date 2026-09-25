package io.github.dmitriyiliyov.oncebox.starter.consumer;

import io.github.dmitriyiliyov.oncebox.starter.consumer.OutboxConsumerProperties.CacheProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OutboxConsumerCachePropertiesUnitTests {
    @Test
    @DisplayName("UT applyDefaults() should set enabled to true when it is null and cacheName is valid")
    void applyDefaults_whenEnabledIsNullAndCacheNameValid_setsEnabledToTrue() {
        CacheProperties properties = new CacheProperties();
        properties.enabled = null;
        properties.cacheName = "myCache";

        properties.applyDefaults();

        assertTrue(properties.enabled);
        assertEquals("myCache", properties.cacheName);
    }

    @Test
    @DisplayName("UT applyDefaults() should keep enabled true when it is true and cacheName is valid")
    void applyDefaults_whenEnabledIsTrueAndCacheNameValid_keepsEnabledTrue() {
        CacheProperties properties = new CacheProperties();
        properties.enabled = true;
        properties.cacheName = "myCache";

        properties.applyDefaults();

        assertTrue(properties.enabled);
        assertEquals("myCache", properties.cacheName);
    }

    @Test
    @DisplayName("UT applyDefaults() should set cacheName to null when enabled is false")
    void applyDefaults_whenEnabledIsFalse_setsCacheNameToNull() {
        CacheProperties properties = new CacheProperties();
        properties.enabled = false;
        properties.cacheName = "shouldBeErased";

        properties.applyDefaults();

        assertFalse(properties.enabled);
        assertNull(properties.cacheName);
    }

    @Test
    @DisplayName("UT applyDefaults() should throw NullPointerException when enabled is null and cacheName is null")
    void applyDefaults_whenEnabledIsNullAndCacheNameIsNull_throwsNPE() {
        CacheProperties properties = new CacheProperties();
        properties.enabled = null;
        properties.cacheName = null;

        NullPointerException exception = assertThrows(NullPointerException.class, properties::applyDefaults);
        assertEquals("'cache-name' cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("UT applyDefaults() should throw IllegalArgumentException when enabled is true and cacheName is blank")
    void applyDefaults_whenEnabledIsTrueAndCacheNameIsBlank_throwsIAE() {
        CacheProperties properties = new CacheProperties();
        properties.enabled = true;

        properties.cacheName = "";
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, properties::applyDefaults);
        assertEquals("'cache-name' cannot be empty or blank", exception1.getMessage());

        properties.cacheName = "   ";
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, properties::applyDefaults);
        assertEquals("'cache-name' cannot be empty or blank", exception2.getMessage());
    }

    @Test
    @DisplayName("UT Getters and Setters should work correctly")
    void gettersAndSetters_workCorrectly() {
        CacheProperties properties = new CacheProperties();

        properties.setEnabled(true);
        assertTrue(properties.isEnabled());

        properties.setCacheName("testCache");
        assertEquals("testCache", properties.getCacheName());
    }

    @Test
    @DisplayName("UT toString() should contain all fields and their values")
    void toString_containsAllFieldsAndValues() {
        CacheProperties properties = new CacheProperties();
        properties.setEnabled(true);
        properties.setCacheName("myCacheName");

        String result = properties.toString();

        assertTrue(result.contains("enabled=true"));
        assertTrue(result.contains("cacheName='myCacheName'"));
    }

    @Test
    @DisplayName("UT toString() should handle null fields gracefully")
    void toString_withNullFields_handlesGracefully() {
        CacheProperties properties = new CacheProperties();

        String result = properties.toString();

        assertTrue(result.contains("enabled=null"));
        assertTrue(result.contains("cacheName='null'"));
    }
}
