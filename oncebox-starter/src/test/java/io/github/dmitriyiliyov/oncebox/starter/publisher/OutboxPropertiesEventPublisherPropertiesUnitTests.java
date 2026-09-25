package io.github.dmitriyiliyov.oncebox.starter.publisher;

import io.github.dmitriyiliyov.oncebox.starter.OutboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.PollingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class OutboxPropertiesEventPublisherPropertiesUnitTests {

    @Test
    @DisplayName("UT applyDefaults() should throw when eventType is null")
    public void applyDefaults_eventTypeNull_shouldThrow() {
        // given
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setEventType(null);
        event.setTopic("topic");
        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        defaults.applyDefaults();

        // when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> event.applyDefaults(defaults));

        // then
        assertEquals("'events' key cannot be null", e.getMessage());
    }

    @Test
    @DisplayName("UT applyDefaults() should throw when eventType is blank")
    public void applyDefaults_eventTypeBlank_shouldThrow() {
        // given
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setEventType("   ");
        event.setTopic("topic");
        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        defaults.applyDefaults();

        // when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> event.applyDefaults(defaults));

        // then
        assertEquals("'events' key cannot be blank", e.getMessage());
    }

    @Test
    @DisplayName("UT applyDefaults() should throw when topic is null")
    public void applyDefaults_topicNull_shouldThrow() {
        // given
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setEventType("event");
        event.setTopic(null);
        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        defaults.applyDefaults();

        // when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> event.applyDefaults(defaults));

        // then
        assertEquals("'topic' cannot be null", e.getMessage());
    }

    @Test
    @DisplayName("UT applyDefaults() should throw when topic is blank")
    public void applyDefaults_topicBlank_shouldThrow() {
        // given
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setEventType("event");
        event.setTopic("   ");
        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        defaults.applyDefaults();

        // when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> event.applyDefaults(defaults));

        // then
        assertEquals("'topic' cannot be blank", e.getMessage());
    }

    @Test
    @DisplayName("UT applyDefaults() should assign default values when fields are null")
    public void applyDefaults_nullFields_shouldUseDefaults() {
        // given
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setEventType("event");
        event.setTopic("topic");
        event.setBatchSize(null);
        event.setPolling(null);
        event.setMaxRetries(null);
        event.setBackoff(null);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        defaults.applyDefaults();

        // when
        event.applyDefaults(defaults);

        // then
        assertEquals(defaults.getBatchSize(), event.getBatchSize());
        assertEquals(defaults.getMaxRetries(), event.getMaxRetries());

        assertEquals(defaults.getPolling().getInitialDelay(), event.getInitialDelay());
        assertEquals(defaults.getPolling().getType(), event.getPolling().getType());

        assertEquals(defaults.getBackoff().getDelay().toMillis(), event.backoffDelay());
        assertEquals(defaults.getBackoff().getMultiplier(), event.backoffMultiplier());
    }

    @Test
    @DisplayName("UT applyDefaults() should override disabled backoff properly")
    public void applyDefaults_disabledBackoff_shouldResetToDisabledDefaults() {
        // given
        OutboxPublisherProperties.BackoffProperties customBackoff = new OutboxPublisherProperties.BackoffProperties();
        customBackoff.setEnabled(false);
        customBackoff.setDelay(Duration.ofHours(10));

        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setEventType("event");
        event.setTopic("topic");
        event.setBackoff(customBackoff);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        defaults.applyDefaults();

        // when
        event.applyDefaults(defaults);

        // then
        assertFalse(event.getBackoff().isEnabled());
        assertEquals(Duration.ZERO, event.getBackoff().getDelay());
    }

    @Test
    @DisplayName("UT backoffMultiplier() should return correct multiplier")
    public void backoffMultiplier_shouldReturnCorrectValue() {
        // given
        OutboxPublisherProperties.BackoffProperties backoff = new OutboxPublisherProperties.BackoffProperties(true, Duration.ofSeconds(5), 7.3);
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setEventType("event");
        event.setTopic("topic");
        event.setBackoff(backoff);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        defaults.applyDefaults();

        // when
        event.applyDefaults(defaults);
        Double multiplier = event.backoffMultiplier();

        // then
        assertEquals(7.3, multiplier);
    }

    @Test
    @DisplayName("UT backoffDelay() should return correct delay in milliseconds")
    public void backoffDelay_shouldReturnCorrectValue() {
        // given
        OutboxPublisherProperties.BackoffProperties backoff = new OutboxPublisherProperties.BackoffProperties(true, Duration.ofSeconds(15), 3.5);
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setEventType("event");
        event.setTopic("topic");
        event.setBackoff(backoff);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        defaults.applyDefaults();

        // when
        event.applyDefaults(defaults);
        long delay = event.backoffDelay();

        // then
        assertEquals(15_000L, delay);
    }

    @Test
    @DisplayName("UT backoffDelay() when delay is below a second should keep it instead of truncating to 0")
    public void backoffDelay_whenDelayIsBelowSecond_shouldKeepIt() {
        // given
        OutboxPublisherProperties.BackoffProperties backoff = new OutboxPublisherProperties.BackoffProperties(true, Duration.ofMillis(500), 2.0);
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setEventType("event");
        event.setTopic("topic");
        event.setBackoff(backoff);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        defaults.applyDefaults();

        // when
        event.applyDefaults(defaults);

        // then
        assertEquals(500L, event.backoffDelay());
    }

    @Test
    @DisplayName("UT applyDefaults() with valid parameters should merge correctly")
    public void applyDefaults_validParameters_shouldKeepValues() {
        // given
        OutboxPublisherProperties.BackoffProperties backoff = new OutboxPublisherProperties.BackoffProperties(true, Duration.ofSeconds(10), 5.3);

        OutboxProperties.PollingProperties polling = new OutboxProperties.PollingProperties();
        polling.setType(PollingType.FIXED);
        polling.setInitialDelay(Duration.ofSeconds(5));
        polling.setFixedDelay(Duration.ofSeconds(10));

        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
        event.setEventType("user-registered");
        event.setTopic("user-topic");
        event.setBatchSize(20);
        event.setPolling(polling);
        event.setMaxRetries(3);
        event.setBackoff(backoff);

        OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
        defaults.applyDefaults();

        // when
        event.applyDefaults(defaults);

        // then
        assertEquals("user-registered", event.getEventType());
        assertEquals("user-topic", event.getTopic());
        assertEquals(20, event.getBatchSize());
        assertEquals(3, event.getMaxRetries());

        assertEquals(PollingType.FIXED, event.getPolling().getType());
        assertEquals(Duration.ofSeconds(5), event.getInitialDelay());
        assertEquals(Duration.ofSeconds(10), event.getFixedDelay());

        assertEquals(backoff, event.getBackoff());
        assertEquals(10_000L, event.backoffDelay());
        assertEquals(5.3, event.backoffMultiplier());
    }

    @Test
    @DisplayName("UT EventProperties.equals() should return true when comparing the same instance")
    public void equals_whenSameInstance_thenTrue() {
        // given
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();

        // then
        assertEquals(event, event);
    }

    @Test
    @DisplayName("UT EventProperties.equals() should return false when comparing with null or different class")
    public void equals_whenNullOrDifferentClass_thenFalse() {
        // given
        OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();

        // then
        assertNotEquals(null, event);
        assertNotEquals("Not an EventProperties object", event);
    }

    @Test
    @DisplayName("UT EventProperties.equals() should return true when objects have identical values")
    public void equals_whenIdenticalValues_thenTrue() {
        // given
        OutboxPublisherProperties.EventProperties event1 = new OutboxPublisherProperties.EventProperties();
        event1.setEventType("USER_CREATED");
        event1.setTopic("user-events");
        event1.setBatchSize(100);

        OutboxPublisherProperties.EventProperties event2 = new OutboxPublisherProperties.EventProperties();
        event2.setEventType("USER_CREATED");
        event2.setTopic("user-events");
        event2.setBatchSize(100);

        // then
        assertEquals(event1, event2);
        assertEquals(event2, event1);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("UT EventProperties.equals() should return false when eventType differs")
    public void equals_whenEventTypeDiffers_thenFalse() {
        // given
        OutboxPublisherProperties.EventProperties event1 = new OutboxPublisherProperties.EventProperties();
        event1.setEventType("USER_CREATED");

        OutboxPublisherProperties.EventProperties event2 = new OutboxPublisherProperties.EventProperties();
        event2.setEventType("USER_DELETED");

        // then
        assertNotEquals(event1, event2);
        assertNotEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("UT EventProperties.equals() should return false when topic differs")
    public void equals_whenTopicDiffers_thenFalse() {
        // given
        OutboxPublisherProperties.EventProperties event1 = new OutboxPublisherProperties.EventProperties();
        event1.setTopic("topic-1");

        OutboxPublisherProperties.EventProperties event2 = new OutboxPublisherProperties.EventProperties();
        event2.setTopic("topic-2");

        // then
        assertNotEquals(event1, event2);
        assertNotEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("UT EventProperties.equals() should return false when batchSize differs")
    public void equals_whenBatchSizeDiffers_thenFalse() {
        // given
        OutboxPublisherProperties.EventProperties event1 = new OutboxPublisherProperties.EventProperties();
        event1.setBatchSize(50);

        OutboxPublisherProperties.EventProperties event2 = new OutboxPublisherProperties.EventProperties();
        event2.setBatchSize(100);

        // then
        assertNotEquals(event1, event2);
        assertNotEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("UT EventProperties.equals() should return false when pollingDefaults differs")
    public void equals_whenPollingDiffers_thenFalse() {
        // given
        OutboxPublisherProperties.EventProperties event1 = new OutboxPublisherProperties.EventProperties();
        OutboxProperties.PollingProperties polling1 = new OutboxProperties.PollingProperties();
        polling1.setInitialDelay(Duration.ofSeconds(10));
        event1.setPolling(polling1);

        OutboxPublisherProperties.EventProperties event2 = new OutboxPublisherProperties.EventProperties();
        OutboxProperties.PollingProperties polling2 = new OutboxProperties.PollingProperties();
        polling2.setInitialDelay(Duration.ofSeconds(20));
        event2.setPolling(polling2);

        // then
        assertNotEquals(event1, event2);
        assertNotEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("UT EventProperties.equals() should return false when maxRetries differs")
    public void equals_whenMaxRetriesDiffers_thenFalse() {
        // given
        OutboxPublisherProperties.EventProperties event1 = new OutboxPublisherProperties.EventProperties();
        event1.setMaxRetries(3);

        OutboxPublisherProperties.EventProperties event2 = new OutboxPublisherProperties.EventProperties();
        event2.setMaxRetries(5);

        // then
        assertNotEquals(event1, event2);
        assertNotEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("UT EventProperties.equals() should return false when backoff differs")
    public void equals_whenBackoffDiffers_thenFalse() {
        // given
        OutboxPublisherProperties.EventProperties event1 = new OutboxPublisherProperties.EventProperties();
        OutboxPublisherProperties.BackoffProperties backoff1 = new OutboxPublisherProperties.BackoffProperties();
        backoff1.setMultiplier(2.0);
        event1.setBackoff(backoff1);

        OutboxPublisherProperties.EventProperties event2 = new OutboxPublisherProperties.EventProperties();
        OutboxPublisherProperties.BackoffProperties backoff2 = new OutboxPublisherProperties.BackoffProperties();
        backoff2.setMultiplier(3.0);
        event2.setBackoff(backoff2);

        // then
        assertNotEquals(event1, event2);
        assertNotEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("UT EventProperties.equals() should safely handle null fields")
    public void equals_whenFieldsAreNull_thenHandleSafely() {
        // given
        OutboxPublisherProperties.EventProperties event1 = new OutboxPublisherProperties.EventProperties();
        OutboxPublisherProperties.EventProperties event2 = new OutboxPublisherProperties.EventProperties();

        event2.setEventType("TEST");

        // then
        assertNotEquals(event1, event2);

        event2.setEventType(null);
        event2.setTopic(null);
        event2.setBatchSize(null);
        event2.setMaxRetries(null);
        event2.setPolling(null);
        event2.setBackoff(null);

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }
}
