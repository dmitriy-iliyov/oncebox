package io.github.dmitriyiliyov.oncebox.starter.outbox;

import io.github.dmitriyiliyov.oncebox.starter.OnceboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.PollingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

public class OutboxPropertiesEventPropertiesUnitTests {

    @Test
    @DisplayName("UT applyDefaults() should throw when eventType is null")
    public void applyDefaults_eventTypeNull_shouldThrow() {
        // given
        OutboxProperties.EventProperties event = new OutboxProperties.EventProperties();
        event.setEventType(null);
        event.setTopic("topic");
        OutboxProperties.EventProperties.Defaults defaults = new OutboxProperties.EventProperties.Defaults();
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
        OutboxProperties.EventProperties event = new OutboxProperties.EventProperties();
        event.setEventType("   ");
        event.setTopic("topic");
        OutboxProperties.EventProperties.Defaults defaults = new OutboxProperties.EventProperties.Defaults();
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
        OutboxProperties.EventProperties event = new OutboxProperties.EventProperties();
        event.setEventType("event");
        event.setTopic(null);
        OutboxProperties.EventProperties.Defaults defaults = new OutboxProperties.EventProperties.Defaults();
        defaults.applyDefaults();

        // when
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> event.applyDefaults(defaults));

        // then
        assertEquals("'topic' cannot be null", e.getMessage());
    }

    @Test
    @DisplayName("UT applyDefaults() when maxRetries is negative should throw IllegalArgumentException")
    void applyDefaults_whenMaxRetriesIsNegative_shouldThrowIllegalArgumentException() {
        // given
        OutboxProperties.EventProperties event = new OutboxProperties.EventProperties();
        event.setEventType("event");
        event.setTopic("topic");
        event.setMaxRetries(-1);
        OutboxProperties.EventProperties.Defaults defaults = new OutboxProperties.EventProperties.Defaults();
        defaults.applyDefaults();

        // when / then
        assertThatThrownBy(() -> event.applyDefaults(defaults))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'max-retries' cannot be negative");
    }

    @Test
    @DisplayName("UT applyDefaults() should throw when topic is blank")
    public void applyDefaults_topicBlank_shouldThrow() {
        // given
        OutboxProperties.EventProperties event = new OutboxProperties.EventProperties();
        event.setEventType("event");
        event.setTopic("   ");
        OutboxProperties.EventProperties.Defaults defaults = new OutboxProperties.EventProperties.Defaults();
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
        OutboxProperties.EventProperties event = new OutboxProperties.EventProperties();
        event.setEventType("event");
        event.setTopic("topic");
        event.setBatchSize(null);
        event.setPolling(null);
        event.setMaxRetries(null);
        event.setBackoff(null);

        OutboxProperties.EventProperties.Defaults defaults = new OutboxProperties.EventProperties.Defaults();
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
        OutboxProperties.BackoffProperties customBackoff = new OutboxProperties.BackoffProperties();
        customBackoff.setEnabled(false);
        customBackoff.setDelay(Duration.ofHours(10));

        OutboxProperties.EventProperties event = new OutboxProperties.EventProperties();
        event.setEventType("event");
        event.setTopic("topic");
        event.setBackoff(customBackoff);

        OutboxProperties.EventProperties.Defaults defaults = new OutboxProperties.EventProperties.Defaults();
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
        OutboxProperties.BackoffProperties backoff = new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(5), 7.3);
        OutboxProperties.EventProperties event = new OutboxProperties.EventProperties();
        event.setEventType("event");
        event.setTopic("topic");
        event.setBackoff(backoff);

        OutboxProperties.EventProperties.Defaults defaults = new OutboxProperties.EventProperties.Defaults();
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
        OutboxProperties.BackoffProperties backoff = new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(15), 3.5);
        OutboxProperties.EventProperties event = new OutboxProperties.EventProperties();
        event.setEventType("event");
        event.setTopic("topic");
        event.setBackoff(backoff);

        OutboxProperties.EventProperties.Defaults defaults = new OutboxProperties.EventProperties.Defaults();
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
        OutboxProperties.BackoffProperties backoff = new OutboxProperties.BackoffProperties(true, Duration.ofMillis(500), 2.0);
        OutboxProperties.EventProperties event = new OutboxProperties.EventProperties();
        event.setEventType("event");
        event.setTopic("topic");
        event.setBackoff(backoff);

        OutboxProperties.EventProperties.Defaults defaults = new OutboxProperties.EventProperties.Defaults();
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
        OutboxProperties.BackoffProperties backoff = new OutboxProperties.BackoffProperties(true, Duration.ofSeconds(10), 5.3);

        OnceboxProperties.PollingProperties polling = new OnceboxProperties.PollingProperties();
        polling.setType(PollingType.FIXED);
        polling.setInitialDelay(Duration.ofSeconds(5));
        polling.setFixedDelay(Duration.ofSeconds(10));

        OutboxProperties.EventProperties event = new OutboxProperties.EventProperties();
        event.setEventType("user-registered");
        event.setTopic("user-topic");
        event.setBatchSize(20);
        event.setPolling(polling);
        event.setMaxRetries(3);
        event.setBackoff(backoff);

        OutboxProperties.EventProperties.Defaults defaults = new OutboxProperties.EventProperties.Defaults();
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
        OutboxProperties.EventProperties event = new OutboxProperties.EventProperties();

        // then
        assertEquals(event, event);
    }

    @Test
    @DisplayName("UT EventProperties.equals() should return false when comparing with null or different class")
    public void equals_whenNullOrDifferentClass_thenFalse() {
        // given
        OutboxProperties.EventProperties event = new OutboxProperties.EventProperties();

        // then
        assertNotEquals(null, event);
        assertNotEquals("Not an EventProperties object", event);
    }

    @Test
    @DisplayName("UT EventProperties.equals() should return true when objects have identical values")
    public void equals_whenIdenticalValues_thenTrue() {
        // given
        OutboxProperties.EventProperties event1 = new OutboxProperties.EventProperties();
        event1.setEventType("USER_CREATED");
        event1.setTopic("user-events");
        event1.setBatchSize(100);

        OutboxProperties.EventProperties event2 = new OutboxProperties.EventProperties();
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
        OutboxProperties.EventProperties event1 = new OutboxProperties.EventProperties();
        event1.setEventType("USER_CREATED");

        OutboxProperties.EventProperties event2 = new OutboxProperties.EventProperties();
        event2.setEventType("USER_DELETED");

        // then
        assertNotEquals(event1, event2);
        assertNotEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("UT EventProperties.equals() should return false when topic differs")
    public void equals_whenTopicDiffers_thenFalse() {
        // given
        OutboxProperties.EventProperties event1 = new OutboxProperties.EventProperties();
        event1.setTopic("topic-1");

        OutboxProperties.EventProperties event2 = new OutboxProperties.EventProperties();
        event2.setTopic("topic-2");

        // then
        assertNotEquals(event1, event2);
        assertNotEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("UT EventProperties.equals() should return false when batchSize differs")
    public void equals_whenBatchSizeDiffers_thenFalse() {
        // given
        OutboxProperties.EventProperties event1 = new OutboxProperties.EventProperties();
        event1.setBatchSize(50);

        OutboxProperties.EventProperties event2 = new OutboxProperties.EventProperties();
        event2.setBatchSize(100);

        // then
        assertNotEquals(event1, event2);
        assertNotEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("UT EventProperties.equals() should return false when pollingDefaults differs")
    public void equals_whenPollingDiffers_thenFalse() {
        // given
        OutboxProperties.EventProperties event1 = new OutboxProperties.EventProperties();
        OnceboxProperties.PollingProperties polling1 = new OnceboxProperties.PollingProperties();
        polling1.setInitialDelay(Duration.ofSeconds(10));
        event1.setPolling(polling1);

        OutboxProperties.EventProperties event2 = new OutboxProperties.EventProperties();
        OnceboxProperties.PollingProperties polling2 = new OnceboxProperties.PollingProperties();
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
        OutboxProperties.EventProperties event1 = new OutboxProperties.EventProperties();
        event1.setMaxRetries(3);

        OutboxProperties.EventProperties event2 = new OutboxProperties.EventProperties();
        event2.setMaxRetries(5);

        // then
        assertNotEquals(event1, event2);
        assertNotEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("UT EventProperties.equals() should return false when backoff differs")
    public void equals_whenBackoffDiffers_thenFalse() {
        // given
        OutboxProperties.EventProperties event1 = new OutboxProperties.EventProperties();
        OutboxProperties.BackoffProperties backoff1 = new OutboxProperties.BackoffProperties();
        backoff1.setMultiplier(2.0);
        event1.setBackoff(backoff1);

        OutboxProperties.EventProperties event2 = new OutboxProperties.EventProperties();
        OutboxProperties.BackoffProperties backoff2 = new OutboxProperties.BackoffProperties();
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
        OutboxProperties.EventProperties event1 = new OutboxProperties.EventProperties();
        OutboxProperties.EventProperties event2 = new OutboxProperties.EventProperties();

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
